package com.traveling.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.traveling.domain.model.User
import com.traveling.domain.repository.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AuthRepository {

    private val prefs: SharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    
    // In-memory list of "registered" users (simulating a DB)
    private val registeredUsers = mutableListOf<User>()

    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser

    init {
        // Load session if exists
        val savedUsername = prefs.getString("logged_in_user", null)
        if (savedUsername != null) {
            _currentUser.value = User(savedUsername, "")
        }
    }

    override suspend fun login(username: String, password: String): Boolean {
        // Check if user exists in our "local DB"
        val user = registeredUsers.find { it.username == username && it.password == password }
        return if (user != null) {
            _currentUser.value = user
            prefs.edit().putString("logged_in_user", username).apply()
            true
        } else {
            false
        }
    }

    override suspend fun signup(username: String, password: String): Boolean {
        // Check if username already taken
        if (registeredUsers.any { it.username == username }) return false
        
        val newUser = User(username, password)
        registeredUsers.add(newUser)
        
        // Auto-login after signup
        _currentUser.value = newUser
        prefs.edit().putString("logged_in_user", username).apply()
        return true
    }

    override fun logout() {
        _currentUser.value = null
        prefs.edit().remove("logged_in_user").apply()
    }
}
