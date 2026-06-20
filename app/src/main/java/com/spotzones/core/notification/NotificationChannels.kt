package com.spotzones.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.content.getSystemService
import com.spotzones.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Centralised notification channel ids and their (idempotent) registration. */
@Singleton
class NotificationChannels @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun ensureCreated() {
        val manager = context.getSystemService<NotificationManager>() ?: return

        val automation = NotificationChannel(
            AUTOMATION,
            context.getString(R.string.channel_automation_name),
            NotificationManager.IMPORTANCE_LOW, // ongoing/quiet; it's a status, not an alert
        ).apply {
            description = context.getString(R.string.channel_automation_desc)
            setShowBadge(false)
        }

        val transitions = NotificationChannel(
            TRANSITIONS,
            context.getString(R.string.channel_transitions_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.channel_transitions_desc)
        }

        manager.createNotificationChannels(listOf(automation, transitions))
    }

    companion object {
        const val AUTOMATION = "automation"
        const val TRANSITIONS = "transitions"
    }
}
