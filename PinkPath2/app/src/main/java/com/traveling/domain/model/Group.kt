package com.traveling.domain.model

import com.google.gson.annotations.SerializedName

data class Group(
    val id: Int,
    val name: String,
    val description: String? = null,
    @SerializedName("imageUrl")
    val imageUrl: String? = null,
    @SerializedName("count")
    val count: GroupCount? = null,
    val users: List<UserData>? = null,
    val photos: List<Post>? = null,
    val userRole: String? = null, // "ADMIN" ou "MEMBER"
    @SerializedName("isPublic")
    val isPublic: Boolean = true,
    val isPending: Boolean = false
)

data class GroupCount(
    val users: Int = 0,
    val photos: Int = 0,
    val paths: Int = 0
)

data class JoinRequest(
    val id: Int,
    val userId: Int,
    val username: String,
    val userAvatar: String? = null,
    val createdAt: String
)
