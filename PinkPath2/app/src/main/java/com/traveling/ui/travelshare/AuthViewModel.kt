package com.traveling.ui.travelshare

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.traveling.domain.model.AuthRequest
import com.traveling.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    val currentUser = repository.currentUser

    private val _isLoggedIn = MutableStateFlow(repository.isLoggedIn())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _authEvent = MutableSharedFlow<AuthEvent>()
    val authEvent = _authEvent.asSharedFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun login(username: String, password: String) {
        viewModelScope.launch {
            if (username.isBlank() || password.isBlank()) {
                _authEvent.emit(AuthEvent.Error("Veuillez remplir tous les champs"))
                return@launch
            }
            _isLoading.value = true
            repository.login(AuthRequest(username, password))
                .onSuccess {
                    _isLoggedIn.value = true
                    _authEvent.emit(AuthEvent.Success)
                }
                .onFailure {
                    _authEvent.emit(AuthEvent.Error(it.localizedMessage ?: "Identifiants incorrects"))
                }
            _isLoading.value = false
        }
    }

    fun signup(username: String, password: String, confirm: String) {
        viewModelScope.launch {
            if (username.isBlank() || password.isBlank()) {
                _authEvent.emit(AuthEvent.Error("Veuillez remplir tous les champs"))
                return@launch
            }
            if (password != confirm) {
                _authEvent.emit(AuthEvent.Error("Les mots de passe ne correspondent pas"))
                return@launch
            }
            _isLoading.value = true
            repository.signup(AuthRequest(username, password))
                .onSuccess {
                    _isLoggedIn.value = true
                    _authEvent.emit(AuthEvent.Success)
                }
                .onFailure {
                    _authEvent.emit(AuthEvent.Error(it.localizedMessage ?: "Erreur d'inscription"))
                }
            _isLoading.value = false
        }
    }

    fun updateProfilePicture(imageFile: File) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            repository.updateProfilePicture(user.id, imageFile)
                .onFailure {
                    _authEvent.emit(AuthEvent.Error(it.localizedMessage ?: "Erreur lors de la mise à jour de la photo"))
                }
            _isLoading.value = false
        }
    }

    fun logout() {
        repository.logout()
        _isLoggedIn.value = false
    }

    sealed class AuthEvent {
        object Success : AuthEvent()
        data class Error(val message: String) : AuthEvent()
    }
}
