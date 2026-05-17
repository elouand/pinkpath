package com.traveling.domain.model

data class RouteInstruction(
    val text: String,
    val distance: Int,
    val duration: Int,
    val direction: String = "straight"
)

data class RouteResponse(
    val points: List<RoutePoint>,
    val distance: Int,
    val duration: Int,
    val instructions: List<RouteInstruction> = emptyList()
)

data class RoutePoint(
    val latitude: Double,
    val longitude: Double
)
