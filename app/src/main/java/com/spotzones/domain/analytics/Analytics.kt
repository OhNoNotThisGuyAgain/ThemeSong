package com.spotzones.domain.analytics

/**
 * Anonymous, opt-in product analytics.
 *
 * Design constraints (privacy-first):
 * - Disabled by default; only emits once the user turns on "Anonymous analytics" in Settings.
 * - Never includes PII: no account ids, no coordinates, no playlist names — only the event name and
 *   a small, explicitly-listed set of non-identifying properties, plus a random install id that is
 *   not tied to the user's identity and is cleared on logout/opt-out.
 *
 * Implementations decide *where* events go (HTTP collector, logcat, no-op); callers depend only on
 * this port, so swapping the backend never touches feature code.
 */
interface Analytics {
    /** Records an event. Cheap and non-blocking; implementations buffer and send off the main thread. */
    fun track(event: AnalyticsEvent)

    /** Reflects the user's opt-in choice. When turned off, queued events are dropped. */
    fun setEnabled(enabled: Boolean)
}

/**
 * Closed set of product events. Keeping these typed (rather than free-form strings at call sites)
 * means every analytic is auditable in one place — you can read this file and know exactly what the
 * app can ever report.
 */
sealed class AnalyticsEvent(val name: String, val properties: Map<String, String> = emptyMap()) {
    data object AppOpened : AnalyticsEvent("app_opened")
    data object OnboardingCompleted : AnalyticsEvent("onboarding_completed")
    data object SpotifyConnected : AnalyticsEvent("spotify_connected")

    data class ZoneCreated(val triggerCount: Int, val hasSchedule: Boolean) :
        AnalyticsEvent("zone_created", mapOf("triggers" to triggerCount.toString(), "scheduled" to hasSchedule.toString()))

    data class ZoneEdited(val viaMap: Boolean) :
        AnalyticsEvent("zone_edited", mapOf("via_map" to viaMap.toString()))

    data object ZoneDeleted : AnalyticsEvent("zone_deleted")

    data class ZoneTransition(val trigger: String, val succeeded: Boolean) :
        AnalyticsEvent("zone_transition", mapOf("trigger" to trigger, "ok" to succeeded.toString()))

    data class AutomationToggled(val enabled: Boolean) :
        AnalyticsEvent("automation_toggled", mapOf("enabled" to enabled.toString()))

    data class BackupExported(val encrypted: Boolean) :
        AnalyticsEvent("backup_exported", mapOf("encrypted" to encrypted.toString()))
}
