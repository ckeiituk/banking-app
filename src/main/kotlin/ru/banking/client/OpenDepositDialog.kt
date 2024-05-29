package ru.banking.client

import tornadofx.*
import java.net.Socket
import javafx.beans.property.SimpleStringProperty
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
import ru.banking.models.Request

class OpenDepositDialog : Fragment("Open Deposit") {
    private val amountField = textfield()
    private val startDateField = datepicker()
    private val endDateField = datepicker()
    private val messageLabel = label()
    private val cardList = mutableListOf<String>().asObservable()
    private val selectedCard = SimpleStringProperty()

    override val root = form {
        fieldset("Open a new deposit") {
            field("Amount") {
                add(amountField)
            }
            field("Start Date") {
                add(startDateField)
            }
            field("End Date") {
                add(endDateField)
            }
            field("Select Card") {
                combobox(selectedCard, cardList)
            }
        }
        button("Submit") {
            action {
                val amount = amountField.text.toDoubleOrNull()
                val startDate = startDateField.value
                val endDate = endDateField.value
                val cardNumber = selectedCard.value?.split(",")?.get(0)?.split(":")?.get(1)?.trim()
                if (amount != null && amount > 0 && startDate != null && endDate != null && cardNumber != null) {
                    val response = openDeposit(amount, startDate.toString(), endDate.toString(), cardNumber)
                    messageLabel.text = response
                    find<DepositsView>().populateDeposits()
                    close()
                } else {
                    messageLabel.text = "Please fill in all fields with valid data"
                }
            }
        }
        add(messageLabel)
    }

    override fun onDock() {
        super.onDock()
        populateCards()
    }

    private fun populateCards() {
        runAsync {
            fetchCards()
        } ui { cards ->
            cardList.setAll(cards)
        }
    }

    private fun fetchCards(): List<String> {
        val userId = Session.userId ?: return emptyList()
        return try {
            val client = Socket("127.0.0.1", 9999)
            val output = client.getOutputStream().bufferedWriter()
            val input = client.getInputStream().bufferedReader()

            val requestData = buildJsonObject {
                put("userId", userId)
            }
            val request = Request("GET_CARDS", requestData)
            output.write(Json.encodeToString(request))
            output.newLine()
            output.flush()

            val response = input.readLine()
            client.close()

            val jsonResponse = Json.decodeFromString<JsonObject>(response)
            val cards = jsonResponse["cards"]?.jsonArray

            cards?.map {
                val cardNumber = it.jsonObject["cardNumber"]?.jsonPrimitive?.content ?: ""
                val balance = it.jsonObject["balance"]?.jsonPrimitive?.double ?: 0.0
                "Card Number: $cardNumber, Balance: $balance"
            } ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun openDeposit(amount: Double, startDate: String, endDate: String, cardNumber: String): String {
        val userId = Session.userId ?: return "User ID not found"
        return try {
            val client = Socket("127.0.0.1", 9999)
            val output = client.getOutputStream().bufferedWriter()
            val input = client.getInputStream().bufferedReader()

            val depositData = buildJsonObject {
                put("userId", userId)
                put("cardNumber", cardNumber)
                put("amount", amount)
                put("startDate", startDate)
                put("endDate", endDate)
            }
            val request = Request("OPEN_DEPOSIT", depositData)

            output.write(Json.encodeToString(request))
            output.newLine()
            output.flush()

            val response = input.readLine()
            client.close()
            response
        } catch (e: Exception) {
            e.printStackTrace()
            "Error: Unable to connect to server"
        }
    }
}
