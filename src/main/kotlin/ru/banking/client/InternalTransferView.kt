package ru.banking.client

import javafx.geometry.Pos
import tornadofx.*
import java.net.Socket
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*

import ru.banking.models.Request
import ru.banking.models.TransferResponse

class InternalTransferView : View("Internal Transfer") {
    private val amountField = textfield()
    private val messageLabel = label()
    private val fromCardChoices = mutableListOf<String>().asObservable()
    private val toCardChoices = mutableListOf<String>().asObservable()
    private val fromCardField = combobox<String> { items = fromCardChoices }
    private val toCardField = combobox<String> { items = toCardChoices }

    init {
        populateCards()
    }

    override val root = form {
        fieldset("Internal Transfer") {
            field("From Card") {
                add(fromCardField)
            }
            field("To Card") {
                add(toCardField)
            }
            field("Amount") {
                add(amountField)
            }
        }
        hbox(10) {
            alignment = Pos.CENTER
            button("Submit") {
                action {
                    val amountText = amountField.text
                    val amount = amountText.toDoubleOrNull()

                    if (amount == null || amount <= 0) {
                        messageLabel.text = "Please enter a positive amount"
                        return@action
                    }

                    val fromCard = fromCardField.selectedItem
                    val toCard = toCardField.selectedItem
                    if (fromCard != null && toCard != null) {
                        val response = handleTransfer(fromCard, toCard, amount)
                        messageLabel.text = response.message
                        if (response.status == "success") {
                            populateCards() // Refresh cards after successful transfer
                        }
                    } else {
                        messageLabel.text = "Please select both cards"
                    }
                }
            }
            button("Back") {
                action {
                    replaceWith<DashboardView>()
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
            fromCardChoices.setAll(cards)
            toCardChoices.setAll(cards)
        }
    }

    private fun fetchCards(): List<String> {
        return try {
            val client = Socket("127.0.0.1", 9999)
            val output = client.getOutputStream().bufferedWriter()
            val input = client.getInputStream().bufferedReader()

            val requestData = buildJsonObject {
                put("userId", JsonPrimitive(Session.userId))
            }
            val request = Request("GET_CARDS", requestData)
            val requestJson = Json.encodeToString(Request.serializer(), request)
            println("Sending request: $requestJson")

            output.write(requestJson)
            output.newLine()
            output.flush()
            println("Request sent")

            val responseJson = input.readLine()
            println("Received response: $responseJson")
            client.close()

            val jsonResponse = Json.decodeFromString<JsonObject>(responseJson)
            val cards = jsonResponse["cards"]?.jsonArray

            cards?.map {
                val cardNumber = it.jsonObject["cardNumber"]?.jsonPrimitive?.content ?: ""
                val balance = it.jsonObject["balance"]?.jsonPrimitive?.content ?: ""
                "Card Number: $cardNumber, Balance: $balance"
            } ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun handleTransfer(fromCard: String, toCard: String, amount: Double): TransferResponse {
        return try {
            val client = Socket("127.0.0.1", 9999)
            val output = client.getOutputStream().bufferedWriter()
            val input = client.getInputStream().bufferedReader()

            val transferData = buildJsonObject {
                put("userId", JsonPrimitive(Session.userId))
                put("fromCard", JsonPrimitive(fromCard.split(",")[0].split(":")[1].trim()))
                put("toCard", JsonPrimitive(toCard.split(",")[0].split(":")[1].trim()))
                put("amount", JsonPrimitive(amount))
            }
            val request = Request("INTERNAL_TRANSFER", transferData)
            val requestJson = Json.encodeToString(Request.serializer(), request)
            println("Sending request: $requestJson")

            output.write(requestJson)
            output.newLine()
            output.flush()
            println("Request sent")

            val responseJson = input.readLine()
            println("Received response: $responseJson")
            client.close()

            Json.decodeFromString(responseJson)
        } catch (e: Exception) {
            e.printStackTrace()
            TransferResponse("error", "Unable to complete the transfer")
        }
    }
}
