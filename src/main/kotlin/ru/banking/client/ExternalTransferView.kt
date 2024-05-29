package ru.banking.client

import javafx.geometry.Pos
import tornadofx.*
import java.net.Socket
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import ru.banking.models.Request
import ru.banking.models.TransferResponse

class ExternalTransferView : View("External Transfer") {
    private val cardNumberField = textfield()
    private val amountField = textfield()
    private val messageLabel = label()
    private val fromCardChoices = mutableListOf<String>().asObservable()
    private val fromCardField = combobox<String> { items = fromCardChoices }

    init {
        populateCards()
    }

    override val root = form {
        fieldset("Transfer to Another Client") {
            field("From Card") {
                add(fromCardField)
            }
            field("To Card Number") {
                add(cardNumberField)
            }
            field("Amount") {
                add(amountField)
            }
        }
        hbox(10) {
            alignment = Pos.CENTER
            button("Submit") {
                action {
                    val cardNumber = cardNumberField.text
                    val amount = amountField.text.toDoubleOrNull()
                    val fromCard = fromCardField.selectedItem

                    if (fromCard == null) {
                        messageLabel.text = "Please select a card"
                        return@action
                    }

                    if (!isValidCardNumber(cardNumber)) {
                        messageLabel.text = "Invalid card number"
                        return@action
                    }

                    if (amount == null || amount <= 0) {
                        messageLabel.text = "Please enter a valid amount"
                        return@action
                    }

                    val response = handleTransfer(fromCard, cardNumber, amount)
                    messageLabel.text = response.message
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

    private fun populateCards() {
        runAsync {
            fetchCards()
        } ui { cards ->
            fromCardChoices.setAll(cards)
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

    private fun isValidCardNumber(cardNumber: String): Boolean {
        return cardNumber.all { it.isDigit() } && cardNumber.length == 16
    }

    private fun handleTransfer(fromCard: String, toCardNumber: String, amount: Double): TransferResponse {
        return try {
            val client = Socket("127.0.0.1", 9999)
            val output = client.getOutputStream().bufferedWriter()
            val input = client.getInputStream().bufferedReader()

            val fromCardNumber = fromCard.split(",")[0].split(":").getOrNull(1)?.trim() ?: return TransferResponse("error", "Invalid from card format")

            val transferData = buildJsonObject {
                put("userId", JsonPrimitive(Session.userId))
                put("fromCard", JsonPrimitive(fromCardNumber))
                put("toCard", JsonPrimitive(toCardNumber))
                put("amount", JsonPrimitive(amount))
            }
            val request = Request("EXTERNAL_TRANSFER", transferData)
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
