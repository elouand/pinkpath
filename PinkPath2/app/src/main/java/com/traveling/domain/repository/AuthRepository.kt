package com.traveling.domain.repository

import com.traveling.domain.model.AuthRequest
import com.traveling.domain.model.AuthResponse
import com.traveling.domain.model.RegisterResponse
import com.traveling.domain.model.UserData
import kotlinx.coroutines.flow.StateFlow
import java.io.File

interface AuthRepository {
    val currentUser: StateFlow<UserData?>
    suspend fun register(request: AuthRequest): Result<RegisterResponse>
    suspend fun login(request: AuthRequest): Result<AuthResponse>
    suspend fun signup(request: AuthRequest): Result<AuthResponse>
    suspend fun updateProfilePicture(userId: String, imageFile: File): Result<String>
    fun logout()
    fun isLoggedIn(): Boolean
}
