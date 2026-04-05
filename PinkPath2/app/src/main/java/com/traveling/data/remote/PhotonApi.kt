package com.traveling.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface PhotonApi {
    @GET("api")
    suspend fun search(
        @Query("q") query: String,
        @Query("limit") limit: Int = 5,
        @Query("lang") lang: String = "fr"
    ): PhotonResponse
}

data class PhotonResponse(
    val features: List<PhotonFeature>
)

data class PhotonFeature(
    val properties: PhotonProperties,
    val geometry: PhotonGeometry
)

data class PhotonProperties(
    val name: String,
    val city: String? = null,
    val country: String? = null,
    val street: String? = null
) {
    val displayName: String get() = listOfNotNull(name, city, country).joinToString(", ")
}

data class PhotonGeometry(
    val coordinates: List<Double>
) {
    val longitude: Double get() = coordinates[0]
    val latitude: Double get() = coordinates[1]
}
