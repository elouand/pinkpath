package com.traveling.ui.travelpath

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.traveling.domain.model.PlaceDetails
import com.traveling.domain.model.RoutePoint
import com.traveling.domain.repository.PlaceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaceViewModel @Inject constructor(
    private val repository: PlaceRepository
) : ViewModel() {

    private val _state = MutableStateFlow<PlaceDetailState>(PlaceDetailState.Loading)
    val state = _state.asStateFlow()

    // État pour la navigation après calcul d'itinéraire
    private val _routeCalculated = MutableStateFlow<List<RoutePoint>?>(null)
    val routeCalculated = _routeCalculated.asStateFlow()

    fun loadPlaceDetails(id: String) {
        viewModelScope.launch {
            _state.value = PlaceDetailState.Loading
            repository.getPlaceDetails(id)
                .onSuccess { details ->
                    Log.d("PlaceViewModel", "✅ Détails reçus : $details")
                    _state.value = PlaceDetailState.Success(details)
                }
                .onFailure { error ->
                    Log.e("PlaceViewModel", "❌ Erreur : ${error.message}")
                    _state.value = PlaceDetailState.Error(error.message ?: "Erreur inconnue")
                }
        }
    }

    fun calculateRoute(startLat: Double, startLon: Double, details: PlaceDetails, mode: String) {
        viewModelScope.launch {
            repository.getRoute(startLat, startLon, details.latitude, details.longitude, mode)
                .onSuccess { route ->
                    // On pourrait stocker le trajet ici si on voulait le partager
                    // Mais pour l'instant on va juste signaler que c'est prêt
                    _routeCalculated.value = route.points
                }
        }
    }
}

sealed class PlaceDetailState {
    object Loading : PlaceDetailState()
    data class Success(val details: PlaceDetails) : PlaceDetailState()
    data class Error(val message: String) : PlaceDetailState()
}
