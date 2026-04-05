package com.traveling.data.repository

import com.traveling.data.remote.CommentRequest
import com.traveling.data.remote.CommentResponse
import com.traveling.data.remote.LikeRequest
import com.traveling.data.remote.LikeResponse
import com.traveling.data.remote.PostApi
import com.traveling.domain.model.Post
import com.traveling.domain.repository.PostRepository
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
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
        authorId: String?
    ): Result<Post> {
        return try {
            val imagePart = MultipartBody.Part.createFormData(
                "image",
                image.name,
                image.asRequestBody("image/*".toMediaTypeOrNull())
            )

            val audioPart = audio?.let {
                MultipartBody.Part.createFormData(
                    "audio",
                    it.name,
                    it.asRequestBody("audio/*".toMediaTypeOrNull())
                )
            }

            val descriptionBody = description.toRequestBody("text/plain".toMediaTypeOrNull())
            val typeLieuBody = typeLieu.toRequestBody("text/plain".toMediaTypeOrNull())
            val latitudeBody = latitude.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val longitudeBody = longitude.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val isPublicBody = isPublic.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val authorIdBody = authorId?.toRequestBody("text/plain".toMediaTypeOrNull())

            val post = api.uploadPhoto(
                imagePart,
                audioPart,
                descriptionBody,
                typeLieuBody,
                latitudeBody,
                longitudeBody,
                isPublicBody,
                authorIdBody
            )
            Result.success(post)
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

    override suspend fun addComment(photoId: String, text: String, authorId: Int): Result<CommentResponse> {
        return try {
            val response = api.addComment(photoId, CommentRequest(text, authorId))
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
}
