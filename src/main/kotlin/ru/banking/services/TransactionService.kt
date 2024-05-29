package ru.banking.services

import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.*
import ru.banking.database.Cards
import ru.banking.models.Card
import java.time.LocalDateTime
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.intOrNull

object TransactionService {
    fun getCards(data: JsonObject): List<Card> {
        val userId = data["userId"]?.jsonPrimitive?.intOrNull ?: return emptyList()
        return transaction {
            Cards.select { Cards.userId eq userId }
                .map {
                    Card(
                        cardId = it[Cards.id].value,
                        cardNumber = it[Cards.cardNumber],
                        expirationDate = it[Cards.expirationDate].toString(),
                        cvv = it[Cards.cvv],
                        balance = it[Cards.balance]
                    )
                }
        }
    }

    fun openCard(data: JsonObject): String {
        val userId = data["userId"]?.jsonPrimitive?.intOrNull ?: return "User ID not found"
        val cardNumber = generateCardNumber()
        val expirationDate = generateExpirationDate()
        val cvv = generateCVV()
        val initialBalance = 0.00

        val result = transaction {
            Cards.insert {
                it[Cards.userId] = userId
                it[Cards.cardNumber] = cardNumber
                it[Cards.expirationDate] = expirationDate
                it[Cards.cvv] = cvv
                it[Cards.balance] = initialBalance
            }
            "Card opened successfully"
        }

        println("Card Details:")
        println("User ID: $userId")
        println("Card Number: $cardNumber")
        println("Expiration Date: $expirationDate")
        println("CVV: $cvv")
        println("Initial Balance: $initialBalance")

        return result
    }

    private fun generateCardNumber(): String {
        return (1..16).map { kotlin.random.Random.nextInt(0, 10) }.joinToString("")
    }

    private fun generateExpirationDate(): LocalDateTime {
        return LocalDateTime.now().plusYears(3)
    }

    private fun generateCVV(): Int {
        return kotlin.random.Random.nextInt(100, 1000)
    }
}
