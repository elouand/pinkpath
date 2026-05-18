package com.traveling.ui.travelshare

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.traveling.data.local.SessionManager
import com.traveling.data.remote.CommentResponse
import com.traveling.data.remote.PhotonApi
import com.traveling.data.remote.PhotonFeature
import com.traveling.domain.model.AppNotification
import com.traveling.domain.model.CreateEventRequest
import com.traveling.domain.model.GroupEvent
import com.traveling.domain.model.Post
import com.traveling.domain.model.Group
import com.traveling.domain.model.JoinRequest
import com.traveling.domain.model.PublicUserProfile
import com.traveling.domain.model.UserSearchResult
import com.traveling.domain.repository.PostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.File
import javax.inject.Inject

@HiltViewModel
class PostViewModel @Inject constructor(
    private val repository: PostRepository,
    private val photonApi: PhotonApi,
    private val sessionManager: SessionManager,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts

    private val _followingPosts = MutableStateFlow<List<Post>>(emptyList())
    val followingPosts: StateFlow<List<Post>> = _followingPosts

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _uploadSuccess = MutableStateFlow(false)
    val uploadSuccess: StateFlow<Boolean> = _uploadSuccess

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _comments = MutableStateFlow<Map<String, List<CommentResponse>>>(emptyMap())
    val comments: StateFlow<Map<String, List<CommentResponse>>> = _comments

    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups: StateFlow<List<Group>> = _groups
    
    private val _selectedGroup = MutableStateFlow<Group?>(null)
    val selectedGroup: StateFlow<Group?> = _selectedGroup

    private val _joinRequests = MutableStateFlow<List<JoinRequest>>(emptyList())
    val joinRequests: StateFlow<List<JoinRequest>> = _joinRequests

    private val _searchResults = MutableStateFlow<List<Group>>(emptyList())
    val searchResults: StateFlow<List<Group>> = _searchResults

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage = _toastMessage.asStateFlow()

    private val _userSearchResults = MutableStateFlow<List<UserSearchResult>>(emptyList())
    val userSearchResults: StateFlow<List<UserSearchResult>> = _userSearchResults

    private val _publicProfile = MutableStateFlow<PublicUserProfile?>(null)
    val publicProfile: StateFlow<PublicUserProfile?> = _publicProfile

    private val _isLoadingProfile = MutableStateFlow(false)
    val isLoadingProfile: StateFlow<Boolean> = _isLoadingProfile

    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications: StateFlow<List<AppNotification>> = _notifications

    private val _unreadCount = MutableStateFlow(0)
    val unreadNotificationCount: StateFlow<Int> = _unreadCount

    private var searchJob: Job? = null
    private var userSearchJob: Job? = null

    init {
        loadPosts()
        loadUserGroups()
    }

    fun loadPosts() {
        viewModelScope.launch {
            _isLoading.value = true
            val userId = sessionManager.getUserId()?.toIntOrNull()
            repository.getPosts(userId).onSuccess {
                _posts.value = it
                _error.value = null
            }.onFailure { handleError(it, "chargement") }
            _isLoading.value = false
        }
    }

    fun loadFollowingPosts() {
        val userId = sessionManager.getUserId()?.toIntOrNull() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            repository.getFollowingPosts(userId).onSuccess {
                _followingPosts.value = it
                _error.value = null
            }.onFailure { handleError(it, "abonnements") }
            _isLoading.value = false
        }
    }

    fun loadUserGroups() {
        val userId = sessionManager.getUserId()?.toIntOrNull() ?: return
        viewModelScope.launch {
            repository.getUserGroups(userId).onSuccess {
                _groups.value = it
            }
        }
    }

    fun loadGroupDetails(groupId: Int) {
        val currentUserId = sessionManager.getUserId()
        viewModelScope.launch {
            _isLoading.value = true
            repository.getGroupDetails(groupId).onSuccess { group ->
                _selectedGroup.value = group
                val isAdmin = group.users?.any { it.id == currentUserId && it.role == "ADMIN" } == true
                if (isAdmin && !group.isPublic) {
                    loadJoinRequests(groupId)
                }
            }.onFailure { handleError(it, "détails groupe") }
            _isLoading.value = false
        }
    }

    fun loadJoinRequests(groupId: Int) {
        viewModelScope.launch {
            repository.getJoinRequests(groupId).onSuccess {
                _joinRequests.value = it
            }
        }
    }

    fun respondToRequest(groupId: Int, requestId: Int, accept: Boolean) {
        viewModelScope.launch {
            repository.respondToJoinRequest(groupId, requestId, accept).onSuccess {
                _toastMessage.value = if (accept) "Membre accepté !" else "Demande refusée"
                loadJoinRequests(groupId)
                loadGroupDetails(groupId)
            }
        }
    }

    fun searchGroups(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        val userId = sessionManager.getUserId()?.toIntOrNull()
        searchJob = viewModelScope.launch {
            _isLoading.value = true
            delay(300)
            repository.searchGroups(query, userId).onSuccess {
                _searchResults.value = it
            }
            _isLoading.value = false
        }
    }

    fun joinGroup(groupId: Int) {
        val userId = sessionManager.getUserId()?.toIntOrNull() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            repository.joinGroup(groupId, userId).onSuccess {
                _toastMessage.value = "Demande envoyée avec succès !"
                _searchResults.value = _searchResults.value.map { 
                    if (it.id == groupId) it.copy(isPending = true) else it 
                }
            }.onFailure { handleError(it, "rejoindre") }
            _isLoading.value = false
        }
    }

    fun createGroup(
        name: String,
        description: String? = null,
        isPrivate: Boolean = false,
        image: File? = null
    ) {
        val userId = sessionManager.getUserId()?.toIntOrNull() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            repository.createGroup(name, userId, description, isPrivate, image).onSuccess {
                loadUserGroups()
                _uploadSuccess.value = true
            }.onFailure {
                handleError(it, "création groupe")
            }
            _isLoading.value = false
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
        authorId: String? = null,
        groupId: Int? = null,
        tags: String? = null
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            repository.uploadPost(
                image, audio, description, typeLieu, latitude, longitude, isPublic, authorId, groupId, tags
            ).onSuccess {
                _uploadSuccess.value = true
                loadPosts()
            }.onFailure {
                handleError(it, "envoi")
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

    fun addUserToGroup(groupId: Int, username: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.addUserToGroup(groupId, username).onSuccess {
                loadGroupDetails(groupId)
            }.onFailure {
                handleError(it, "ajout membre")
            }
            _isLoading.value = false
        }
    }

    fun toggleLike(postId: String, userId: Int) {
        viewModelScope.launch {
            val currentPosts = _posts.value
            val currentFollowingPosts = _followingPosts.value

            fun optimisticUpdate(list: List<Post>) = list.map {
                if (it.id == postId) {
                    val newIsLiked = !it.isLiked
                    it.copy(isLiked = newIsLiked, likes = if (newIsLiked) it.displayLikes + 1 else it.displayLikes - 1)
                } else it
            }
            _posts.value = optimisticUpdate(currentPosts)
            _followingPosts.value = optimisticUpdate(currentFollowingPosts)

            repository.likePost(postId, userId).onSuccess { response ->
                fun confirmUpdate(list: List<Post>) = list.map {
                    if (it.id == postId) it.copy(likes = response.likes, isLiked = response.isLiked ?: it.isLiked) else it
                }
                _posts.value = confirmUpdate(_posts.value)
                _followingPosts.value = confirmUpdate(_followingPosts.value)
            }.onFailure {
                _posts.value = currentPosts
                _followingPosts.value = currentFollowingPosts
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

    fun addComment(postId: String, text: String, userId: Int) {
        viewModelScope.launch {
            repository.addComment(postId, text, userId).onSuccess {
                loadComments(postId)
                loadPosts()
            }.onFailure {
                handleError(it, "ajout commentaire")
            }
        }
    }

    fun clearToast() { _toastMessage.value = null }

    private fun handleError(t: Throwable, action: String) {
        _error.value = "Erreur $action: ${t.localizedMessage}"
    }

    fun resetUploadStatus() { _uploadSuccess.value = false }

    fun clearError() { _error.value = null }

    private val _shareSuccess = MutableStateFlow(false)
    val shareSuccess = _shareSuccess.asStateFlow()

    fun shareItinerary(itineraryId: Int, userId: Int, description: String, isPublic: Boolean = true, groupId: Int? = null) {
        viewModelScope.launch {
            repository.shareItinerary(itineraryId, userId, description, isPublic, groupId)
                .onSuccess { _shareSuccess.value = true }
                .onFailure { handleError(it, "partage itinéraire") }
        }
    }

    fun clearShareSuccess() { _shareSuccess.value = false }

    fun searchUsers(query: String) {
        userSearchJob?.cancel()
        if (query.isBlank()) {
            _userSearchResults.value = emptyList()
            return
        }
        userSearchJob = viewModelScope.launch {
            delay(200)
            repository.searchUsers(query).onSuccess {
                _userSearchResults.value = it
            }.onFailure {
                _userSearchResults.value = emptyList()
            }
        }
    }

    fun clearUserSearch() { _userSearchResults.value = emptyList() }

    fun loadPublicProfile(userId: Int) {
        val myId = sessionManager.getUserId()?.toIntOrNull()
        viewModelScope.launch {
            _isLoadingProfile.value = true
            _publicProfile.value = null
            repository.getUserProfile(userId, myId).onSuccess {
                _publicProfile.value = it
            }.onFailure { handleError(it, "profil") }
            _isLoadingProfile.value = false
        }
    }

    fun followUser(targetUserId: Int) {
        val myId = sessionManager.getUserId()?.toIntOrNull() ?: return
        viewModelScope.launch {
            repository.followUser(targetUserId, myId).onSuccess {
                _publicProfile.value = _publicProfile.value?.copy(
                    isFollowing = true,
                    followersCount = (_publicProfile.value?.followersCount ?: 0) + 1
                )
            }.onFailure { handleError(it, "follow") }
        }
    }

    fun unfollowUser(targetUserId: Int) {
        val myId = sessionManager.getUserId()?.toIntOrNull() ?: return
        viewModelScope.launch {
            repository.unfollowUser(targetUserId, myId).onSuccess {
                _publicProfile.value = _publicProfile.value?.copy(
                    isFollowing = false,
                    notifyEnabled = false,
                    followersCount = maxOf(0, (_publicProfile.value?.followersCount ?: 1) - 1)
                )
            }.onFailure { handleError(it, "unfollow") }
        }
    }

    fun toggleNotify(targetUserId: Int, notify: Boolean) {
        val myId = sessionManager.getUserId()?.toIntOrNull() ?: return
        viewModelScope.launch {
            repository.toggleNotify(targetUserId, myId, notify).onSuccess {
                _publicProfile.value = _publicProfile.value?.copy(notifyEnabled = notify)
            }.onFailure { handleError(it, "notification") }
        }
    }

    fun loadNotifications() {
        val userId = sessionManager.getUserId()?.toIntOrNull() ?: return
        viewModelScope.launch {
            repository.getNotifications(userId).onSuccess {
                _notifications.value = it
                _unreadCount.value = it.count { n -> !n.read }
            }
        }
    }

    // ── Events ──────────────────────────────────────────────────────────────

    private val _groupEvents = MutableStateFlow<List<GroupEvent>>(emptyList())
    val groupEvents: StateFlow<List<GroupEvent>> = _groupEvents

    private val _eventCreateSuccess = MutableStateFlow(false)
    val eventCreateSuccess: StateFlow<Boolean> = _eventCreateSuccess

    fun loadGroupEvents(groupId: Int) {
        val userId = sessionManager.getUserId()?.toIntOrNull()
        viewModelScope.launch {
            repository.getGroupEvents(groupId, userId).onSuccess {
                _groupEvents.value = it
            }
        }
    }

    fun createEvent(groupId: Int, title: String?, description: String?, startAt: String, itineraryId: Int?) {
        val userId = sessionManager.getUserId()?.toIntOrNull() ?: return
        viewModelScope.launch {
            repository.createEvent(groupId, CreateEventRequest(title, description, startAt, itineraryId, userId))
                .onSuccess {
                    _groupEvents.value = _groupEvents.value + it
                    _eventCreateSuccess.value = true
                }
                .onFailure { handleError(it, "création événement") }
        }
    }

    fun clearEventCreateSuccess() { _eventCreateSuccess.value = false }

    fun toggleInterest(eventId: Int, currentlyInterested: Boolean) {
        val userId = sessionManager.getUserId()?.toIntOrNull() ?: return
        viewModelScope.launch {
            repository.toggleInterest(eventId, userId, !currentlyInterested).onSuccess { newCount ->
                _groupEvents.value = _groupEvents.value.map { ev ->
                    if (ev.id == eventId) ev.copy(isInterested = !currentlyInterested, interestedCount = newCount) else ev
                }
            }
        }
    }

    fun markAllNotificationsRead() {
        val userId = sessionManager.getUserId()?.toIntOrNull() ?: return
        viewModelScope.launch {
            repository.markAllNotificationsRead(userId).onSuccess {
                _notifications.value = _notifications.value.map { it.copy(read = true) }
                _unreadCount.value = 0
            }
        }
    }

    private val _suggestedTags = MutableStateFlow<List<String>>(emptyList())
    val suggestedTags: StateFlow<List<String>> = _suggestedTags

    private val _isSuggestingTags = MutableStateFlow(false)
    val isSuggestingTags: StateFlow<Boolean> = _isSuggestingTags

    fun suggestTagsFromPlace(placeName: String, imageUri: Uri? = null) {
        viewModelScope.launch {
            _isSuggestingTags.value = true
            _suggestedTags.value = emptyList()
            try {
                val imageBase64 = imageUri?.let { uri ->
                    try {
                        val bitmap = appContext.contentResolver.openInputStream(uri)?.use { stream ->
                            BitmapFactory.decodeStream(stream)
                        } ?: return@let null
                        val maxSize = 400
                        val scale = minOf(maxSize.toFloat() / bitmap.width, maxSize.toFloat() / bitmap.height, 1f)
                        val scaled = Bitmap.createScaledBitmap(
                            bitmap,
                            (bitmap.width * scale).toInt(),
                            (bitmap.height * scale).toInt(),
                            true
                        )
                        val out = ByteArrayOutputStream()
                        scaled.compress(Bitmap.CompressFormat.JPEG, 70, out)
                        android.util.Base64.encodeToString(out.toByteArray(), android.util.Base64.NO_WRAP)
                    } catch (e: Exception) { null }
                }
                repository.suggestTags(placeName, imageBase64)
                    .onSuccess { tags -> _suggestedTags.value = tags }
                    .onFailure { err -> _toastMessage.value = "Suggestion impossible : ${err.message}" }
            } catch (e: Exception) {
                _toastMessage.value = "Erreur : ${e.message}"
            } finally {
                _isSuggestingTags.value = false
            }
        }
    }

    fun clearSuggestedTags() { _suggestedTags.value = emptyList() }

    private val _reportedPosts = MutableStateFlow<List<com.traveling.domain.model.ReportedPost>>(emptyList())
    val reportedPosts: StateFlow<List<com.traveling.domain.model.ReportedPost>> = _reportedPosts

    fun reportPost(postId: String) {
        val userId = sessionManager.getUserId()?.toIntOrNull() ?: return
        viewModelScope.launch {
            repository.reportPost(postId, userId).onSuccess {
                _toastMessage.value = "Post signalé"
            }.onFailure {
                _toastMessage.value = "Erreur lors du signalement"
            }
        }
    }

    fun loadReportedPosts() {
        val userId = sessionManager.getUserId()?.toIntOrNull() ?: return
        viewModelScope.launch {
            repository.getReportedPosts(userId).onSuccess { _reportedPosts.value = it }
        }
    }

    fun deleteReportedPost(postId: String) {
        val userId = sessionManager.getUserId()?.toIntOrNull() ?: return
        viewModelScope.launch {
            repository.deletePost(postId, userId).onSuccess {
                _reportedPosts.value = _reportedPosts.value.filter { it.id.toString() != postId }
                _toastMessage.value = "Post supprimé"
            }.onFailure {
                _toastMessage.value = "Erreur lors de la suppression"
            }
        }
    }
}
