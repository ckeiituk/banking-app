package ru.banking.models

import kotlinx.serialization.Serializable

@Serializable
data class Card(
    val cardId: Int,
    val cardNumber: String,
    val expirationDate: String,
    val cvv: Int,
    val balance: Double
)
