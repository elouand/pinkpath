package com.traveling.domain.model

import com.google.gson.annotations.SerializedName

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
    
    // Le serveur envoie "isPublic" dans le GET
    @SerializedName("isPublic")
    val isPublic: Boolean = true
) {
    val fullImageUrl: String? get() = imageUrl
    val displayLikes: Int get() = likes
}
