package com.traveling.domain.model

data class AuthRequest(
    val username: String,
    val password: String,
    val pseudo: String? = null,
    val email: String? = null
)

data class AuthResponse(
    val message: String,
    val token: String,
    val user: UserData
)

data class UserData(
    val id: String,
    val username: String? = null,
    val pseudo: String? = null,
    val email: String? = null,
    val profileUrl: String? = null
)

data class RegisterResponse(
    val message: String,
    val userId: String,
    val pseudo: String? = null
)

data class ProfilePictureResponse(
    val message: String,
    val profileUrl: String
)
