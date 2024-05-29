package ru.banking.services

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import ru.banking.database.Cards
import ru.banking.database.Transactions
import ru.banking.models.Card
import ru.banking.models.TransferResponse
import java.time.LocalDateTime
import kotlinx.serialization.json.*

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

        return transaction {
            Cards.insert {
                it[Cards.userId] = userId
                it[Cards.cardNumber] = cardNumber
                it[Cards.expirationDate] = expirationDate
                it[Cards.cvv] = cvv
                it[Cards.balance] = initialBalance
            }
            "Card opened successfully"
        }
    }

    fun handleInternalTransfer(data: JsonObject): TransferResponse {
        return handleTransfer(data, isExternal = false)
    }

    fun handleExternalTransfer(data: JsonObject): TransferResponse {
        return handleTransfer(data, isExternal = true)
    }

    private fun handleTransfer(data: JsonObject, isExternal: Boolean): TransferResponse {
        val userId = data["userId"]?.jsonPrimitive?.intOrNull ?: return TransferResponse("error", "User ID not found")
        val fromCardNumber = data["fromCard"]?.jsonPrimitive?.content ?: return TransferResponse("error", "From Card not found")
        val toCardNumber = data["toCard"]?.jsonPrimitive?.content ?: return TransferResponse("error", "To Card not found")
        val amount = data["amount"]?.jsonPrimitive?.doubleOrNull ?: return TransferResponse("error", "Amount not found")

        if (amount <= 0) {
            return TransferResponse("error", "Amount must be positive")
        }

        return transaction {
            println("Checking from card: $fromCardNumber")
            val fromCard = Cards.select { Cards.cardNumber eq fromCardNumber }.singleOrNull()
            println("From card query result: $fromCard")
            if (fromCard == null) {
                println("From Card not found: $fromCardNumber")
                return@transaction TransferResponse("error", "From Card not found")
            }

            val fromCardBalance = fromCard[Cards.balance]
            println("From card balance: $fromCardBalance")
            if (fromCardBalance < amount) {
                println("Insufficient funds: $fromCardNumber, Balance: $fromCardBalance, Amount: $amount")
                return@transaction TransferResponse("error", "Insufficient funds")
            }

            // Check if the toCard exists
            println("Checking to card: $toCardNumber")
            val toCard = Cards.select { Cards.cardNumber eq toCardNumber }.singleOrNull()
            println("To card query result: $toCard")
            if (toCard == null) {
                println("To Card not found: $toCardNumber")
                return@transaction TransferResponse("error", "To Card not found")
            }

            println("Transferring amount: $amount from $fromCardNumber to $toCardNumber")

            Cards.update({ Cards.cardNumber eq fromCardNumber }) {
                with(SqlExpressionBuilder) {
                    it.update(Cards.balance, Cards.balance - amount)
                }
            }

            if (!isExternal) {
                Cards.update({ Cards.cardNumber eq toCardNumber }) {
                    with(SqlExpressionBuilder) {
                        it.update(Cards.balance, Cards.balance + amount)
                    }
                }
            }

            Transactions.insert {
                it[Transactions.fromCard] = fromCardNumber
                it[Transactions.toCard] = toCardNumber
                it[Transactions.amount] = amount
                it[Transactions.transactionDate] = LocalDateTime.now()
                it[Transactions.status] = "Completed"
            }

            TransferResponse("success", "Transaction completed")
        }
    }

    fun checkCardExists(cardNumber: String): Boolean {
        return transaction {
            val card = Cards.select { Cards.cardNumber eq cardNumber }.singleOrNull()
            println("Checking card existence for card number: $cardNumber")
            println("Query result: $card")
            card != null
        }
    }

    fun getTransactions(data: JsonObject): List<JsonObject> {
        val userId = data["userId"]?.jsonPrimitive?.intOrNull ?: return emptyList()

        val fromTransactions = transaction {
            Transactions
                .select { Transactions.fromCard inSubQuery (Cards.slice(Cards.cardNumber).select { Cards.userId eq userId }) }
                .map {
                    JsonObject(mapOf(
                        "fromCard" to JsonPrimitive(it[Transactions.fromCard]),
                        "toCard" to JsonPrimitive(it[Transactions.toCard]),
                        "amount" to JsonPrimitive(it[Transactions.amount]),
                        "transactionDate" to JsonPrimitive(it[Transactions.transactionDate].toString()),
                        "status" to JsonPrimitive(it[Transactions.status])
                    ))
                }
        }

        val toTransactions = transaction {
            Transactions
                .select { Transactions.toCard inSubQuery (Cards.slice(Cards.cardNumber).select { Cards.userId eq userId }) }
                .map {
                    JsonObject(mapOf(
                        "fromCard" to JsonPrimitive(it[Transactions.fromCard]),
                        "toCard" to JsonPrimitive(it[Transactions.toCard]),
                        "amount" to JsonPrimitive(it[Transactions.amount]),
                        "transactionDate" to JsonPrimitive(it[Transactions.transactionDate].toString()),
                        "status" to JsonPrimitive(it[Transactions.status])
                    ))
                }
        }

        return (fromTransactions + toTransactions).distinct()
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
