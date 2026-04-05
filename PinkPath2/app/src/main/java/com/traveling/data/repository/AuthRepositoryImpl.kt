package com.traveling.data.repository

import com.traveling.data.local.SessionManager
import com.traveling.data.remote.AuthApi
import com.traveling.domain.model.AuthRequest
import com.traveling.domain.model.AuthResponse
import com.traveling.domain.model.RegisterResponse
import com.traveling.domain.model.UserData
import com.traveling.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api: AuthApi,
    private val sessionManager: SessionManager
) : AuthRepository {

    private val _currentUser = MutableStateFlow<UserData?>(null)
    override val currentUser: StateFlow<UserData?> = _currentUser.asStateFlow()

    init {
        val userId = sessionManager.getUserId()
        val username = sessionManager.getUsername()
        val profileUrl = sessionManager.getProfileUrl()
        if (userId != null && username != null) {
            _currentUser.value = UserData(userId, username, profileUrl)
        }
    }

    override suspend fun register(request: AuthRequest): Result<RegisterResponse> {
        return try {
            val response = api.register(request)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun login(request: AuthRequest): Result<AuthResponse> {
        return try {
            val response = api.login(request)
            sessionManager.saveAuthToken(response.token)
            sessionManager.saveUserId(response.user.id)
            sessionManager.saveUsername(response.user.username)
            sessionManager.saveProfileUrl(response.user.profileUrl)
            _currentUser.value = response.user
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signup(request: AuthRequest): Result<AuthResponse> {
        return try {
            api.register(request)
            login(request)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateProfilePicture(userId: String, imageFile: File): Result<String> {
        return try {
            val requestFile = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("avatar", imageFile.name, requestFile)
            val userIdBody = userId.toRequestBody("text/plain".toMediaTypeOrNull())
            
            val response = api.updateProfilePicture(body, userIdBody)
            sessionManager.saveProfileUrl(response.profileUrl)
            
            _currentUser.value = _currentUser.value?.copy(profileUrl = response.profileUrl)
            
            Result.success(response.profileUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun logout() {
        sessionManager.logout()
        _currentUser.value = null
    }

    override fun isLoggedIn(): Boolean {
        return sessionManager.isLoggedIn()
    }
}
