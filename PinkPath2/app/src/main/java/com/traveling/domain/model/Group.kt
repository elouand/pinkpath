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
    val users: List<UserData>? = null
)

data class GroupCount(
    val users: Int = 0,
    val photos: Int = 0,
    val paths: Int = 0
)
