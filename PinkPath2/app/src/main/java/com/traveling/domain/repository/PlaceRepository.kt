package com.traveling.domain.repository

import com.traveling.domain.model.Place
import com.traveling.domain.model.PlaceDetails
import com.traveling.domain.model.RouteResponse

interface PlaceRepository {
    suspend fun getNearbyPlaces(lat: Double, lon: Double): Result<List<Place>>
    suspend fun getPlaceDetails(id: String): Result<PlaceDetails>
    suspend fun getRoute(
        startLat: Double, startLon: Double,
        endLat: Double, endLon: Double,
        mode: String
    ): Result<RouteResponse>
}
