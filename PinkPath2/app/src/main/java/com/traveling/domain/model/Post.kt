package com.traveling.domain.model

import com.google.gson.annotations.SerializedName
import com.traveling.NetworkConfig

data class Post(
    val id: String,
    val title: String? = null,
    val content: String? = null,
    
    @SerializedName("author") val authorNameFromServer: String? = null,
    @SerializedName("authorAvatarUrl") val authorAvatarFromServer: String? = null,
    
    val user: UserData? = null,

    val location: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val tags: List<String>? = null,
    val likes: Int? = null,
    @SerializedName("likesCount") val likesCount: Int? = null,
    @SerializedName("isLiked", alternate = ["is_liked", "liked"]) val isLiked: Boolean = false,
    @SerializedName("commentCount") val commentsCount: Int? = null,
    val imageUrl: String? = null,
    val audioUrl: String? = null,
    val createdAt: String? = null,
    val isPublic: Boolean? = null
) {
    val authorName: String get() = authorNameFromServer ?: user?.username ?: "Anonyme"
    
    val authorAvatar: String? get() {
        val rawUrl = authorAvatarFromServer ?: user?.profileUrl
        if (rawUrl == null) return null
        if (rawUrl.startsWith("http")) return rawUrl
        val baseUrl = NetworkConfig.BASE_URL.substringBefore("/api")
        return if (rawUrl.startsWith("/")) "$baseUrl$rawUrl" else "$baseUrl/$rawUrl"
    }

    val fullImageUrl: String? get() {
        if (imageUrl == null) return null
        if (imageUrl.startsWith("http")) return imageUrl
        val baseUrl = NetworkConfig.BASE_URL.substringBefore("/api")
        return if (imageUrl.startsWith("/")) "$baseUrl$imageUrl" else "$baseUrl/$imageUrl"
    }

    val displayLikes: Int get() = likes ?: likesCount ?: 0
}
