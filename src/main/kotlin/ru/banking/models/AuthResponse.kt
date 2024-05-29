package ru.banking.models

import kotlinx.serialization.Serializable

@Serializable
data class AuthResponse(
    val status: String,
    val message: String,
    val userId: Int? = null
)
