package com.traveling.data.repository

import com.traveling.data.remote.*
import com.traveling.domain.model.Post
import com.traveling.domain.model.Group
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
}
