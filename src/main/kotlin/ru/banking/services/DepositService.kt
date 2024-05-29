package ru.banking.services

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import ru.banking.database.Cards
import ru.banking.database.Deposits
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlinx.serialization.json.*
import ru.banking.models.TransferResponse

object DepositService {
    fun openDeposit(data: JsonObject): String {
        val userId = data["userId"]?.jsonPrimitive?.intOrNull ?: return "User ID not found"
        val cardNumber = data["cardNumber"]?.jsonPrimitive?.content ?: return "Card number not found"
        val amount = data["amount"]?.jsonPrimitive?.doubleOrNull?.toBigDecimal() ?: return "Amount not found"
        val startDate = data["startDate"]?.jsonPrimitive?.content?.let { LocalDate.parse(it) } ?: return "Start date not found"
        val endDate = data["endDate"]?.jsonPrimitive?.content?.let { LocalDate.parse(it) } ?: return "End date not found"

        if (amount <= 0.toBigDecimal()) {
            return "Amount must be positive"
        }

        return transaction {
            // Check if card exists and has sufficient funds
            val card = Cards.select { Cards.cardNumber eq cardNumber }.singleOrNull()
            if (card == null) {
                return@transaction "Card not found"
            }
            val cardBalance = card[Cards.balance].toBigDecimal()

            if (cardBalance < amount) {
                return@transaction "Insufficient funds"
            }

            // Deduct amount from card balance
            Cards.update({ Cards.cardNumber eq cardNumber }) {
                it[Cards.balance] = (cardBalance - amount).toDouble()
            }

            // Create deposit
            Deposits.insert {
                it[Deposits.userId] = userId
                it[Deposits.amount] = amount
                it[Deposits.startDate] = startDate
                it[Deposits.endDate] = endDate
            }
            "Deposit opened successfully"
        }
    }

    fun closeDeposit(data: JsonObject): TransferResponse {
        val depositId = data["depositId"]?.jsonPrimitive?.intOrNull ?: return TransferResponse("error", "Deposit ID not found")
        val userId = data["userId"]?.jsonPrimitive?.intOrNull ?: return TransferResponse("error", "User ID not found")

        return transaction {
            val deposit = Deposits.select { Deposits.id eq depositId }.singleOrNull() ?: return@transaction TransferResponse("error", "Deposit not found")
            val endDate = deposit[Deposits.endDate]
            val amount = deposit[Deposits.amount]
            val startDate = deposit[Deposits.startDate]

            val isEarlyClosure = LocalDate.now().isBefore(endDate)

            val finalAmount = if (isEarlyClosure) {
                // Apply penalty for early closure (e.g., 1% of the deposit amount)
                amount - (amount * 0.01.toBigDecimal())
            } else {
                // Apply interest if closing at the end of the term
                val interestRate = 0.05 // Example interest rate: 5% annually
                val days = ChronoUnit.DAYS.between(startDate, endDate)
                val interest = amount * interestRate.toBigDecimal() * (days.toBigDecimal() / 365.toBigDecimal())
                amount + interest
            }

            Deposits.update({ Deposits.id eq depositId }) {
                it[Deposits.status] = "closed"
            }

            // Find the card with the smallest id for the user
            val card = Cards.select { Cards.userId eq userId }.orderBy(Cards.id to SortOrder.ASC).limit(1).singleOrNull()
            if (card == null) {
                return@transaction TransferResponse("error", "No card found for user")
            }
            val cardNumber = card[Cards.cardNumber]
            val cardBalance = card[Cards.balance].toBigDecimal()
            Cards.update({ Cards.cardNumber eq cardNumber }) {
                it[Cards.balance] = (cardBalance + finalAmount).toDouble()
            }

            TransferResponse("success", "Deposit closed successfully. Amount transferred: $finalAmount")
        }
    }

    fun getDeposits(userId: Int): List<JsonObject> {
        return transaction {
            Deposits.select { Deposits.userId eq userId }
                .map {
                    JsonObject(mapOf(
                        "id" to JsonPrimitive(it[Deposits.id].value),
                        "amount" to JsonPrimitive(it[Deposits.amount].toString()),
                        "startDate" to JsonPrimitive(it[Deposits.startDate].toString()),
                        "endDate" to JsonPrimitive(it[Deposits.endDate].toString()),
                        "status" to JsonPrimitive(it[Deposits.status])
                    ))
                }
        }
    }
}