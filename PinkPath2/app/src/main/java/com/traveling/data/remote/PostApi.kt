package com.traveling.data.remote

import com.google.gson.annotations.SerializedName
import com.traveling.domain.model.Post
import com.traveling.domain.model.Group
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.*

data class CommentRequest(
    @SerializedName("text") val text: String, 
    @SerializedName("userId") val userId: Int
)

data class LikeRequest(val userId: Int)

data class LikeResponse(
    val likes: Int,
    val isLiked: Boolean? = null
)

data class CommentResponse(
    val id: Int,
    val text: String? = null,
    @SerializedName("date") val date: String? = null,
    @SerializedName("authorName") val authorName: String? = null,
    @SerializedName("authorAvatarUrl") val authorAvatarUrl: String? = null
)

data class AddUserToGroupRequest(
    val usernameToAdd: String
)

interface PostApi {
    @GET("photos")
    suspend fun getPosts(@Query("userId") userId: Int? = null): List<Post>

    @POST("photos")
    suspend fun uploadPhoto(@Body body: RequestBody): ResponseBody

    @POST("photos/{photoId}/like")
    suspend fun likePost(
        @Path("photoId") photoId: String,
        @Body request: LikeRequest
    ): LikeResponse

    @GET("photos/{photoId}/comments")
    suspend fun getComments(@Path("photoId") photoId: String): List<CommentResponse>

    @POST("photos/{photoId}/comments")
    suspend fun addComment(
        @Path("photoId") photoId: String,
        @Body request: CommentRequest
    ): CommentResponse

    @GET("users/{userId}/groups")
    suspend fun getUserGroups(@Path("userId") userId: Int): List<Group>

    @POST("groups")
    suspend fun createGroup(@Body body: RequestBody): Group

    @POST("groups/{groupId}/add-user")
    suspend fun addUserToGroup(
        @Path("groupId") groupId: Int,
        @Body request: AddUserToGroupRequest
    ): Map<String, Any>
}
