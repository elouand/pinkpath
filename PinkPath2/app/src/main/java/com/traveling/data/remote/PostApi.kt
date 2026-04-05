package com.traveling.data.remote

import com.google.gson.annotations.SerializedName
import com.traveling.domain.model.Post
import com.traveling.domain.model.UserData
import com.traveling.NetworkConfig
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

data class CommentRequest(val text: String, val authorId: Int)
data class LikeRequest(val userId: Int)
data class LikeResponse(
    val likes: Int,
    val isLiked: Boolean? = null
)

data class CommentResponse(
    val id: Int,
    val text: String? = null,
    @SerializedName("createdAt") val _createdAt: String? = null,
    @SerializedName("date") val _date: String? = null,
    val author: UserData? = null,
    @SerializedName("authorName") val _authorName: String? = null,
    @SerializedName("authorAvatarUrl") val _authorAvatarUrl: String? = null
) {
    val authorName: String get() = author?.username ?: _authorName ?: "Anonyme"
    
    val createdAt: String get() = _createdAt ?: _date ?: ""

    val authorAvatarUrl: String? get() {
        val rawUrl = author?.profileUrl ?: _authorAvatarUrl
        if (rawUrl == null) return null
        if (rawUrl.startsWith("http")) return rawUrl
        
        // Fix relative URL by prepending the base domain
        val baseUrl = NetworkConfig.BASE_URL.substringBefore("/api")
        return if (rawUrl.startsWith("/")) "$baseUrl$rawUrl" else "$baseUrl/$rawUrl"
    }
}

interface PostApi {
    @GET("photos")
    suspend fun getPosts(@Query("userId") userId: Int? = null): List<Post>

    @Multipart
    @POST("photos")
    suspend fun uploadPhoto(
        @Part image: MultipartBody.Part,
        @Part audio: MultipartBody.Part?,
        @Part("description") description: RequestBody,
        @Part("type_lieu") type_lieu: RequestBody,
        @Part("latitude") latitude: RequestBody,
        @Part("longitude") longitude: RequestBody,
        @Part("is_public") is_public: RequestBody,
        @Part("authorId") authorId: RequestBody?
    ): Post

    @POST("photos/{photoId}/like")
    suspend fun likePost(
        @Path("photoId") photoId: String,
        @Body request: LikeRequest
    ): LikeResponse

    @POST("photos/{photoId}/comments")
    suspend fun addComment(
        @Path("photoId") photoId: String,
        @Body request: CommentRequest
    ): CommentResponse

    @GET("photos/{photoId}/comments")
    suspend fun getComments(@Path("photoId") photoId: String): List<CommentResponse>
}
