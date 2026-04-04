package com.traveling.ui.travelshare

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.traveling.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    val currentUser = repository.currentUser

    private val _authEvent = MutableSharedFlow<AuthEvent>()
    val authEvent = _authEvent.asSharedFlow()

    fun login(username: String, password: String) {
        viewModelScope.launch {
            if (username.isBlank() || password.isBlank()) {
                _authEvent.emit(AuthEvent.Error("Veuillez remplir tous les champs"))
                return@launch
            }
            val success = repository.login(username, password)
            if (success) {
                _authEvent.emit(AuthEvent.Success)
            } else {
                _authEvent.emit(AuthEvent.Error("Identifiants incorrects"))
            }
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
            val success = repository.signup(username, password)
            if (success) {
                _authEvent.emit(AuthEvent.Success)
            } else {
                _authEvent.emit(AuthEvent.Error("Cet utilisateur existe déjà"))
            }
        }
    }

    fun logout() {
        repository.logout()
    }

    sealed class AuthEvent {
        object Success : AuthEvent()
        data class Error(val message: String) : AuthEvent()
    }
}
