package com.spotzones.domain.analytics

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Locks the analytics contract: event names and properties are what a collector will receive.
 * If someone changes a name or a property key, this test makes the (intentional) change explicit.
 */
class AnalyticsEventTest {

    @Test fun `event names are stable`() {
        assertThat(AnalyticsEvent.AppOpened.name).isEqualTo("app_opened")
        assertThat(AnalyticsEvent.SpotifyConnected.name).isEqualTo("spotify_connected")
        assertThat(AnalyticsEvent.OnboardingCompleted.name).isEqualTo("onboarding_completed")
        assertThat(AnalyticsEvent.ZoneDeleted.name).isEqualTo("zone_deleted")
    }

    @Test fun `zone created carries non-identifying properties only`() {
        val event = AnalyticsEvent.ZoneCreated(triggerCount = 2, hasSchedule = true)
        assertThat(event.name).isEqualTo("zone_created")
        assertThat(event.properties).containsExactly("triggers", "2", "scheduled", "true")
    }

    @Test fun `zone transition reports trigger and outcome`() {
        val event = AnalyticsEvent.ZoneTransition(trigger = "ZONE_ENTER", succeeded = false)
        assertThat(event.properties["trigger"]).isEqualTo("ZONE_ENTER")
        assertThat(event.properties["ok"]).isEqualTo("false")
    }

    @Test fun `automation toggle records the new state`() {
        assertThat(AnalyticsEvent.AutomationToggled(true).properties["enabled"]).isEqualTo("true")
    }
}
