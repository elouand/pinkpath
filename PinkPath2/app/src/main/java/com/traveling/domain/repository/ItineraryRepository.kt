package com.traveling.domain.repository

import com.traveling.domain.model.ItineraryFull
import com.traveling.domain.model.ItineraryStepFull
import com.traveling.domain.model.ItineraryVariant
import com.traveling.domain.model.ItineraryVariantsResponse
import com.traveling.domain.model.LocationPoint
import com.traveling.domain.model.MultiRouteResponse
import com.traveling.domain.model.SavedItinerary

interface ItineraryRepository {
    suspend fun generateItineraries(
        locations: List<LocationPoint>,
        durationMinutes: Int,
        mode: String,
        activities: List<String>,
        wantsGoodWeather: Boolean = false
    ): Result<ItineraryVariantsResponse>

    suspend fun saveItinerary(
        userId: Int,
        name: String,
        variant: ItineraryVariant,
        mode: String
    ): Result<SavedItinerary>

    suspend fun getUserItineraries(userId: Int): Result<List<SavedItinerary>>

    suspend fun getItineraryById(id: Int): Result<ItineraryFull>

    suspend fun getMultiWaypointRoute(waypoints: String, mode: String): Result<MultiRouteResponse>

    suspend fun updateItinerary(id: Int, name: String, steps: List<ItineraryStepFull>): Result<ItineraryFull>
    suspend fun copyItinerary(itineraryId: Int, userId: Int): Result<SavedItinerary>
}
