package com.traveling.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

interface PhotonApi {
    @GET("api")
    suspend fun search(
        @Query("q") query: String,
        @Query("limit") limit: Int = 10,
        @Query("lang") lang: String = "fr",
        @Query("lat") lat: Double? = null,
        @Query("lon") lon: Double? = null
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
    val name: String? = null,
    val city: String? = null,
    val country: String? = null,
    val street: String? = null,
    val postcode: String? = null,
    @SerializedName("osm_id") val osmId: Long? = null,
    @SerializedName("osm_type") val osmType: String? = null
) {
    val displayName: String get() = listOfNotNull(street, postcode, city, country).distinct().joinToString(", ")
}

data class PhotonGeometry(
    val coordinates: List<Double>
) {
    val longitude: Double get() = coordinates.getOrNull(0) ?: 0.0
    val latitude: Double get() = coordinates.getOrNull(1) ?: 0.0
}
