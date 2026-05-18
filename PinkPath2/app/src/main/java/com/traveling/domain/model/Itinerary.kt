package com.traveling.domain.model

data class LocationPoint(val lat: Double, val lon: Double, val name: String)

data class GenerateItineraryRequest(
    val locations: List<LocationPoint>,
    val durationMinutes: Int,
    val mode: String,
    val activities: List<String>,
    val wantsGoodWeather: Boolean = false,
    val budget: Int = 0,
    val effortLevel: String = "normal",
    val weatherSensitivity: List<String> = emptyList(),
    val timeSlot: String = "all"
)

data class WeatherDay(
    val date: String,
    val label: String,
    val condition: String,
    val isGood: Boolean
)

data class ItineraryStep(
    val name: String,
    val type: String?,
    val lat: Double,
    val lon: Double,
    val photoUrl: String? = null,
    val openingHours: String? = null,
    val avgCost: Int = 0,
    val costIsReal: Boolean = false,
    val postPhotoUrls: List<String> = emptyList()
)

data class ItineraryVariant(
    val name: String,
    val description: String,
    val estimatedDuration: Int,
    val estimatedDistance: Int,
    val steps: List<ItineraryStep>,
    val estimatedBudget: Int = 0,
    val effortScore: Int = 1,
    val suggestedSlot: String? = null
)

data class ItineraryVariantsResponse(
    val variants: List<ItineraryVariant>,
    val goodWeatherDays: List<WeatherDay>? = null
)

data class SaveItineraryRequest(
    val userId: Int,
    val name: String,
    val duration: Int,
    val distance: Double,
    val mode: String,
    val steps: List<ItineraryStep>
)

data class SavedItinerary(
    val id: Int,
    val name: String,
    val duration: Int,
    val distance: Double,
    val mode: String,
    val stepsCount: Int,
    val createdAt: String
)

data class ItineraryStepFull(
    val id: Int,
    val order: Int,
    val name: String,
    val type: String?,
    val latitude: Double,
    val longitude: Double
)

data class ItineraryFull(
    val id: Int,
    val name: String,
    val duration: Int,
    val distance: Double,
    val mode: String,
    val steps: List<ItineraryStepFull>
)

data class MultiRouteResponse(
    val points: List<RoutePoint>,
    val distance: Int,
    val duration: Int
)

data class UpdateStepRequest(
    val name: String,
    val type: String?,
    val latitude: Double,
    val longitude: Double
)

data class UpdateItineraryRequest(
    val name: String,
    val steps: List<UpdateStepRequest>
)

data class ShareItineraryRequest(
    val userId: Int,
    val description: String,
    val isPublic: Boolean = true,
    val groupId: Int? = null
)

data class CopyItineraryRequest(val userId: Int)

data class CommunityPost(
    val photoUrl: String,
    val description: String?,
    val authorPseudo: String?
)

data class StepDetailResponse(
    val name: String,
    val wikiSummary: String?,
    val wikiPhotoUrl: String?,
    val communityPosts: List<CommunityPost>
)
