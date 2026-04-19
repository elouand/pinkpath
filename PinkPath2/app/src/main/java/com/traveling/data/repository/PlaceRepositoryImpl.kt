package com.traveling.data.repository

import android.util.Log
import com.traveling.data.remote.PlaceApi
import com.traveling.domain.model.Place
import com.traveling.domain.model.PlaceDetails
import com.traveling.domain.model.RouteResponse
import com.traveling.domain.repository.PlaceRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaceRepositoryImpl @Inject constructor(
    private val api: PlaceApi
) : PlaceRepository {
    override suspend fun getNearbyPlaces(lat: Double, lon: Double): Result<List<Place>> {
        return try {
            Log.d("PlaceRepository", "📡 Appel nearby: lat=$lat, lon=$lon")
            val places = api.getNearbyPlaces(lat, lon)
            Log.d("PlaceRepository", "✅ Reponse nearby: ${places.size} lieux trouvés")
            Result.success(places)
        } catch (e: Exception) {
            Log.e("PlaceRepository", "💥 Erreur nearby: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun getPlaceDetails(id: String): Result<PlaceDetails> {
        return try {
            Log.d("PlaceRepository", "📡 Appel details: id=$id")
            val details = api.getPlaceDetails(id)
            Log.d("PlaceRepository", "✅ Reponse details: ${details.name}")
            Result.success(details)
        } catch (e: Exception) {
            Log.e("PlaceRepository", "💥 Erreur details: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun getRoute(
        startLat: Double,
        startLon: Double,
        endLat: Double,
        endLon: Double,
        mode: String
    ): Result<RouteResponse> {
        return try {
            Log.d("PlaceRepository", "📡 Appel route: from($startLat,$startLon) to($endLat,$endLon) mode=$mode")
            val route = api.getRoute(startLat, startLon, endLat, endLon, mode)
            Log.d("PlaceRepository", "✅ Reponse route: ${route.points.size} points reçus")
            Result.success(route)
        } catch (e: Exception) {
            Log.e("PlaceRepository", "💥 Erreur route: ${e.message}", e)
            Result.failure(e)
        }
    }
}
