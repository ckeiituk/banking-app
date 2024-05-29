package ru.banking.models

import kotlinx.serialization.Serializable

@Serializable
data class TransferResponse(
    val status: String,
    val message: String
)