package com.spotzones.automation

import android.content.Context
import com.spotzones.automation.service.AutomationService
import com.spotzones.automation.work.GeofenceSyncWorker
import com.spotzones.core.permissions.PermissionChecker
import com.spotzones.domain.model.ManualOverride
import com.spotzones.domain.model.OverrideMode
import com.spotzones.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single entry point the UI uses to control automation, hiding the service/worker/override details.
 */
@Singleton
class AutomationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val permissions: PermissionChecker,
) {
    /** Starts the foreground monitoring service (if permitted) and re-syncs geofences. */
    fun startMonitoring() {
        if (!permissions.hasAnyLocation()) return
        GeofenceSyncWorker.enqueue(context)
        AutomationService.start(context)
    }

    fun stopMonitoring() = AutomationService.stop(context)

    /** Applies a manual override that suspends automation for the chosen duration/condition. */
    suspend fun pauseAutomation(mode: OverrideMode, currentZoneId: String? = null) {
        val now = System.currentTimeMillis()
        val expiresAt = when (mode) {
            OverrideMode.FIFTEEN_MIN -> now + 15 * 60_000L
            OverrideMode.THIRTY_MIN -> now + 30 * 60_000L
            OverrideMode.ONE_HOUR -> now + 60 * 60_000L
            OverrideMode.UNTIL_TOMORROW -> tomorrowMidnight()
            OverrideMode.UNTIL_LEAVE_ZONE, OverrideMode.PERMANENT -> null
        }
        settingsRepository.setOverride(ManualOverride(mode, expiresAt, boundZoneId = currentZoneId.takeIf { mode == OverrideMode.UNTIL_LEAVE_ZONE }))
    }

    suspend fun resumeAutomation() = settingsRepository.setOverride(null)

    private fun tomorrowMidnight(): Long =
        LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}
