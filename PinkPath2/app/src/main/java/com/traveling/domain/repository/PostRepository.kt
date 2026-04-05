package com.traveling.domain.repository

import com.traveling.data.remote.CommentResponse
import com.traveling.data.remote.LikeResponse
import com.traveling.domain.model.Post
import java.io.File

interface PostRepository {
    suspend fun getPosts(userId: Int? = null): Result<List<Post>>
    
    suspend fun uploadPost(
        image: File,
        audio: File?,
        description: String,
        typeLieu: String,
        latitude: Double,
        longitude: Double,
        isPublic: Boolean,
        authorId: String?
    ): Result<Post>

    suspend fun likePost(photoId: String, userId: Int): Result<LikeResponse>
    suspend fun addComment(photoId: String, text: String, authorId: Int): Result<CommentResponse>
    suspend fun getComments(photoId: String): Result<List<CommentResponse>>
}
