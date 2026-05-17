package com.traveling.ui.travelpath

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.traveling.data.remote.PhotonApi
import com.traveling.data.remote.PhotonFeature
import com.traveling.domain.model.Place
import com.traveling.domain.model.RouteInstruction
import com.traveling.domain.model.RoutePoint
import com.traveling.domain.repository.PlaceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MapUiState(
    val places: List<Place> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchResults: List<PhotonFeature> = emptyList(),
    val isSearching: Boolean = false,
    val currentRoute: List<RoutePoint>? = null,
    val isCalculatingRoute: Boolean = false,
    val userLocation: RoutePoint? = null,
    val pendingCenter: Pair<Double, Double>? = null,
    val pendingPlaceId: String? = null,
    val isSearchingDetails: Boolean = false,
    val routeDistance: Int = 0,
    val routeDuration: Int = 0,
    val routeMode: String = "",
    val routeInstructions: List<RouteInstruction> = emptyList(),
    val currentInstructionIndex: Int = 0,
    val isNavigating: Boolean = false
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val repository: PlaceRepository,
    private val photonApi: PhotonApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun updateUserLocation(lat: Double, lon: Double) {
        _uiState.value = _uiState.value.copy(userLocation = RoutePoint(lat, lon))
    }

    fun fetchNearbyPlaces(lat: Double, lon: Double) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repository.getNearbyPlaces(lat, lon)
                .onSuccess { places ->
                    _uiState.value = _uiState.value.copy(places = places, isLoading = false)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(error = error.message, isLoading = false)
                }
        }
    }

    fun onSearchQueryChanged(query: String, lat: Double? = null, lon: Double? = null) {
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(searchResults = emptyList(), isSearching = false)
            return
        }

        searchJob = viewModelScope.launch {
            delay(500)
            _uiState.value = _uiState.value.copy(isSearching = true)
            try {
                val response = photonApi.search(query = query, lat = lat, lon = lon)
                _uiState.value = _uiState.value.copy(searchResults = response.features, isSearching = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSearching = false)
            }
        }
    }
    
    fun calculateRoute(startLat: Double, startLon: Double, endLat: Double, endLon: Double, mode: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isCalculatingRoute = true, 
                error = null,
                currentRoute = null 
            )
            repository.getRoute(startLat, startLon, endLat, endLon, mode)
                .onSuccess { route ->
                    _uiState.value = _uiState.value.copy(
                        currentRoute = route.points,
                        isCalculatingRoute = false,
                        routeDistance = route.distance,
                        routeDuration = route.duration,
                        routeInstructions = route.instructions,
                        routeMode = mode,
                        currentInstructionIndex = 0
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        error = "Erreur itinéraire: ${error.message}",
                        isCalculatingRoute = false
                    )
                }
        }
    }

    fun clearRoute() {
        _uiState.value = _uiState.value.copy(
            currentRoute = null,
            routeDistance = 0,
            routeDuration = 0,
            routeInstructions = emptyList(),
            isNavigating = false,
            currentInstructionIndex = 0
        )
    }

    fun startNavigation() {
        _uiState.value = _uiState.value.copy(isNavigating = true)
    }

    fun stopNavigation() {
        _uiState.value = _uiState.value.copy(isNavigating = false, currentInstructionIndex = 0)
    }

    fun nextInstruction() {
        val idx = _uiState.value.currentInstructionIndex
        val max = _uiState.value.routeInstructions.size - 1
        if (idx < max) _uiState.value = _uiState.value.copy(currentInstructionIndex = idx + 1)
    }

    fun prevInstruction() {
        val idx = _uiState.value.currentInstructionIndex
        if (idx > 0) _uiState.value = _uiState.value.copy(currentInstructionIndex = idx - 1)
    }

    fun setPendingCenter(lat: Double, lon: Double) {
        _uiState.value = _uiState.value.copy(pendingCenter = Pair(lat, lon))
    }

    fun clearPendingCenter() {
        _uiState.value = _uiState.value.copy(pendingCenter = null)
    }

    fun searchPlaceForNavigation(name: String, lat: Double, lon: Double) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearchingDetails = true)
            try {
                val results = photonApi.search(query = name, limit = 10, lat = lat, lon = lon)
                val match = results.features
                    .filter { it.properties.osmType == "N" && it.properties.osmId != null }
                    .minByOrNull { f ->
                        val dLat = f.geometry.latitude - lat
                        val dLon = f.geometry.longitude - lon
                        dLat * dLat + dLon * dLon
                    }
                _uiState.value = _uiState.value.copy(
                    pendingPlaceId = match?.properties?.osmId?.toString(),
                    isSearchingDetails = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSearchingDetails = false)
            }
        }
    }

    fun clearPendingPlaceId() {
        _uiState.value = _uiState.value.copy(pendingPlaceId = null)
    }
}
