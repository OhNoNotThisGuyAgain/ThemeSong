package com.spotzones.automation.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.spotzones.automation.AutomationCoordinator
import com.spotzones.domain.model.TransitionTrigger
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

/**
 * Runs a single automation pass off the main thread. Triggered by geofence transitions and a
 * periodic safety re-check. Using WorkManager (rather than always-on polling) is central to the
 * battery budget: the app does work only when the OS hands it a transition or on a sparse schedule.
 */
@HiltWorker
class AutomationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val coordinator: AutomationCoordinator,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = try {
        val triggerName = inputData.getString(KEY_TRIGGER)
        val trigger = triggerName?.let { runCatching { TransitionTrigger.valueOf(it) }.getOrNull() }
            ?: TransitionTrigger.RULE_MATCH
        coordinator.evaluate(trigger)
        Result.success()
    } catch (e: Exception) {
        Timber.e(e, "Automation pass failed")
        // Transient failures (no network, Spotify warming up) are worth one retry.
        if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.success()
    }

    companion object {
        const val UNIQUE_NAME = "automation_pass"
        private const val KEY_TRIGGER = "trigger"
        private const val MAX_ATTEMPTS = 3

        fun enqueue(context: Context, trigger: TransitionTrigger) {
            val request = OneTimeWorkRequestBuilder<AutomationWorker>()
                .setInputData(workDataOf(KEY_TRIGGER to trigger.name))
                .build()
            // REPLACE so a fresh transition supersedes a queued, now-stale evaluation.
            WorkManager.getInstance(context)
                .enqueueUniqueWork(UNIQUE_NAME, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
