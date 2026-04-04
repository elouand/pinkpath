package com.traveling.domain.model

data class User(
    val username: String,
    val password: String // En situation réelle, on ne stocke jamais le mot de passe en clair
)
