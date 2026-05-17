package com.traveling.data.repository

import com.traveling.data.remote.*
import com.traveling.data.remote.FollowRequest
import com.traveling.data.remote.NotifyRequest
import com.traveling.data.remote.ReadAllRequest
import com.traveling.domain.model.AppNotification
import com.traveling.domain.model.CreateEventRequest
import com.traveling.domain.model.GroupEvent
import com.traveling.domain.model.InterestRequest
import com.traveling.domain.model.Post
import com.traveling.domain.model.Group
import com.traveling.domain.model.JoinRequest
import com.traveling.domain.model.PublicUserProfile
import com.traveling.domain.model.ShareItineraryRequest
import com.traveling.domain.model.UserSearchResult
import com.traveling.domain.repository.PostRepository
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostRepositoryImpl @Inject constructor(
    private val api: PostApi
) : PostRepository {
    override suspend fun getPosts(userId: Int?): Result<List<Post>> {
        return try {
            val posts = api.getPosts(userId)
            Result.success(posts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadPost(
        image: File,
        audio: File?,
        description: String,
        typeLieu: String,
        latitude: Double,
        longitude: Double,
        isPublic: Boolean,
        authorId: String?,
        groupId: Int?,
        tags: String?
    ): Result<Post> {
        return try {
            val builder = MultipartBody.Builder().setType(MultipartBody.FORM)
            
            builder.addFormDataPart(
                "image", image.name,
                image.asRequestBody("image/*".toMediaTypeOrNull())
            )

            audio?.let {
                builder.addFormDataPart(
                    "audio", it.name,
                    it.asRequestBody("audio/*".toMediaTypeOrNull())
                )
            }

            builder.addFormDataPart("description", description)
            builder.addFormDataPart("type_lieu", typeLieu)
            builder.addFormDataPart("latitude", latitude.toString())
            builder.addFormDataPart("longitude", longitude.toString())
            builder.addFormDataPart("is_public", isPublic.toString())
            
            authorId?.let { builder.addFormDataPart("authorId", it) }
            groupId?.let { builder.addFormDataPart("groupId", it.toString()) }
            tags?.let { builder.addFormDataPart("tags", it) }

            api.uploadPhoto(builder.build())
            Result.success(Post(id = "0")) 
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun likePost(photoId: String, userId: Int): Result<LikeResponse> {
        return try {
            val response = api.likePost(photoId, LikeRequest(userId))
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addComment(photoId: String, text: String, userId: Int): Result<CommentResponse> {
        return try {
            val response = api.addComment(photoId, CommentRequest(text, userId))
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getComments(photoId: String): Result<List<CommentResponse>> {
        return try {
            val comments = api.getComments(photoId)
            Result.success(comments)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUserGroups(userId: Int): Result<List<Group>> {
        return try {
            Result.success(api.getUserGroups(userId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createGroup(
        name: String, 
        userId: Int, 
        description: String?, 
        isPrivate: Boolean,
        image: File?
    ): Result<Group> {
        return try {
            val builder = MultipartBody.Builder().setType(MultipartBody.FORM)
            
            builder.addFormDataPart("name", name)
            builder.addFormDataPart("userId", userId.toString())
            description?.let { builder.addFormDataPart("description", it) }
            // Le serveur attend 'isPublic' (true si pas privé)
            builder.addFormDataPart("isPublic", (!isPrivate).toString())

            image?.let {
                builder.addFormDataPart(
                    "groupImage", it.name,
                    it.asRequestBody("image/*".toMediaTypeOrNull())
                )
            }

            val group = api.createGroup(builder.build())
            Result.success(group)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addUserToGroup(groupId: Int, usernameToAdd: String): Result<Unit> {
        return try {
            api.addUserToGroup(groupId, AddUserToGroupRequest(usernameToAdd))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun shareItinerary(itineraryId: Int, userId: Int, description: String, isPublic: Boolean, groupId: Int?): Result<Unit> {
        return try {
            api.shareItinerary(itineraryId, ShareItineraryRequest(userId, description, isPublic, groupId))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getGroupDetails(groupId: Int): Result<Group> {
        return try {
            Result.success(api.getGroupDetails(groupId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getJoinRequests(groupId: Int): Result<List<JoinRequest>> {
        return try {
            Result.success(api.getJoinRequests(groupId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun respondToJoinRequest(groupId: Int, requestId: Int, accept: Boolean): Result<Unit> {
        return try {
            api.respondToJoinRequest(groupId, requestId, mapOf("action" to if (accept) "accept" else "reject"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchGroups(query: String, userId: Int?): Result<List<Group>> {
        return try {
            Result.success(api.searchGroups(query, userId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun joinGroup(groupId: Int, userId: Int): Result<Unit> {
        return try {
            api.joinGroup(groupId, JoinGroupRequest(userId))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchUsers(query: String): Result<List<UserSearchResult>> {
        return try {
            Result.success(api.searchUsers(query))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUserProfile(userId: Int, followerId: Int?): Result<PublicUserProfile> {
        return try {
            Result.success(api.getUserProfile(userId, followerId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun followUser(userId: Int, followerId: Int): Result<Unit> {
        return try {
            api.followUser(userId, FollowRequest(followerId))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun unfollowUser(userId: Int, followerId: Int): Result<Unit> {
        return try {
            api.unfollowUser(userId, FollowRequest(followerId))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun toggleNotify(userId: Int, followerId: Int, notify: Boolean): Result<Unit> {
        return try {
            api.toggleNotify(userId, NotifyRequest(followerId, notify))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getNotifications(userId: Int): Result<List<AppNotification>> {
        return try {
            Result.success(api.getNotifications(userId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markAllNotificationsRead(userId: Int): Result<Unit> {
        return try {
            api.markAllRead(ReadAllRequest(userId))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getGroupEvents(groupId: Int, userId: Int?): Result<List<GroupEvent>> {
        return try {
            Result.success(api.getGroupEvents(groupId, userId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createEvent(groupId: Int, request: CreateEventRequest): Result<GroupEvent> {
        return try {
            Result.success(api.createEvent(groupId, request))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun toggleInterest(eventId: Int, userId: Int, interested: Boolean): Result<Int> {
        return try {
            val response = if (interested)
                api.markInterested(eventId, InterestRequest(userId))
            else
                api.unmarkInterested(eventId, InterestRequest(userId))
            val count = (response["interestedCount"] as? Double)?.toInt() ?: 0
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
