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

data class UserSearchResult(
    val id: Int,
    val username: String,
    val pseudo: String? = null,
    val profileUrl: String? = null,
    val followersCount: Int = 0
)

data class PublicUserProfile(
    val id: Int,
    val username: String,
    val pseudo: String? = null,
    val profileUrl: String? = null,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val isFollowing: Boolean = false,
    val notifyEnabled: Boolean = false,
    val posts: List<Post> = emptyList()
)

data class AppNotification(
    val id: Int,
    val type: String,
    val photoId: Int? = null,
    val eventId: Int? = null,
    val groupId: Int? = null,
    val eventTitle: String? = null,
    val read: Boolean,
    val createdAt: String,
    val fromId: Int,
    val fromUsername: String,
    val fromPseudo: String? = null,
    val fromProfileUrl: String? = null
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
