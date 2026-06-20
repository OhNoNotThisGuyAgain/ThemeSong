package com.spotzones.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GeoCoordinateTest {

    @Test fun `distance between same point is zero`() {
        val p = GeoCoordinate(40.0, -73.0)
        assertThat(p.distanceTo(p)).isWithin(0.001).of(0.0)
    }

    @Test fun `one degree of latitude is approximately 111 km`() {
        val a = GeoCoordinate(40.0, -73.0)
        val b = GeoCoordinate(41.0, -73.0)
        // Haversine: ~111.2 km per degree of latitude.
        assertThat(a.distanceTo(b)).isWithin(500.0).of(111_200.0)
    }

    @Test fun `rejects out of range coordinates`() {
        try {
            GeoCoordinate(91.0, 0.0)
            assertThat(false).isTrue() // should not reach
        } catch (e: IllegalArgumentException) {
            assertThat(e).hasMessageThat().contains("latitude")
        }
    }

    @Test fun `zone contains nearby point`() {
        val zone = GeoCoordinate(40.0, -73.0)
        val near = GeoCoordinate(40.001, -73.0) // ~111 m north
        assertThat(zone.distanceTo(near)).isLessThan(200.0)
    }
}
