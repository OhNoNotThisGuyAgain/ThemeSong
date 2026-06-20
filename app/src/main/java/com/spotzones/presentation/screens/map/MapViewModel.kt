package com.spotzones.presentation.screens.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spotzones.core.permissions.PermissionChecker
import com.spotzones.domain.analytics.Analytics
import com.spotzones.domain.analytics.AnalyticsEvent
import com.spotzones.domain.location.LocationProvider
import com.spotzones.domain.model.GeoCoordinate
import com.spotzones.domain.model.Zone
import com.spotzones.domain.repository.ZoneRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Live, in-map edit buffer for a zone's geometry. Kept separate from the persisted [Zone] so a
 *  drag/resize can be previewed and cancelled without touching the database. */
data class ZoneDraft(
    val id: String,
    val name: String,
    val center: GeoCoordinate,
    val radiusMeters: Float,
    val priority: Int,
    val colorHex: Long,
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val zoneRepository: ZoneRepository,
    private val locationProvider: LocationProvider,
    private val permissions: PermissionChecker,
    private val analytics: Analytics,
) : ViewModel() {

    val zones: StateFlow<List<Zone>> =
        zoneRepository.observeZones().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _myLocation = MutableStateFlow<GeoCoordinate?>(null)
    val myLocation: StateFlow<GeoCoordinate?> = _myLocation.asStateFlow()

    /** One-shot "animate the camera here" signals (my-location button, zoom-to-fit). */
    private val _recenter = MutableSharedFlow<GeoCoordinate>(extraBufferCapacity = 1)
    val recenter: SharedFlow<GeoCoordinate> = _recenter.asSharedFlow()

    private val _editing = MutableStateFlow<ZoneDraft?>(null)
    val editing: StateFlow<ZoneDraft?> = _editing.asStateFlow()

    fun hasLocationPermission(): Boolean = permissions.hasAnyLocation()

    init {
        refreshLocation()
    }

    /** Refreshes the cached fix without moving the camera (used to seed "new zone here"). */
    fun refreshLocation() = viewModelScope.launch {
        if (permissions.hasAnyLocation()) {
            _myLocation.value = locationProvider.lastKnownLocation().getOrNull()
        }
    }

    /**
     * Fetches a *fresh* fix and only then emits a recenter signal. This fixes the original bug where
     * the button read the previous (often null) cached location synchronously and did nothing.
     */
    fun recenterOnMe() = viewModelScope.launch {
        if (!permissions.hasAnyLocation()) return@launch
        val fix = locationProvider.lastKnownLocation().getOrNull() ?: return@launch
        _myLocation.value = fix
        _recenter.emit(fix)
    }

    // --- In-map editing ---

    fun beginEdit(zone: Zone) {
        _editing.value = ZoneDraft(
            id = zone.id,
            name = zone.name,
            center = zone.center,
            radiusMeters = zone.radiusMeters.toFloat(),
            priority = zone.priority,
            colorHex = zone.colorHex,
        )
    }

    fun cancelEdit() { _editing.value = null }

    fun updateDraftCenter(latitude: Double, longitude: Double) {
        _editing.update { it?.copy(center = GeoCoordinate(latitude, longitude)) }
    }

    fun updateDraftRadius(meters: Float) {
        _editing.update { it?.copy(radiusMeters = meters.coerceIn(20f, 50_000f)) }
    }

    fun updateDraftPriority(priority: Int) {
        _editing.update { it?.copy(priority = priority.coerceIn(1, 100)) }
    }

    fun saveDraft() = viewModelScope.launch {
        val draft = _editing.value ?: return@launch
        val existing = zoneRepository.getZone(draft.id) ?: return@launch
        zoneRepository.upsert(
            existing.copy(
                center = draft.center,
                radiusMeters = draft.radiusMeters.toDouble(),
                priority = draft.priority,
            ),
        )
        analytics.track(AnalyticsEvent.ZoneEdited(viaMap = true))
        _editing.value = null
    }

    fun deleteEditing() = viewModelScope.launch {
        val draft = _editing.value ?: return@launch
        zoneRepository.delete(draft.id)
        analytics.track(AnalyticsEvent.ZoneDeleted)
        _editing.value = null
    }

    private inline fun <T> MutableStateFlow<T>.update(transform: (T) -> T) { value = transform(value) }
}
