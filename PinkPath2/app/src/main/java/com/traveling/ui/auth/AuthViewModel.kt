package com.traveling.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.traveling.data.local.SessionManager
import com.traveling.domain.model.AuthRequest
import com.traveling.domain.model.UserData
import com.traveling.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    val currentUser: StateFlow<UserData?> = repository.currentUser

    private val _isLoggedIn = MutableStateFlow(sessionManager.isLoggedIn())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            repository.login(AuthRequest(username, password)).onSuccess {
                _isLoggedIn.value = true
            }.onFailure {
                _error.value = it.localizedMessage ?: "Erreur de connexion"
            }
            _isLoading.value = false
        }
    }

    fun register(username: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            repository.register(AuthRequest(username, password)).onSuccess {
                login(username, password)
            }.onFailure {
                _error.value = it.localizedMessage ?: "Erreur d'inscription"
            }
            _isLoading.value = false
        }
    }

    fun updateProfilePicture(imageFile: File) {
        val userId = currentUser.value?.id ?: return
        viewModelScope.launch {
            _isLoading.value = true
            repository.updateProfilePicture(userId, imageFile).onFailure {
                _error.value = it.localizedMessage ?: "Erreur lors de la mise à jour de la photo"
            }
            _isLoading.value = false
        }
    }

    fun logout() {
        sessionManager.logout()
        _isLoggedIn.value = false
    }

    fun clearError() {
        _error.value = null
    }
}
