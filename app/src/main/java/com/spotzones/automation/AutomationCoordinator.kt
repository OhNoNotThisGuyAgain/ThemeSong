package com.spotzones.automation

import com.spotzones.domain.engine.AutomationDecision
import com.spotzones.domain.engine.RuleEngine
import com.spotzones.domain.location.ContextProvider
import com.spotzones.domain.model.Action
import com.spotzones.domain.model.TransitionHistory
import com.spotzones.domain.model.TransitionTrigger
import com.spotzones.domain.repository.HistoryRepository
import com.spotzones.domain.repository.RuleRepository
import com.spotzones.domain.repository.SettingsRepository
import com.spotzones.domain.repository.ZoneRepository
import com.spotzones.domain.spotify.SpotifyController
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

/** Outcome of one automation pass, surfaced to the service for its notification + to tests. */
data class AutomationResult(
    val decision: AutomationDecision,
    val changed: Boolean,
    val activeZoneName: String?,
    val suppressedByOverride: Boolean,
)

/**
 * The automation brain. Pulls together context, persisted zones/rules and the pure [RuleEngine],
 * then reconciles the winning candidate against what is currently active and drives Spotify.
 *
 * Key correctness properties:
 * - **Idempotent**: re-running with an unchanged winner performs no Spotify calls and writes no
 *   history, so geofence jitter and periodic re-checks are cheap and side-effect-free.
 * - **Serialized**: a mutex prevents overlapping passes (a geofence event during a periodic tick)
 *   from racing on Spotify or the state store.
 * - **Respectful of intent**: manual overrides and the global automation toggle short-circuit the
 *   pass before any playback change.
 */
@Singleton
class AutomationCoordinator @Inject constructor(
    private val zoneRepository: ZoneRepository,
    private val ruleRepository: RuleRepository,
    private val settingsRepository: SettingsRepository,
    private val historyRepository: HistoryRepository,
    private val contextProvider: ContextProvider,
    private val spotifyController: SpotifyController,
    private val stateStore: AutomationStateStore,
    private val ruleEngine: RuleEngine,
) {
    private val mutex = Mutex()

    suspend fun evaluate(trigger: TransitionTrigger = TransitionTrigger.RULE_MATCH): AutomationResult =
        mutex.withLock { runEvaluation(trigger) }

    private suspend fun runEvaluation(trigger: TransitionTrigger): AutomationResult {
        val settings = settingsRepository.current()
        val override = settingsRepository.currentOverride()
        val previousId = stateStore.activeCandidateId.value

        // Global off-switch and manual overrides win before anything else.
        if (!settings.automationEnabled) {
            return AutomationResult(AutomationDecision(null, emptyList()), changed = false, null, suppressedByOverride = false)
        }
        if (override != null && override.isActive(System.currentTimeMillis(), stateStore.lastActiveZoneId)) {
            return AutomationResult(AutomationDecision(null, emptyList()), changed = false, null, suppressedByOverride = true)
        }

        val zones = zoneRepository.getEnabledZones()
        val rules = ruleRepository.getEnabledRules()
        val context = contextProvider.currentContext()

        // Optional global gate: only automate while headphones/audio device is connected.
        if (settings.requireHeadphonesForAuto && !context.headphonesConnected) {
            return AutomationResult(AutomationDecision(null, emptyList()), changed = false, null, suppressedByOverride = false)
        }

        val decision = ruleEngine.evaluate(zones, rules, context)
        val winner = decision.winner
        val changed = winner?.id != previousId

        if (!changed) {
            return AutomationResult(decision, changed = false, winner?.displayName, suppressedByOverride = false)
        }

        // Close out the previous zone in history (records exit time).
        if (previousId != null) {
            historyRepository.closeOpenEntry(stateStore.lastActiveZoneId, System.currentTimeMillis(), skippedSongs = 0)
        }

        applyAction(winner?.action ?: Action.None, decision, trigger)
        stateStore.update(winner?.id, winner?.sourceZone?.id)

        return AutomationResult(decision, changed = true, winner?.displayName, suppressedByOverride = false)
    }

    private suspend fun applyAction(action: Action, decision: AutomationDecision, trigger: TransitionTrigger) {
        when (action) {
            is Action.Play -> {
                spotifyController.connect()
                val result = spotifyController.apply(action.config)
                recordTransition(decision, trigger, action.config.playlist?.name, action.config.playlist?.uri, result.isSuccess, result)
            }
            Action.Pause -> spotifyController.pause()
            is Action.SetVolume -> Unit // handled within Play; standalone volume rules are rare
            Action.None -> {
                // Left a zone with nothing else matching: stop the music to avoid surprise playback.
                spotifyController.pause()
            }
        }
    }

    private suspend fun recordTransition(
        decision: AutomationDecision,
        trigger: TransitionTrigger,
        playlistName: String?,
        playlistUri: String?,
        succeeded: Boolean,
        result: com.spotzones.domain.util.Outcome<Unit>,
    ) {
        val zone = decision.activeZone
        historyRepository.record(
            TransitionHistory(
                zoneId = zone?.id,
                zoneName = decision.winner?.displayName ?: "Automation",
                playlistName = playlistName,
                playlistUri = playlistUri,
                trigger = trigger,
                enteredAt = System.currentTimeMillis(),
                exitedAt = null,
                succeeded = succeeded,
                failureReason = (result as? com.spotzones.domain.util.Outcome.Failure)?.error?.message,
            ),
        )
        if (!succeeded) Timber.w("Transition applied with failure: %s", result)
    }
}
