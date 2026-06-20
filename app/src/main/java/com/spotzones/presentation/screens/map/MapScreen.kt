package com.spotzones.presentation.screens.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

/**
 * Interactive map of all zones. Long-press anywhere to drop a new zone at that point; tap a marker
 * to edit. Radius circles are drawn per zone in its own colour, so overlaps/priorities are visible
 * at a glance.
 */
@Composable
fun MapScreen(
    onCreateZoneAt: (Double, Double) -> Unit,
    onEditZone: (String) -> Unit,
    viewModel: MapViewModel = hiltViewModel(),
) {
    val zones by viewModel.zones.collectAsStateWithLifecycle()
    val myLocation by viewModel.myLocation.collectAsStateWithLifecycle()
    // Permission state is read once per composition; enabling the my-location layer without it throws.
    val locationGranted = androidx.compose.runtime.remember { viewModel.hasLocationPermission() }
    val mapsKeyMissing = com.spotzones.BuildConfig.MAPS_API_KEY.isBlank()

    val cameraPositionState = rememberCameraPositionState {
        // Default to a sensible city-level view so a fresh install isn't staring at zoom-0 ocean.
        position = CameraPosition.fromLatLngZoom(LatLng(40.0, -73.0), 11f)
    }

    // Center on the user (or first zone) once a location is available.
    LaunchedEffect(myLocation, zones.size) {
        val target = myLocation?.let { LatLng(it.latitude, it.longitude) }
            ?: zones.firstOrNull()?.let { LatLng(it.center.latitude, it.center.longitude) }
        if (target != null) {
            cameraPositionState.position = CameraPosition.fromLatLngZoom(target, 14f)
        }
    }

    Box(Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = locationGranted, mapType = MapType.NORMAL),
            uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false),
            onMapLongClick = { latLng -> onCreateZoneAt(latLng.latitude, latLng.longitude) },
        ) {
            zones.forEach { zone ->
                val center = LatLng(zone.center.latitude, zone.center.longitude)
                val color = Color(zone.colorHex)
                Circle(
                    center = center,
                    radius = zone.radiusMeters,
                    strokeColor = color,
                    strokeWidth = 4f,
                    fillColor = color.copy(alpha = 0.18f),
                )
                Marker(
                    state = MarkerState(position = center),
                    title = zone.name,
                    snippet = zone.playback.playlist?.name ?: "No playlist",
                    onInfoWindowClick = { onEditZone(zone.id) },
                    onClick = { false },
                )
            }
        }

        SmallFloatingActionButton(
            onClick = {
                viewModel.refreshLocation()
                myLocation?.let {
                    cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 15f))
                }
            },
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
        ) {
            Icon(Icons.Outlined.MyLocation, contentDescription = "My location")
        }

        ExtendedFloatingActionButton(
            onClick = {
                val c = cameraPositionState.position.target
                onCreateZoneAt(c.latitude, c.longitude)
            },
            text = { Text("New zone here") },
            icon = { Icon(Icons.Outlined.MyLocation, contentDescription = null) },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )

        // Without a Maps API key the tiles render blank; explain it instead of a mysterious grey screen.
        if (mapsKeyMissing) {
            androidx.compose.material3.Card(
                modifier = Modifier.align(Alignment.TopCenter).padding(16.dp),
                colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            ) {
                androidx.compose.foundation.layout.Column(Modifier.padding(16.dp)) {
                    Text(
                        "Map needs a Google Maps key",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    Text(
                        "Add MAPS_API_KEY to secrets.properties and rebuild (see README → Google Maps setup).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }
    }
}
