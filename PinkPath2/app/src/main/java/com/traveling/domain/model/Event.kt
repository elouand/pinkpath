package com.traveling.domain.model

data class GroupEvent(
    val id: Int,
    val title: String? = null,
    val description: String? = null,
    val startAt: String,
    val groupId: Int,
    val itineraryId: Int? = null,
    val itineraryName: String? = null,
    val itinerary: SharedItineraryData? = null,
    val createdById: Int,
    val createdByName: String? = null,
    val interestedCount: Int = 0,
    val isInterested: Boolean = false,
    val interestedUsers: List<UserSearchResult> = emptyList()
)

data class CreateEventRequest(
    val title: String? = null,
    val description: String? = null,
    val startAt: String,
    val itineraryId: Int? = null,
    val createdById: Int
)

data class InterestRequest(val userId: Int)
