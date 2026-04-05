package com.traveling.domain.model

data class AuthRequest(
    val username: String,
    val password: String
)

data class AuthResponse(
    val message: String,
    val token: String,
    val user: UserData
)

data class UserData(
    val id: String,
    val username: String,
    val profileUrl: String? = null
)

data class RegisterResponse(
    val message: String,
    val userId: String
)

data class ProfilePictureResponse(
    val message: String,
    val profileUrl: String
)
