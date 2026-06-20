package com.spotzones.presentation.screens.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spotzones.domain.model.Zone
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

/**
 * Interactive OpenStreetMap of all zones.
 *
 * Capabilities:
 * - **My location** — fetches a *fresh* fix in the ViewModel then recenters (the old button read a
 *   stale cached value synchronously and did nothing).
 * - **In-map editing** — tap a zone to select it, drag its pin to move the center, and use the
 *   bottom sheet's sliders to resize the radius / change priority, then Save. No need to open the
 *   full editor for quick geometry tweaks.
 * - **Zoom-to-fit** all zones, dark map tiles in dark theme, and haptic feedback.
 */
@Composable
fun MapScreen(
    onCreateZoneAt: (Double, Double) -> Unit,
    onEditZone: (String) -> Unit,
    viewModel: MapViewModel = hiltViewModel(),
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptics = LocalHapticFeedback.current

    val zones by viewModel.zones.collectAsStateWithLifecycle()
    val myLocation by viewModel.myLocation.collectAsStateWithLifecycle()
    val editing by viewModel.editing.collectAsStateWithLifecycle()
    val locationGranted = remember { viewModel.hasLocationPermission() }

    var mapView by remember { mutableStateOf<MapView?>(null) }
    val myLocationOverlay = remember { mutableStateOf<MyLocationNewOverlay?>(null) }
    val zoneOverlays = remember { mutableListOf<Overlay>() }
    var initialCenterDone by remember { mutableStateOf(false) }

    // Seed the initial camera on the user (or first zone) once available.
    LaunchedEffect(myLocation, zones.size) {
        if (initialCenterDone) return@LaunchedEffect
        val map = mapView ?: return@LaunchedEffect
        val target = myLocation?.let { GeoPoint(it.latitude, it.longitude) }
            ?: zones.firstOrNull()?.let { GeoPoint(it.center.latitude, it.center.longitude) }
        if (target != null) {
            map.controller.setZoom(15.0)
            map.controller.animateTo(target)
            initialCenterDone = true
        }
    }

    // Recenter signals (my-location button) animate the camera to a freshly-resolved fix.
    LaunchedEffect(Unit) {
        viewModel.recenter.collect { coord ->
            mapView?.controller?.animateTo(GeoPoint(coord.latitude, coord.longitude))
            mapView?.controller?.setZoom(16.0)
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
                    isTilesScaledToDpi = true
                    controller.setZoom(11.0)
                    controller.setCenter(GeoPoint(40.0, -73.0))
                    overlays.add(
                        MapEventsOverlay(
                            object : MapEventsReceiver {
                                override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                                    // Tapping empty map dismisses an in-progress edit.
                                    viewModel.cancelEdit()
                                    return false
                                }

                                override fun longPressHelper(p: GeoPoint?): Boolean {
                                    p?.let {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onCreateZoneAt(it.latitude, it.longitude)
                                    }
                                    return true
                                }
                            },
                        ),
                    )
                    if (locationGranted) {
                        myLocationOverlay.value = MyLocationNewOverlay(GpsMyLocationProvider(ctx), this).apply {
                            enableMyLocation()
                            overlays.add(this)
                        }
                    }
                    mapView = this
                    if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) onResume()
                }
            },
            update = { map ->
                // Rebuild only the zone overlays (markers + radius circles); leave events + my-location intact.
                map.overlays.removeAll(zoneOverlays)
                zoneOverlays.clear()
                val draft = editing
                zones.forEach { zone ->
                    val selected = draft?.id == zone.id
                    val center = if (selected) GeoPoint(draft!!.center.latitude, draft.center.longitude)
                    else GeoPoint(zone.center.latitude, zone.center.longitude)
                    val radius = if (selected) draft!!.radiusMeters.toDouble() else zone.radiusMeters
                    val color = Color(if (selected) draft!!.colorHex else zone.colorHex)
                    val circle = buildCircle(center, radius, color, selected)
                    map.overlays.add(circle)
                    zoneOverlays.add(circle)

                    val marker = Marker(map).apply {
                        position = center
                        title = zone.name
                        snippet = zone.playback.playlist?.name ?: "No playlist"
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        // Tint the pin with the zone's colour so the map reads at a glance.
                        androidx.core.content.ContextCompat.getDrawable(map.context, com.spotzones.R.drawable.ic_map_pin)
                            ?.mutate()
                            ?.also { androidx.core.graphics.drawable.DrawableCompat.setTint(it, color.toArgb()) }
                            ?.let { icon = it }
                        isDraggable = selected
                        setOnMarkerClickListener { _, _ ->
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.beginEdit(zone)
                            true
                        }
                        if (selected) {
                            setOnMarkerDragListener(object : Marker.OnMarkerDragListener {
                                override fun onMarkerDragStart(m: Marker) {}
                                override fun onMarkerDrag(m: Marker) {
                                    // Live-redraw the circle as the pin moves.
                                    circle.points = Polygon.pointsAsCircle(m.position, radius)
                                    map.invalidate()
                                }
                                override fun onMarkerDragEnd(m: Marker) {
                                    viewModel.updateDraftCenter(m.position.latitude, m.position.longitude)
                                }
                            })
                        }
                    }
                    map.overlays.add(marker)
                    zoneOverlays.add(marker)
                }
                map.invalidate()
            },
        )

        Text(
            "© OpenStreetMap contributors",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 8.dp, bottom = 8.dp),
        )

        // First-run hint so an empty map invites the core action.
        AnimatedVisibility(
            visible = zones.isEmpty() && editing == null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            ElevatedCard(modifier = Modifier.padding(16.dp), shape = RoundedCornerShape(20.dp)) {
                Text(
                    "Long-press anywhere to drop your first zone",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        }

        // Right-edge controls (hidden while editing to keep focus on the sheet).
        AnimatedVisibility(
            visible = editing == null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopEnd),
        ) {
            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SmallFloatingActionButton(onClick = {
                    // Primary: osmdroid's own location provider (already powering the blue dot).
                    recenterOnMyLocation(mapView, myLocationOverlay.value)
                    // Fallback: fused provider via the ViewModel, which emits a recenter signal.
                    viewModel.recenterOnMe()
                }) {
                    Icon(Icons.Outlined.MyLocation, contentDescription = "My location")
                }
                if (zones.isNotEmpty()) {
                    SmallFloatingActionButton(
                        onClick = { mapView?.let { fitToZones(it, zones) } },
                    ) {
                        Icon(Icons.Outlined.CenterFocusStrong, contentDescription = "Fit all zones")
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = editing == null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomEnd),
        ) {
            ExtendedFloatingActionButton(
                onClick = {
                    val center = mapView?.mapCenter as? GeoPoint
                    if (center != null) onCreateZoneAt(center.latitude, center.longitude)
                },
                text = { Text("New zone here") },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                modifier = Modifier.padding(16.dp),
            )
        }

        // Bottom geometry editor.
        AnimatedVisibility(
            visible = editing != null,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            editing?.let { draft ->
                ZoneEditPanel(
                    draft = draft,
                    onRadius = viewModel::updateDraftRadius,
                    onPriority = { viewModel.updateDraftPriority(it) },
                    onSave = { viewModel.saveDraft() },
                    onCancel = { viewModel.cancelEdit() },
                    onDelete = { viewModel.deleteEditing() },
                    onOpenFull = { onEditZone(draft.id) },
                )
            }
        }
    }
}

@Composable
private fun ZoneEditPanel(
    draft: ZoneDraft,
    onRadius: (Float) -> Unit,
    onPriority: (Int) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onOpenFull: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(14.dp)) {
                    androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) { drawCircle(Color(draft.colorHex)) }
                }
                Spacer(Modifier.size(10.dp))
                Text(draft.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text("Drag the pin to move", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.size(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Radius", style = MaterialTheme.typography.bodyLarge)
                Text("${draft.radiusMeters.toInt()} m", color = MaterialTheme.colorScheme.primary)
            }
            Slider(value = draft.radiusMeters, onValueChange = onRadius, valueRange = 20f..2000f)

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Priority", style = MaterialTheme.typography.bodyLarge)
                Text(draft.priority.toString(), color = MaterialTheme.colorScheme.primary)
            }
            Slider(value = draft.priority.toFloat(), onValueChange = { onPriority(it.toInt()) }, valueRange = 1f..100f)

            Spacer(Modifier.size(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.size(4.dp))
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.weight(1f))
                OutlinedButton(onClick = onOpenFull) {
                    Icon(Icons.Outlined.Edit, contentDescription = null)
                    Spacer(Modifier.size(4.dp))
                    Text("Details")
                }
                TextButton(onClick = onCancel) { Text("Cancel") }
                Button(onClick = onSave) { Text("Save") }
            }
        }
    }
}

private fun buildCircle(center: GeoPoint, radiusMeters: Double, color: Color, selected: Boolean): Polygon =
    Polygon().apply {
        points = Polygon.pointsAsCircle(center, radiusMeters)
        fillPaint.color = color.copy(alpha = if (selected) 0.30f else 0.18f).toArgb()
        outlinePaint.color = color.toArgb()
        outlinePaint.strokeWidth = if (selected) 7f else 4f
    }

/**
 * Centers the map on the device location using osmdroid's [MyLocationNewOverlay]. If a fix already
 * exists we jump straight there; otherwise we enable updates and animate on the first fix (the
 * callback runs off the UI thread, so we post back to the map). This is far more reliable than the
 * fused last-known location, which is frequently null until something requests updates.
 */
private fun recenterOnMyLocation(map: MapView?, overlay: MyLocationNewOverlay?) {
    if (map == null || overlay == null) return
    overlay.enableMyLocation()
    val fix = overlay.myLocation
    if (fix != null) {
        map.controller.animateTo(fix)
        map.controller.setZoom(16.0)
    } else {
        overlay.runOnFirstFix {
            val first = overlay.myLocation ?: return@runOnFirstFix
            map.post {
                map.controller.animateTo(first)
                map.controller.setZoom(16.0)
            }
        }
    }
}

private fun fitToZones(map: MapView, zones: List<Zone>) {
    if (zones.isEmpty()) return
    if (zones.size == 1) {
        val z = zones.first()
        map.controller.animateTo(GeoPoint(z.center.latitude, z.center.longitude))
        map.controller.setZoom(15.0)
        return
    }
    // Expand each center by its radius (~deg) so circles aren't clipped at the edges.
    var north = -90.0; var south = 90.0; var east = -180.0; var west = 180.0
    zones.forEach { z ->
        val latPad = z.radiusMeters / 111_000.0
        val lonPad = z.radiusMeters / (111_000.0 * Math.cos(Math.toRadians(z.center.latitude)).coerceAtLeast(0.01))
        north = maxOf(north, z.center.latitude + latPad)
        south = minOf(south, z.center.latitude - latPad)
        east = maxOf(east, z.center.longitude + lonPad)
        west = minOf(west, z.center.longitude - lonPad)
    }
    map.zoomToBoundingBox(BoundingBox(north, east, south, west), true, 96)
}
