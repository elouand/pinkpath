package com.traveling.domain.model

data class RouteResponse(
    val points: List<RoutePoint>,
    val distance: Double,
    val duration: Double
)

data class RoutePoint(
    val latitude: Double,
    val longitude: Double
)
