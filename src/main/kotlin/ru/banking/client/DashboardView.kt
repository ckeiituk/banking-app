package ru.banking.client

import javafx.geometry.Pos
import tornadofx.*
import java.net.Socket
import ru.banking.models.Request
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*

class DashboardView : View("Dashboard") {
    private val cardsList = mutableListOf<String>().asObservable()
    private val messageLabel = label()

    override val root = vbox(10) {
        alignment = Pos.CENTER
        label("Welcome to your dashboard!")
        button("Open Card") {
            action {
                replaceWith<OpenCardView>()
            }
        }
        button("View Deposits") {
            action {
                replaceWith<DepositsView>()
            }
        }
        button("Internal Transfer") {
            action {
                replaceWith<InternalTransferView>()
            }
        }
        button("Transfer to Another Client") {
            action {
                replaceWith<ExternalTransferView>()
            }
        }
        listview(cardsList)
        button("Refresh Cards") {
            action {
                println("Refresh button clicked")
                refreshCards()
            }
        }
        add(messageLabel)
    }

    init {
        refreshCards()
    }

    private fun refreshCards() {
        val response = fetchCards()
        handleResponse(response)
    }

    private fun fetchCards(): String {
        val userId = Session.userId ?: return "User ID not found"
        println("User ID: $userId")

        return try {
            println("Connecting to server...")
            val client = Socket("127.0.0.1", 9999)
            println("Connected to server")

            val output = client.getOutputStream().bufferedWriter()
            val input = client.getInputStream().bufferedReader()

            val requestData = buildJsonObject {
                put("userId", JsonPrimitive(userId))  // Используем JsonPrimitive для userId
            }
            val request = Request("GET_CARDS", requestData)
            val requestJson = Json.encodeToString(Request.serializer(), request)
            println("Sending request: $requestJson")

            output.write(requestJson)
            output.newLine()
            output.flush()
            println("Request sent")

            val response = input.readLine()
            println("Received response: $response")

            client.close()
            println("Connection closed")
            response
        } catch (e: Exception) {
            e.printStackTrace()
            "Error: Unable to connect to server"
        }
    }

    private fun handleResponse(response: String) {
        try {
            val jsonResponse = Json.decodeFromString<JsonObject>(response)
            val cards = jsonResponse["cards"]?.jsonArray

            if (cards != null) {
                cardsList.clear()
                for (card in cards) {
                    val cardNumber = card.jsonObject["cardNumber"]?.jsonPrimitive?.content
                    val expirationDate = card.jsonObject["expirationDate"]?.jsonPrimitive?.content
                    val cvv = card.jsonObject["cvv"]?.jsonPrimitive?.content
                    val balance = card.jsonObject["balance"]?.jsonPrimitive?.content

                    cardsList.add("Card Number: $cardNumber, Expiration Date: $expirationDate, CVV: $cvv, Balance: $balance")
                }
                messageLabel.text = "Cards fetched successfully"
            } else {
                messageLabel.text = "Error: No cards found"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            messageLabel.text = "Error: Unable to process server response"
        }
    }
}
