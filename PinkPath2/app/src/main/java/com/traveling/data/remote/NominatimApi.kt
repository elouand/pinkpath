package com.traveling.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

interface NominatimApi {
    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 10,
        @Query("accept-language") lang: String = "fr",
        @Query("lat") lat: Double? = null,
        @Query("lon") lon: Double? = null
    ): List<NominatimResult>
}

data class NominatimResult(
    @SerializedName("osm_type") val osmType: String?,
    @SerializedName("osm_id") val osmId: Long?,
    val lat: String,
    val lon: String,
    val name: String?,
    @SerializedName("display_name") val displayName: String
) {
    fun toPhotonFeature() = PhotonFeature(
        properties = PhotonProperties(
            name = name,
            city = displayName,
            osmId = osmId,
            osmType = when (osmType) {
                "node" -> "N"
                "way" -> "W"
                "relation" -> "R"
                else -> null
            }
        ),
        geometry = PhotonGeometry(
            coordinates = listOf(lon.toDouble(), lat.toDouble())
        )
    )
}
