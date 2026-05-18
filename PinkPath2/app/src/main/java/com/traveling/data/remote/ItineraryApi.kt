package com.traveling.data.remote

import com.traveling.domain.model.CopyItineraryRequest
import com.traveling.domain.model.GenerateItineraryRequest
import com.traveling.domain.model.ItineraryFull
import com.traveling.domain.model.ItineraryVariantsResponse
import com.traveling.domain.model.MultiRouteResponse
import com.traveling.domain.model.SaveItineraryRequest
import com.traveling.domain.model.SavedItinerary
import com.traveling.domain.model.StepDetailResponse
import com.traveling.domain.model.UpdateItineraryRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ItineraryApi {
    @POST("itineraries/generate")
    suspend fun generateItineraries(@Body request: GenerateItineraryRequest): ItineraryVariantsResponse

    @POST("itineraries/save")
    suspend fun saveItinerary(@Body request: SaveItineraryRequest): SavedItinerary

    @GET("users/{userId}/itineraries")
    suspend fun getUserItineraries(@Path("userId") userId: Int): List<SavedItinerary>

    @GET("itineraries/{id}")
    suspend fun getItineraryById(@Path("id") id: Int): ItineraryFull

    @GET("route/multi")
    suspend fun getMultiWaypointRoute(
        @Query("waypoints") waypoints: String,
        @Query("mode") mode: String
    ): MultiRouteResponse

    @PATCH("itineraries/{id}")
    suspend fun updateItinerary(
        @Path("id") id: Int,
        @Body request: UpdateItineraryRequest
    ): ItineraryFull

    @POST("itineraries/{id}/copy")
    suspend fun copyItinerary(
        @Path("id") id: Int,
        @Body request: CopyItineraryRequest
    ): SavedItinerary

    @GET("places/step-details")
    suspend fun getStepDetails(
        @Query("name") name: String,
        @Query("lat") lat: Double,
        @Query("lon") lon: Double
    ): StepDetailResponse
}
