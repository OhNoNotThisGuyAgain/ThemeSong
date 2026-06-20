package com.spotzones.data.analytics

import android.content.Context
import androidx.core.content.edit
import com.spotzones.BuildConfig
import com.spotzones.core.coroutines.ApplicationScope
import com.spotzones.core.coroutines.IoDispatcher
import com.spotzones.di.AuthClient
import com.spotzones.domain.analytics.Analytics
import com.spotzones.domain.analytics.AnalyticsEvent
import com.spotzones.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber

/**
 * Sends anonymous, opt-in analytics to a configurable HTTPS collector.
 *
 * Where events go: `BuildConfig.ANALYTICS_ENDPOINT`. When that is blank (the default, no collector
 * configured), events are logged to logcat instead of leaving the device, so the pipeline is fully
 * functional and observable out of the box without committing anyone's server URL. A maintainer
 * enables real delivery by setting `ANALYTICS_ENDPOINT` (+ optional `ANALYTICS_API_KEY`).
 *
 * Delivery is fire-and-forget through a bounded channel consumed by a single coroutine, so call
 * sites never block and a flaky network can never stall the app. Sending is gated on the user's
 * opt-in; turning it off drops the queue and rotates the anonymous install id.
 */
@Singleton
class HttpAnalytics @Inject constructor(
    @ApplicationContext private val context: Context,
    @AuthClient private val client: OkHttpClient,
    private val settingsRepository: SettingsRepository,
    @ApplicationScope private val scope: CoroutineScope,
    @IoDispatcher private val io: CoroutineDispatcher,
) : Analytics {

    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }
    private val prefs = context.getSharedPreferences("analytics", Context.MODE_PRIVATE)

    @Volatile private var enabled = false

    private val queue = Channel<AnalyticsEvent>(capacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    init {
        // Mirror the user's opt-in choice without each call site having to know about it.
        scope.launch {
            settingsRepository.settings
                .map { it.analyticsEnabled }
                .distinctUntilChanged()
                .collect { setEnabled(it) }
        }
        // Single consumer serialises network sends.
        scope.launch(io) {
            for (event in queue) {
                if (enabled) runCatching { send(event) }.onFailure { Timber.d("analytics send failed: %s", it.message) }
            }
        }
    }

    override fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        if (!enabled) {
            // Drop anything queued and rotate the anonymous id so a future opt-in can't be correlated.
            while (queue.tryReceive().isSuccess) { /* drain */ }
            prefs.edit { remove(KEY_INSTALL_ID) }
        }
    }

    override fun track(event: AnalyticsEvent) {
        if (!enabled) return
        queue.trySend(event)
    }

    private suspend fun send(event: AnalyticsEvent) = withContext(io) {
        val payload = buildPayload(event)
        val endpoint = BuildConfig.ANALYTICS_ENDPOINT
        if (endpoint.isBlank()) {
            // No collector configured: keep it on-device but observable.
            Timber.i("analytics(local): %s", payload)
            return@withContext
        }
        val request = Request.Builder()
            .url(endpoint)
            .post(payload.toRequestBody(JSON_MEDIA))
            .apply { if (BuildConfig.ANALYTICS_API_KEY.isNotBlank()) header("Authorization", "Bearer ${BuildConfig.ANALYTICS_API_KEY}") }
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) Timber.d("analytics http %d", response.code)
        }
    }

    private fun buildPayload(event: AnalyticsEvent): String {
        val props = buildMap<String, JsonElement> {
            put("event", JsonPrimitive(event.name))
            put("install_id", JsonPrimitive(installId()))
            put("app_version", JsonPrimitive(BuildConfig.VERSION_NAME))
            put("platform", JsonPrimitive("android"))
            put("ts", JsonPrimitive(System.currentTimeMillis()))
            event.properties.forEach { (k, v) -> put(k, JsonPrimitive(v)) }
        }
        return json.encodeToString(JsonObject(props))
    }

    /** Random, rotating, non-identifying id so events from one install can be loosely grouped. */
    private fun installId(): String {
        prefs.getString(KEY_INSTALL_ID, null)?.let { return it }
        val id = UUID.randomUUID().toString()
        prefs.edit { putString(KEY_INSTALL_ID, id) }
        return id
    }

    private companion object {
        const val KEY_INSTALL_ID = "install_id"
        val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    }
}
