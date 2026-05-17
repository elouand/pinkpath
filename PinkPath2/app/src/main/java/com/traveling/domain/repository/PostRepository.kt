package com.traveling.domain.repository

import com.traveling.data.remote.CommentResponse
import com.traveling.data.remote.LikeResponse
import com.traveling.domain.model.Post
import com.traveling.domain.model.Group
import com.traveling.domain.model.JoinRequest
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
    suspend fun shareItinerary(itineraryId: Int, userId: Int, description: String, isPublic: Boolean = true, groupId: Int? = null): Result<Unit>
    suspend fun getGroupDetails(groupId: Int): Result<Group>
    suspend fun getJoinRequests(groupId: Int): Result<List<JoinRequest>>
    suspend fun respondToJoinRequest(groupId: Int, requestId: Int, accept: Boolean): Result<Unit>
    
    suspend fun searchGroups(query: String, userId: Int? = null): Result<List<Group>>
    suspend fun joinGroup(groupId: Int, userId: Int): Result<Unit>
}
