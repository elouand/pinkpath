package com.traveling.domain.repository

import com.traveling.domain.model.User
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val currentUser: StateFlow<User?>
    suspend fun login(username: String, password: String): Boolean
    suspend fun signup(username: String, password: String): Boolean
    fun logout()
}
