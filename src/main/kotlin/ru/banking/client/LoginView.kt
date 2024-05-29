package ru.banking.client

import javafx.geometry.Pos
import tornadofx.*
import java.net.Socket
import ru.banking.models.AuthResponse
import ru.banking.models.Request
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.put

class LoginView : View("Banking App") {
    private val emailField = textfield()
    private val passwordField = passwordfield()
    private val messageLabel = label()

    override val root = vbox(10) {
        alignment = Pos.CENTER
        add(emailField)
        add(passwordField)
        button("Login") {
            action {
                val email = emailField.text
                val password = passwordField.text
                val response = authenticate(email, password)
                if (response.status == "success") {
                    Session.userId = response.userId
                    replaceWith<DashboardView>()
                } else {
                    messageLabel.text = response.message
                }
            }
        }
        add(messageLabel)
        button("Register") {
            action {
                replaceWith<RegisterView>()
            }
        }
    }

    private fun authenticate(email: String, password: String): AuthResponse {
        return try {
            val client = Socket("127.0.0.1", 9999)
            val output = client.getOutputStream().bufferedWriter()
            val input = client.getInputStream().bufferedReader()

            val requestData = buildJsonObject {
                put("email", email)
                put("password", password)
            }
            val request = Request("AUTH", requestData)
            val requestJson = Json.encodeToString(Request.serializer(), request)
            println("Sending request: $requestJson")

            output.write(requestJson)
            output.newLine()
            output.flush()
            println("Request sent")

            val responseJson = input.readLine()
            println("Received response: $responseJson")

            client.close()
            println("Connection closed")

            Json.decodeFromString(responseJson)
        } catch (e: Exception) {
            e.printStackTrace()
            AuthResponse("error", "Unable to connect to server", null)
        }
    }
}
