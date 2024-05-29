package ru.banking.client

import javafx.geometry.Pos
import tornadofx.*
import java.net.Socket
import ru.banking.models.Request
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.jsonPrimitive

class OpenCardView : View("Open Card") {
    private val messageLabel = label()

    override val root = form {
        fieldset("Open a new card") {
            field("Status") {
                add(messageLabel)
            }
        }
        button("Submit") {
            action {
                println("Submit button clicked")
                val response = openCard()
                handleResponse(response)
                println("Response: $response")
            }
        }
        button("Back") {
            action {
                println("Back button clicked")
                replaceWith<DashboardView>()
            }
        }
    }

    private fun openCard(): String {
        val userId = Session.userId ?: return "User ID not found"
        println("User ID: $userId")

        return try {
            println("Connecting to server...")
            val client = Socket("127.0.0.1", 9999)
            println("Connected to server")

            val output = client.getOutputStream().bufferedWriter()
            val input = client.getInputStream().bufferedReader()

            val requestData = buildJsonObject {
                put("userId", userId)
            }
            val request = Request("OPEN_CARD", requestData)
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
            val message = jsonResponse["message"]?.jsonPrimitive?.content

            if (message != null) {
                messageLabel.text = message
            } else {
                messageLabel.text = "Error: No message from server"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            messageLabel.text = "Error: Unable to process server response"
        }
    }
}
