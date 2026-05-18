package com.traveling.data.remote

import com.google.gson.annotations.SerializedName
import com.traveling.domain.model.AppNotification
import com.traveling.domain.model.CreateEventRequest
import com.traveling.domain.model.GroupEvent
import com.traveling.domain.model.InterestRequest
import com.traveling.domain.model.Post
import com.traveling.domain.model.Group
import com.traveling.domain.model.JoinRequest
import com.traveling.domain.model.PublicUserProfile
import com.traveling.domain.model.ReportedPost
import com.traveling.domain.model.ReportRequest
import com.traveling.domain.model.ShareItineraryRequest
import com.traveling.domain.model.SuggestTagsResponse
import com.traveling.domain.model.UserSearchResult
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

data class JoinGroupRequest(
    val userId: Int
)

data class FollowRequest(val followerId: Int)
data class NotifyRequest(val followerId: Int, val notify: Boolean)
data class ReadAllRequest(val userId: Int)

interface PostApi {
    @GET("photos")
    suspend fun getPosts(@Query("userId") userId: Int? = null): List<Post>

    @GET("photos/following")
    suspend fun getFollowingPosts(@Query("userId") userId: Int): List<Post>

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

    @POST("itineraries/{id}/share")
    suspend fun shareItinerary(
        @Path("id") id: Int,
        @Body request: ShareItineraryRequest
    ): Post

    @GET("groups/{groupId}")
    suspend fun getGroupDetails(@Path("groupId") groupId: Int): Group

    @GET("groups/{groupId}/requests")
    suspend fun getJoinRequests(@Path("groupId") groupId: Int): List<JoinRequest>

    @POST("groups/{groupId}/requests/{requestId}/respond")
    suspend fun respondToJoinRequest(
        @Path("groupId") groupId: Int,
        @Path("requestId") requestId: Int,
        @Body body: Map<String, String>
    ): Map<String, Any>

    @GET("groups/search")
    suspend fun searchGroups(
        @Query("query") query: String,
        @Query("userId") userId: Int? = null
    ): List<Group>

    @POST("groups/{groupId}/join")
    suspend fun joinGroup(@Path("groupId") groupId: Int, @Body request: JoinGroupRequest): Map<String, Any>

    @GET("users/search")
    suspend fun searchUsers(@Query("query") query: String): List<UserSearchResult>

    @GET("users/{userId}/profile")
    suspend fun getUserProfile(
        @Path("userId") userId: Int,
        @Query("followerId") followerId: Int? = null
    ): PublicUserProfile

    @POST("users/{userId}/follow")
    suspend fun followUser(@Path("userId") userId: Int, @Body request: FollowRequest): Map<String, Any>

    @DELETE("users/{userId}/follow")
    suspend fun unfollowUser(@Path("userId") userId: Int, @Body request: FollowRequest): Map<String, Any>

    @PUT("users/{userId}/follow/notify")
    suspend fun toggleNotify(@Path("userId") userId: Int, @Body request: NotifyRequest): Map<String, Any>

    @GET("users/{userId}/notifications")
    suspend fun getNotifications(@Path("userId") userId: Int): List<AppNotification>

    @PUT("notifications/read-all")
    suspend fun markAllRead(@Body request: ReadAllRequest): Map<String, Any>

    @GET("groups/{groupId}/events")
    suspend fun getGroupEvents(
        @Path("groupId") groupId: Int,
        @Query("userId") userId: Int? = null
    ): List<GroupEvent>

    @POST("groups/{groupId}/events")
    suspend fun createEvent(
        @Path("groupId") groupId: Int,
        @Body request: CreateEventRequest
    ): GroupEvent

    @POST("events/{eventId}/interest")
    suspend fun markInterested(
        @Path("eventId") eventId: Int,
        @Body request: InterestRequest
    ): Map<String, Any>

    @DELETE("events/{eventId}/interest")
    suspend fun unmarkInterested(
        @Path("eventId") eventId: Int,
        @Body request: InterestRequest
    ): Map<String, Any>

    @POST("ai/suggest-tags")
    suspend fun suggestTags(@Body body: Map<String, String>): SuggestTagsResponse

    @POST("photos/{photoId}/report")
    suspend fun reportPost(
        @Path("photoId") photoId: String,
        @Body request: ReportRequest
    ): Map<String, Any>

    @GET("admin/reports")
    suspend fun getReportedPosts(
        @Query("userId") userId: Int
    ): List<ReportedPost>

    @DELETE("admin/photos/{photoId}")
    suspend fun deletePost(
        @Path("photoId") photoId: String,
        @Query("userId") userId: Int
    ): Map<String, Any>
}
