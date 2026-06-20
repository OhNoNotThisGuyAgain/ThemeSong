package com.spotzones.automation.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.spotzones.automation.AutomationCoordinator
import com.spotzones.automation.AutomationStateStore
import com.spotzones.core.coroutines.IoDispatcher
import com.spotzones.domain.location.LocationProvider
import com.spotzones.domain.model.ManualOverride
import com.spotzones.domain.model.OverrideMode
import com.spotzones.domain.model.TransitionTrigger
import com.spotzones.domain.repository.SettingsRepository
import com.spotzones.domain.spotify.SpotifyController
import com.spotzones.location.MovementTracker
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Foreground service that owns continuous monitoring while automation is active.
 *
 * Responsibilities:
 * - Hold a low-power location stream and feed [MovementTracker] (for speed/movement triggers).
 * - Run a sparse periodic re-evaluation as a safety net for non-geofence conditions (time windows,
 *   battery, Bluetooth) that the OS won't wake us for.
 * - Keep the Spotify connection warm and surface the persistent notification with quick controls.
 *
 * The service is only running when monitoring is genuinely needed; geofence transitions still work
 * without it via the broadcast receiver, so battery cost stays proportional to user intent.
 */
@AndroidEntryPoint
class AutomationService : Service() {

    @Inject lateinit var coordinator: AutomationCoordinator
    @Inject lateinit var notifications: AutomationNotifications
    @Inject lateinit var locationProvider: LocationProvider
    @Inject lateinit var movementTracker: MovementTracker
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var spotifyController: SpotifyController
    @Inject lateinit var zoneRepository: com.spotzones.domain.repository.ZoneRepository
    @Inject lateinit var stateStore: AutomationStateStore
    @Inject @IoDispatcher lateinit var io: CoroutineDispatcher

    private val scope = CoroutineScope(SupervisorJob())
    private var periodicJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        startInForeground(activeZoneName = null, paused = false)
        observeState()
        collectLocation()
        startPeriodicEvaluation()
        scope.launch { spotifyController.connect() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE -> pauseAutomation()
            ACTION_RESUME -> resumeAutomation()
            ACTION_SKIP -> scope.launch { spotifyController.skipNext() }
            ACTION_STOP -> stopSelf()
        }
        return START_STICKY
    }

    private fun observeState() {
        // Refresh the notification whenever the active zone changes.
        stateStore.activeCandidateId
            .onEach { refreshNotification() }
            .launchIn(scope)
    }

    private fun collectLocation() {
        locationProvider.locationUpdates()
            .onEach { coordinate ->
                movementTracker.onLocation(coordinate, System.currentTimeMillis(), speedMetersPerSecond = null)
            }
            .launchIn(scope)
    }

    private fun startPeriodicEvaluation() {
        periodicJob?.cancel()
        periodicJob = scope.launch(io) {
            while (isActive) {
                delay(PERIODIC_INTERVAL_MS)
                runCatching { coordinator.evaluate(TransitionTrigger.SCHEDULE) }
                    .onFailure { Timber.w(it, "Periodic evaluation failed") }
            }
        }
    }

    private fun pauseAutomation() = scope.launch {
        settingsRepository.setOverride(ManualOverride(OverrideMode.PERMANENT, expiresAt = null))
        spotifyController.pause()
        refreshNotification()
    }

    private fun resumeAutomation() = scope.launch {
        settingsRepository.setOverride(null)
        coordinator.evaluate(TransitionTrigger.MANUAL)
        refreshNotification()
    }

    private fun refreshNotification() = scope.launch {
        val paused = settingsRepository.currentOverride() != null
        val zoneName = stateStore.lastActiveZoneId?.let { zoneRepository.getZone(it)?.name }
        startInForeground(zoneName, paused)
    }

    private fun startInForeground(activeZoneName: String?, paused: Boolean) {
        val notification = notifications.foreground(activeZoneName, paused)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                AutomationNotifications.FOREGROUND_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
            )
        } else {
            startForeground(AutomationNotifications.FOREGROUND_ID, notification)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_PAUSE = "com.spotzones.action.PAUSE"
        const val ACTION_RESUME = "com.spotzones.action.RESUME"
        const val ACTION_SKIP = "com.spotzones.action.SKIP"
        const val ACTION_STOP = "com.spotzones.action.STOP"
        private const val PERIODIC_INTERVAL_MS = 5 * 60_000L

        fun start(context: Context) {
            val intent = Intent(context, AutomationService::class.java)
            androidx.core.content.ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AutomationService::class.java))
        }
    }
}
