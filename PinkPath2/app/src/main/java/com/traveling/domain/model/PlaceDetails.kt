package com.traveling.domain.model

data class PlaceDetails(
    val id: String,
    val name: String,
    val type: String,
    val latitude: Double,
    val longitude: Double,
    val description: String?,
    val website: String?,
    val phone: String?,
    val email: String?,
    val address: Address?,
    val opening_hours: String?,
    val cuisine: String?,
    val stars: Int?,
    val wheelchair: String?,
    val internet_access: String?,
    val photos: List<PlacePhoto> = emptyList()
)

data class Address(
    val street: String?,
    val housenumber: String?,
    val city: String?,
    val postcode: String?,
    val country: String?
)

data class PlacePhoto(
    val url: String,
    val thumb: String,
    val title: String
)
