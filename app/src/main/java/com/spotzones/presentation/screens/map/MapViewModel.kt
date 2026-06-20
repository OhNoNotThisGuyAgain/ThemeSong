package com.spotzones.presentation.screens.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spotzones.core.permissions.PermissionChecker
import com.spotzones.domain.location.LocationProvider
import com.spotzones.domain.model.GeoCoordinate
import com.spotzones.domain.model.Zone
import com.spotzones.domain.repository.ZoneRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class MapViewModel @Inject constructor(
    zoneRepository: ZoneRepository,
    private val locationProvider: LocationProvider,
    private val permissions: PermissionChecker,
) : ViewModel() {

    val zones: StateFlow<List<Zone>> =
        zoneRepository.observeZones().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _myLocation = MutableStateFlow<GeoCoordinate?>(null)
    val myLocation: StateFlow<GeoCoordinate?> = _myLocation.asStateFlow()

    /** Whether the map may enable the "my location" layer — enabling it without permission throws. */
    fun hasLocationPermission(): Boolean = permissions.hasAnyLocation()

    init {
        refreshLocation()
    }

    fun refreshLocation() = viewModelScope.launch {
        if (permissions.hasAnyLocation()) {
            _myLocation.value = locationProvider.lastKnownLocation().getOrNull()
        }
    }
}
