package com.traveling.ui.travelshare

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.traveling.data.local.SessionManager
import com.traveling.data.remote.CommentResponse
import com.traveling.data.remote.PhotonApi
import com.traveling.data.remote.PhotonFeature
import com.traveling.domain.model.Post
import com.traveling.domain.repository.PostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.File
import javax.inject.Inject

@HiltViewModel
class PostViewModel @Inject constructor(
    private val repository: PostRepository,
    private val photonApi: PhotonApi,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _uploadSuccess = MutableStateFlow(false)
    val uploadSuccess: StateFlow<Boolean> = _uploadSuccess

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _comments = MutableStateFlow<Map<String, List<CommentResponse>>>(emptyMap())
    val comments: StateFlow<Map<String, List<CommentResponse>>> = _comments

    init {
        loadPosts()
    }

    fun loadPosts() {
        viewModelScope.launch {
            _isLoading.value = true
            val userId = sessionManager.getUserId()?.toIntOrNull()
            repository.getPosts(userId).onSuccess {
                _posts.value = it
                _error.value = null
            }.onFailure {
                handleError(it, "chargement")
            }
            _isLoading.value = false
        }
    }

    suspend fun searchLocation(query: String): Result<List<PhotonFeature>> {
        return try {
            val response = photonApi.search(query)
            Result.success(response.features)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun uploadPost(
        image: File,
        audio: File?,
        description: String,
        typeLieu: String,
        latitude: Double,
        longitude: Double,
        isPublic: Boolean,
        authorId: String? = null
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            repository.uploadPost(
                image, audio, description, typeLieu, latitude, longitude, isPublic, authorId
            ).onSuccess {
                _uploadSuccess.value = true
                loadPosts()
            }.onFailure {
                handleError(it, "envoi")
            }
            _isLoading.value = false
        }
    }

    fun toggleLike(postId: String, userId: Int) {
        viewModelScope.launch {
            // Optimistic UI update
            val currentPosts = _posts.value
            _posts.value = currentPosts.map {
                if (it.id == postId) {
                    val newIsLiked = !it.isLiked
                    val currentLikes = it.displayLikes
                    val newLikes = if (newIsLiked) currentLikes + 1 else currentLikes - 1
                    it.copy(isLiked = newIsLiked, likes = newLikes)
                } else it
            }

            repository.likePost(postId, userId).onSuccess { response ->
                // Sync with server response
                _posts.value = _posts.value.map {
                    if (it.id == postId) {
                        it.copy(
                            likes = response.likes,
                            isLiked = response.isLiked ?: it.isLiked
                        )
                    } else it
                }
            }.onFailure {
                // Rollback on failure
                _posts.value = currentPosts
                handleError(it, "like")
            }
        }
    }

    fun loadComments(postId: String) {
        viewModelScope.launch {
            repository.getComments(postId).onSuccess {
                _comments.value = _comments.value + (postId to it)
            }.onFailure {
                handleError(it, "chargement commentaires")
            }
        }
    }

    fun addComment(postId: String, text: String, authorId: Int) {
        viewModelScope.launch {
            repository.addComment(postId, text, authorId).onSuccess {
                loadComments(postId) // Refresh comments
                // Optionally refresh post to get new comment count
                loadPosts()
            }.onFailure {
                handleError(it, "ajout commentaire")
            }
        }
    }

    private fun handleError(t: Throwable, action: String) {
        t.printStackTrace()
        val message = when (t) {
            is HttpException -> "Erreur $action (HTTP ${t.code()})"
            else -> "Erreur lors de l'action $action: ${t.localizedMessage}"
        }
        _error.value = message
    }
    
    fun resetUploadStatus() {
        _uploadSuccess.value = false
    }

    fun clearError() {
        _error.value = null
    }
}
