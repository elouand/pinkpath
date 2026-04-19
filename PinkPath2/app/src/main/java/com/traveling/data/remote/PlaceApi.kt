package com.traveling.data.remote

import com.traveling.domain.model.Place
import com.traveling.domain.model.PlaceDetails
import com.traveling.domain.model.RouteResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface PlaceApi {
    @GET("places/nearby")
    suspend fun getNearbyPlaces(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double
    ): List<Place>

    @GET("places/{id}")
    suspend fun getPlaceDetails(@Path("id") id: String): PlaceDetails

    @GET("route")
    suspend fun getRoute(
        @Query("startLat") startLat: Double,
        @Query("startLon") startLon: Double,
        @Query("endLat") endLat: Double,
        @Query("endLon") endLon: Double,
        @Query("mode") mode: String
    ): RouteResponse
}
