package com.traveling.data.repository

import com.traveling.data.remote.ItineraryApi
import com.traveling.domain.model.CopyItineraryRequest
import com.traveling.domain.model.GenerateItineraryRequest
import com.traveling.domain.model.ItineraryFull
import com.traveling.domain.model.ItineraryStepFull
import com.traveling.domain.model.ItineraryVariant
import com.traveling.domain.model.ItineraryVariantsResponse
import com.traveling.domain.model.LocationPoint
import com.traveling.domain.model.MultiRouteResponse
import com.traveling.domain.model.SaveItineraryRequest
import com.traveling.domain.model.SavedItinerary
import com.traveling.domain.model.UpdateItineraryRequest
import com.traveling.domain.model.UpdateStepRequest
import com.traveling.domain.repository.ItineraryRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItineraryRepositoryImpl @Inject constructor(
    private val api: ItineraryApi
) : ItineraryRepository {

    override suspend fun generateItineraries(
        locations: List<LocationPoint>,
        durationMinutes: Int,
        mode: String,
        activities: List<String>,
        wantsGoodWeather: Boolean
    ): Result<ItineraryVariantsResponse> = try {
        val response = api.generateItineraries(
            GenerateItineraryRequest(locations, durationMinutes, mode, activities, wantsGoodWeather)
        )
        Result.success(response)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun saveItinerary(
        userId: Int,
        name: String,
        variant: ItineraryVariant,
        mode: String
    ): Result<SavedItinerary> = try {
        val saved = api.saveItinerary(
            SaveItineraryRequest(
                userId = userId,
                name = name,
                duration = variant.estimatedDuration,
                distance = variant.estimatedDistance.toDouble(),
                mode = mode,
                steps = variant.steps
            )
        )
        Result.success(saved)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getUserItineraries(userId: Int): Result<List<SavedItinerary>> = try {
        Result.success(api.getUserItineraries(userId))
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getItineraryById(id: Int): Result<ItineraryFull> = try {
        Result.success(api.getItineraryById(id))
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getMultiWaypointRoute(waypoints: String, mode: String): Result<MultiRouteResponse> = try {
        Result.success(api.getMultiWaypointRoute(waypoints, mode))
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updateItinerary(id: Int, name: String, steps: List<ItineraryStepFull>): Result<ItineraryFull> = try {
        val request = UpdateItineraryRequest(
            name = name,
            steps = steps.map { UpdateStepRequest(it.name, it.type, it.latitude, it.longitude) }
        )
        Result.success(api.updateItinerary(id, request))
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun copyItinerary(itineraryId: Int, userId: Int): Result<SavedItinerary> = try {
        Result.success(api.copyItinerary(itineraryId, CopyItineraryRequest(userId)))
    } catch (e: Exception) {
        Result.failure(e)
    }
}
