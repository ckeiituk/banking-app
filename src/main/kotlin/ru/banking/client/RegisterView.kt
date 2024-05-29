package ru.banking.client

import javafx.geometry.Pos
import tornadofx.*
import java.net.Socket
import ru.banking.models.Request
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class RegisterView : View("Register") {
    private val nameField = textfield()
    private val emailField = textfield()
    private val passwordField = passwordfield()
    private val confirmPasswordField = passwordfield()
    private val messageLabel = label()

    override val root = vbox(10) {
        alignment = Pos.CENTER
        form {
            fieldset("Register") {
                field("Name") {
                    add(nameField)
                }
                field("Email") {
                    add(emailField)
                }
                field("Password") {
                    add(passwordField)
                }
                field("Confirm Password") {
                    add(confirmPasswordField)
                }
            }
        }
        button("Register") {
            action {
                println("Register button clicked")
                val name = nameField.text
                val email = emailField.text
                val password = passwordField.text
                val confirmPassword = confirmPasswordField.text
                if (password != confirmPassword) {
                    messageLabel.text = "Passwords do not match"
                } else {
                    val response = register(name, email, password)
                    handleResponse(response)
                }
            }
        }
        add(messageLabel)
        button("Back") {
            action {
                println("Back button clicked")
                replaceWith<LoginView>()
            }
        }
    }

    private fun register(name: String, email: String, password: String): String {
        println("User details - Name: $name, Email: $email")

        return try {
            println("Connecting to server...")
            val client = Socket("127.0.0.1", 9999)
            println("Connected to server")

            val output = client.getOutputStream().bufferedWriter()
            val input = client.getInputStream().bufferedReader()

            val requestData = buildJsonObject {
                put("name", name)
                put("email", email)
                put("password", password)
            }
            val request = Request("REGISTER", requestData)
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
            val status = jsonResponse["status"]?.jsonPrimitive?.content
            val message = jsonResponse["message"]?.jsonPrimitive?.content

            if (status == "success") {
                messageLabel.text = "Registration successful: $message"
            } else {
                messageLabel.text = "Registration failed: $message"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            messageLabel.text = "Error: Unable to process server response"
        }
    }
}
