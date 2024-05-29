package ru.banking.services

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import ru.banking.database.Cards
import ru.banking.database.Transactions
import ru.banking.models.Card
import ru.banking.models.TransferResponse
import java.time.LocalDateTime
import kotlinx.serialization.json.*
import org.jetbrains.exposed.dao.id.EntityID

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

    fun handleInternalTransfer(data: JsonObject): TransferResponse {
        val userId = data["userId"]?.jsonPrimitive?.intOrNull ?: return TransferResponse("error", "User ID not found")
        val fromCardNumber = data["fromCard"]?.jsonPrimitive?.content ?: return TransferResponse("error", "From Card not found")
        val toCardNumber = data["toCard"]?.jsonPrimitive?.content ?: return TransferResponse("error", "To Card not found")
        val amount = data["amount"]?.jsonPrimitive?.doubleOrNull ?: return TransferResponse("error", "Amount not found")

        if (amount <= 0) {
            return TransferResponse("error", "Amount must be positive")
        }

        return transaction {
            val fromCard = Cards.select { Cards.cardNumber eq fromCardNumber }.singleOrNull()
            val toCard = Cards.select { Cards.cardNumber eq toCardNumber }.singleOrNull()

            if (fromCard == null || toCard == null) {
                TransferResponse("error", "One or both cards not found")
            } else {
                val fromCardBalance = fromCard[Cards.balance]
                val toCardBalance = toCard[Cards.balance]

                if (fromCardBalance >= amount) {
                    Cards.update({ Cards.cardNumber eq fromCardNumber }) {
                        it[Cards.balance] = fromCardBalance - amount
                    }

                    Cards.update({ Cards.cardNumber eq toCardNumber }) {
                        it[Cards.balance] = toCardBalance + amount
                    }

                    Transactions.insert {
                        it[Transactions.fromCard] = fromCard[Cards.id].value
                        it[Transactions.toCard] = toCard[Cards.id].value
                        it[Transactions.amount] = amount
                        it[Transactions.transactionDate] = LocalDateTime.now()
                        it[Transactions.status] = "Completed"
                    }

                    TransferResponse("success", "Transaction completed")
                } else {
                    TransferResponse("error", "Insufficient funds")
                }
            }
        }
    }

    fun getTransactions(data: JsonObject): List<JsonObject> {
        val userId = data["userId"]?.jsonPrimitive?.intOrNull ?: return emptyList()

        val fromTransactions = transaction {
            Transactions
                .select { Transactions.fromCard inSubQuery (Cards.slice(Cards.id).select { Cards.userId eq userId }) }
                .map {
                    JsonObject(mapOf(
                        "fromCard" to JsonPrimitive(it[Transactions.fromCard].toString()),
                        "toCard" to JsonPrimitive(it[Transactions.toCard].toString()),
                        "amount" to JsonPrimitive(it[Transactions.amount]),
                        "transactionDate" to JsonPrimitive(it[Transactions.transactionDate].toString()),
                        "status" to JsonPrimitive(it[Transactions.status])
                    ))
                }
        }

        val toTransactions = transaction {
            Transactions
                .select { Transactions.toCard inSubQuery (Cards.slice(Cards.id).select { Cards.userId eq userId }) }
                .map {
                    JsonObject(mapOf(
                        "fromCard" to JsonPrimitive(it[Transactions.fromCard].toString()),
                        "toCard" to JsonPrimitive(it[Transactions.toCard].toString()),
                        "amount" to JsonPrimitive(it[Transactions.amount]),
                        "transactionDate" to JsonPrimitive(it[Transactions.transactionDate].toString()),
                        "status" to JsonPrimitive(it[Transactions.status])
                    ))
                }
        }

        return fromTransactions + toTransactions
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
