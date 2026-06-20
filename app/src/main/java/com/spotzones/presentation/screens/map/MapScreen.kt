package com.spotzones.presentation.screens.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spotzones.domain.model.Zone
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

/**
 * Interactive map of all zones using OpenStreetMap (free, no API key). Long-press anywhere to
 * drop a new zone; tap a marker to edit. Radius circles are drawn per zone in its own colour.
 */
@Composable
fun MapScreen(
    onCreateZoneAt: (Double, Double) -> Unit,
    onEditZone: (String) -> Unit,
    viewModel: MapViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val zones by viewModel.zones.collectAsStateWithLifecycle()
    val myLocation by viewModel.myLocation.collectAsStateWithLifecycle()
    val locationGranted = remember { viewModel.hasLocationPermission() }

    var mapView by remember { mutableStateOf<MapView?>(null) }
    var initialCenterDone by remember { mutableStateOf(false) }

    LaunchedEffect(myLocation, zones.size) {
        if (initialCenterDone) return@LaunchedEffect
        val map = mapView ?: return@LaunchedEffect
        val target = myLocation?.let { GeoPoint(it.latitude, it.longitude) }
            ?: zones.firstOrNull()?.let { GeoPoint(it.center.latitude, it.center.longitude) }
        if (target != null) {
            map.controller.setZoom(14.0)
            map.controller.setCenter(target)
            initialCenterDone = true
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView?.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView?.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView?.onDetach()
        }
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(11.0)
                    controller.setCenter(GeoPoint(40.0, -73.0))
                    overlays.add(
                        MapEventsOverlay(
                            object : MapEventsReceiver {
                                override fun singleTapConfirmedHelper(p: GeoPoint?) = false

                                override fun longPressHelper(p: GeoPoint?): Boolean {
                                    p?.let { onCreateZoneAt(it.latitude, it.longitude) }
                                    return true
                                }
                            },
                        ),
                    )
                    mapView = this
                    if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                        onResume()
                    }
                }
            },
            update = { map ->
                map.overlays.removeAll { it !is MapEventsOverlay }
                if (locationGranted) {
                    MyLocationNewOverlay(GpsMyLocationProvider(context), map).apply {
                        enableMyLocation()
                        map.overlays.add(this)
                    }
                }
                zones.forEach { zone -> addZoneOverlays(map, zone, onEditZone) }
                map.invalidate()
            },
        )

        Text(
            "© OpenStreetMap contributors",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 8.dp, bottom = 8.dp),
        )

        SmallFloatingActionButton(
            onClick = {
                viewModel.refreshLocation()
                myLocation?.let {
                    mapView?.controller?.setCenter(GeoPoint(it.latitude, it.longitude))
                    mapView?.controller?.setZoom(15.0)
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
        ) {
            Icon(Icons.Outlined.MyLocation, contentDescription = "My location")
        }

        ExtendedFloatingActionButton(
            onClick = {
                val center = mapView?.mapCenter as? GeoPoint
                if (center != null) {
                    onCreateZoneAt(center.latitude, center.longitude)
                }
            },
            text = { Text("New zone here") },
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        )
    }
}

private fun addZoneOverlays(map: MapView, zone: Zone, onEditZone: (String) -> Unit) {
    val center = GeoPoint(zone.center.latitude, zone.center.longitude)
    val color = Color(zone.colorHex)

    Polygon().apply {
        points = Polygon.pointsAsCircle(center, zone.radiusMeters.toDouble())
        fillPaint.color = color.copy(alpha = 0.18f).toArgb()
        outlinePaint.color = color.toArgb()
        outlinePaint.strokeWidth = 4f
        map.overlays.add(this)
    }

    Marker(map).apply {
        position = center
        title = zone.name
        snippet = zone.playback.playlist?.name ?: "No playlist"
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        setOnMarkerClickListener { _, _ ->
            onEditZone(zone.id)
            true
        }
        map.overlays.add(this)
    }
}
