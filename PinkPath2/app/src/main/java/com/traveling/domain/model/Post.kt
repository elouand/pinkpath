package com.traveling.domain.model

import com.google.gson.annotations.SerializedName

data class SharedItineraryData(
    val id: Int,
    val name: String,
    val duration: Int,
    val distance: Double,
    val mode: String,
    val steps: List<ItineraryStepFull> = emptyList()
)

data class Post(
    val id: String,
    val title: String? = null,
    val content: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val imageUrl: String? = null,
    val audioUrl: String? = null,

    @SerializedName("author")
    val authorName: String? = null,

    @SerializedName("authorAvatarUrl")
    val authorAvatar: String? = null,

    val likes: Int = 0,

    @SerializedName("commentCount")
    val commentsCount: Int = 0,

    @SerializedName("isLiked")
    val isLiked: Boolean = false,

    val tags: List<String>? = null,
    val groupName: String? = null,

    @SerializedName("isPublic")
    val isPublic: Boolean = true,

    val itineraryId: Int? = null,
    val sharedItinerary: SharedItineraryData? = null
) {
    val fullImageUrl: String? get() = imageUrl
    val displayLikes: Int get() = likes
}
