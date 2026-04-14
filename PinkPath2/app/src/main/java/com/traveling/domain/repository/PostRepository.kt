package com.traveling.domain.repository

import com.traveling.data.remote.CommentResponse
import com.traveling.data.remote.LikeResponse
import com.traveling.domain.model.Post
import com.traveling.domain.model.Group
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
        authorId: String?,
        groupId: Int? = null,
        tags: String? = null
    ): Result<Post>

    suspend fun likePost(photoId: String, userId: Int): Result<LikeResponse>
    suspend fun addComment(photoId: String, text: String, userId: Int): Result<CommentResponse>
    suspend fun getComments(photoId: String): Result<List<CommentResponse>>

    // Group methods
    suspend fun getUserGroups(userId: Int): Result<List<Group>>
    suspend fun createGroup(
        name: String, 
        userId: Int, 
        description: String? = null, 
        isPrivate: Boolean = false,
        image: File? = null
    ): Result<Group>
    suspend fun addUserToGroup(groupId: Int, usernameToAdd: String): Result<Unit>
}
