package com.spotzones.data.settings

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Last map camera position, so reopening the Map returns you to where you were. */
data class SavedCamera(val latitude: Double, val longitude: Double, val zoom: Double)

/**
 * Lightweight SharedPreferences store for the map camera. Kept out of DataStore because it's written
 * frequently (on every pan/zoom settle) and read once on open — a synchronous prefs file is the
 * right tool and avoids churning the settings DataStore.
 */
@Singleton
class MapCameraStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("map_camera", Context.MODE_PRIVATE)

    fun save(latitude: Double, longitude: Double, zoom: Double) {
        prefs.edit {
            putString(KEY_LAT, latitude.toString())
            putString(KEY_LNG, longitude.toString())
            putString(KEY_ZOOM, zoom.toString())
        }
    }

    fun last(): SavedCamera? {
        val lat = prefs.getString(KEY_LAT, null)?.toDoubleOrNull() ?: return null
        val lng = prefs.getString(KEY_LNG, null)?.toDoubleOrNull() ?: return null
        val zoom = prefs.getString(KEY_ZOOM, null)?.toDoubleOrNull() ?: return null
        return SavedCamera(lat, lng, zoom)
    }

    private companion object {
        const val KEY_LAT = "lat"
        const val KEY_LNG = "lng"
        const val KEY_ZOOM = "zoom"
    }
}
