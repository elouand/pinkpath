package com.traveling.ui.travelpath

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import com.traveling.NetworkConfig
import com.traveling.data.local.ItineraryLocalStore
import com.traveling.data.local.SessionManager
import com.traveling.data.remote.PhotonApi
import com.traveling.data.remote.PhotonFeature
import com.traveling.domain.model.ItineraryFull
import com.traveling.domain.model.ItineraryStepFull
import com.traveling.domain.model.ItineraryVariant
import com.traveling.domain.model.LocationPoint
import com.traveling.domain.model.RoutePoint
import com.traveling.domain.model.SavedItinerary
import com.traveling.domain.model.WeatherDay
import com.traveling.domain.repository.ItineraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ItineraryViewModel @Inject constructor(
    private val repository: ItineraryRepository,
    private val sessionManager: SessionManager,
    private val photonApi: PhotonApi,
    private val localStore: ItineraryLocalStore
) : ViewModel() {

    data class UiState(
        val isGenerating: Boolean = false,
        val isSaving: Boolean = false,
        val variants: List<ItineraryVariant>? = null,
        val goodWeatherDays: List<WeatherDay>? = null,
        val savedItineraries: List<SavedItinerary> = emptyList(),
        val saveSuccess: Boolean = false,
        val error: String? = null,
        val searchResults: List<PhotonFeature> = emptyList(),
        val activeItinerary: ItineraryFull? = null,
        val activeRoute: List<RoutePoint>? = null,
        val editItinerary: ItineraryFull? = null,
        val isLoadingItinerary: Boolean = false,
        val copySuccess: Boolean = false,
        val likedIds: Set<Int> = emptySet(),
        val isOffline: Boolean = false
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        _uiState.value = _uiState.value.copy(likedIds = localStore.getLikedIds())
        loadItineraries()
    }

    fun searchLocation(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(searchResults = emptyList())
            return
        }
        searchJob = viewModelScope.launch {
            delay(400)
            try {
                val response = photonApi.search(query, limit = 5)
                _uiState.value = _uiState.value.copy(
                    searchResults = response.features.filter { !it.properties.name.isNullOrBlank() }
                )
            } catch (_: Exception) {}
        }
    }

    fun clearSearchResults() {
        _uiState.value = _uiState.value.copy(searchResults = emptyList())
    }

    fun generateItineraries(
        locations: List<LocationPoint>,
        durationMinutes: Int,
        mode: String,
        activities: List<String>,
        wantsGoodWeather: Boolean = false,
        budget: Int = 0,
        effortLevel: String = "normal",
        weatherSensitivity: List<String> = emptyList(),
        timeSlot: String = "all"
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGenerating = true, error = null, variants = null, goodWeatherDays = null)
            repository.generateItineraries(locations, durationMinutes, mode, activities, wantsGoodWeather, budget, effortLevel, weatherSensitivity, timeSlot)
                .onSuccess { response ->
                    _uiState.value = _uiState.value.copy(
                        variants = response.variants,
                        goodWeatherDays = response.goodWeatherDays,
                        isGenerating = false
                    )
                }
                .onFailure { _uiState.value = _uiState.value.copy(error = it.message, isGenerating = false) }
        }
    }

    fun saveItinerary(name: String, variant: ItineraryVariant, mode: String) {
        val userId = sessionManager.getUserId()?.toIntOrNull()
        if (userId == null) {
            _uiState.value = _uiState.value.copy(error = "Connectez-vous pour sauvegarder un itinéraire")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            repository.saveItinerary(userId, name, variant, mode)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isSaving = false, saveSuccess = true, variants = null)
                    loadItineraries()
                }
                .onFailure { _uiState.value = _uiState.value.copy(isSaving = false, error = it.message) }
        }
    }

    fun loadItineraries() {
        val userId = sessionManager.getUserId()?.toIntOrNull() ?: return
        viewModelScope.launch {
            repository.getUserItineraries(userId)
                .onSuccess { list ->
                    localStore.saveCachedItineraries(list)
                    _uiState.value = _uiState.value.copy(savedItineraries = list, isOffline = false)
                }
                .onFailure {
                    val cached = localStore.getCachedItineraries()
                    if (cached.isNotEmpty()) {
                        _uiState.value = _uiState.value.copy(savedItineraries = cached, isOffline = true)
                    }
                }
        }
    }

    fun toggleLike(id: Int) {
        localStore.toggleLike(id)
        _uiState.value = _uiState.value.copy(likedIds = localStore.getLikedIds())
    }

    fun clearSaveSuccess() { _uiState.value = _uiState.value.copy(saveSuccess = false) }
    fun clearVariants() { _uiState.value = _uiState.value.copy(variants = null, goodWeatherDays = null) }
    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }

    fun startItinerary(itinerary: SavedItinerary) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingItinerary = true, error = null, activeRoute = null)
            repository.getItineraryById(itinerary.id)
                .onSuccess { full ->
                    _uiState.value = _uiState.value.copy(
                        activeItinerary = full,
                        activeRoute = null,
                        isLoadingItinerary = false
                    )
                }
                .onFailure { _uiState.value = _uiState.value.copy(error = it.message, isLoadingItinerary = false) }
        }
    }

    fun recalculateRoute(startLat: Double, startLon: Double) {
        val steps = _uiState.value.activeItinerary?.steps ?: return
        val mode = _uiState.value.activeItinerary?.mode ?: "walking"
        val waypoints = buildString {
            append("$startLat,$startLon")
            steps.forEach { append(";${it.latitude},${it.longitude}") }
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingItinerary = true)
            repository.getMultiWaypointRoute(waypoints, mode)
                .onSuccess { _uiState.value = _uiState.value.copy(activeRoute = it.points, isLoadingItinerary = false) }
                .onFailure { _uiState.value = _uiState.value.copy(isLoadingItinerary = false) }
        }
    }

    fun goToNextStep() {
        val current = _uiState.value.activeItinerary ?: return
        val remaining = current.steps.drop(1)
        if (remaining.isEmpty()) {
            clearActiveRoute()
            return
        }
        _uiState.value = _uiState.value.copy(
            activeItinerary = current.copy(steps = remaining),
            activeRoute = null
        )
    }

    fun clearActiveRoute() {
        _uiState.value = _uiState.value.copy(activeRoute = null, activeItinerary = null)
    }

    fun startEdit(itinerary: SavedItinerary) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingItinerary = true)
            repository.getItineraryById(itinerary.id)
                .onSuccess { _uiState.value = _uiState.value.copy(editItinerary = it, isLoadingItinerary = false) }
                .onFailure { _uiState.value = _uiState.value.copy(error = it.message, isLoadingItinerary = false) }
        }
    }

    fun clearEditItinerary() {
        _uiState.value = _uiState.value.copy(editItinerary = null)
    }

    fun updateItinerary(id: Int, name: String, steps: List<ItineraryStepFull>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            repository.updateItinerary(id, name, steps)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isSaving = false, saveSuccess = true, editItinerary = null)
                    loadItineraries()
                }
                .onFailure { _uiState.value = _uiState.value.copy(isSaving = false, error = it.message) }
        }
    }

    fun copyItinerary(itineraryId: Int, userId: Int) {
        viewModelScope.launch {
            repository.copyItinerary(itineraryId, userId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(copySuccess = true)
                    loadItineraries()
                }
                .onFailure { _uiState.value = _uiState.value.copy(error = it.message) }
        }
    }

    fun clearCopySuccess() { _uiState.value = _uiState.value.copy(copySuccess = false) }

    fun downloadPdf(itineraryId: Int, itineraryName: String, context: Context) {
        val url = "${NetworkConfig.BASE_URL}itineraries/$itineraryId/pdf"
        val safeName = itineraryName.replace(Regex("[^a-zA-Z0-9]"), "_")
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("Itinéraire – $itineraryName")
            .setDescription("Téléchargement du PDF…")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "itineraire_$safeName.pdf")
            .setMimeType("application/pdf")
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        dm.enqueue(request)
    }
}
