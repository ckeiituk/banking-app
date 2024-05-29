package ru.banking.server

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import ru.banking.database.DatabaseFactory
import ru.banking.models.Request
import ru.banking.services.TransactionService
import ru.banking.services.UserService
import java.io.EOFException
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

fun main() {
    DatabaseFactory.init()

    // Check if a specific card exists when the server starts
    val testCardNumber = "8143017180189305"
    val cardExists = TransactionService.checkCardExists(testCardNumber)
    if (cardExists) {
        println("Test card $testCardNumber exists in the database.")
    } else {
        println("Test card $testCardNumber does NOT exist in the database.")
    }

    val server = ServerSocket(9999)
    println("Server is running on port 9999")

    while (true) {
        val client = server.accept()
        println("Client connected: ${client.inetAddress}")
        thread { ClientHandler(client).run() }
    }
}

class ClientHandler(private val client: Socket) : Runnable {
    override fun run() {
        client.use {
            try {
                val input = client.getInputStream().bufferedReader()
                val output = client.getOutputStream().bufferedWriter()

                while (true) {
                    val requestJson = input.readLine() ?: break
                    println("Received request JSON: $requestJson")
                    val request = Json.decodeFromString<Request>(requestJson)
                    println("Decoded request: $request")

                    // Extract and log individual components
                    val requestData = request.data
                    val userId = requestData["userId"]?.jsonPrimitive?.intOrNull
                    val fromCardNumber = requestData["fromCard"]?.jsonPrimitive?.content
                    val toCardNumber = requestData["toCardNumber"]?.jsonPrimitive?.content
                    val amount = requestData["amount"]?.jsonPrimitive?.doubleOrNull

                    println("Extracted values - UserId: $userId, FromCard: $fromCardNumber, ToCard: $toCardNumber, Amount: $amount")

                    // Check if the toCard exists
                    if (toCardNumber != null) {
                        val toCardExists = TransactionService.checkCardExists(toCardNumber)
                        println("To Card existence check for $toCardNumber: $toCardExists")
                    }

                    val response = handleRequest(request)
                    val responseJson = Json.encodeToString(response)
                    println("Sending response: $responseJson")
                    output.write(responseJson)
                    output.newLine()
                    output.flush()
                }
            } catch (e: EOFException) {
                println("Client disconnected: ${client.inetAddress}")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun handleRequest(request: Request): JsonObject {
        return when (request.type) {
            "AUTH" -> createJsonResponse(UserService.authenticate(request.data))
            "REGISTER" -> createJsonResponse(UserService.register(request.data))
            "GET_CARDS" -> JsonObject(mapOf("cards" to Json.encodeToJsonElement(TransactionService.getCards(request.data))))
            "OPEN_CARD" -> JsonObject(mapOf("message" to JsonPrimitive(TransactionService.openCard(request.data))))
            "INTERNAL_TRANSFER" -> {
                val response = TransactionService.handleInternalTransfer(request.data)
                JsonObject(mapOf("status" to JsonPrimitive(response.status), "message" to JsonPrimitive(response.message)))
            }
            "EXTERNAL_TRANSFER" -> {
                val response = TransactionService.handleExternalTransfer(request.data)
                JsonObject(mapOf("status" to JsonPrimitive(response.status), "message" to JsonPrimitive(response.message)))
            }
            "GET_TRANSACTIONS" -> {
                val transactions = TransactionService.getTransactions(request.data)
                JsonObject(mapOf("transactions" to Json.encodeToJsonElement(transactions)))
            }
            else -> JsonObject(mapOf("error" to JsonPrimitive("Invalid request type")))
        }
    }

    private fun createJsonResponse(data: Map<String, Any>): JsonObject {
        val jsonMap = data.mapValues { entry ->
            when (val value = entry.value) {
                is String -> JsonPrimitive(value)
                is Number -> JsonPrimitive(value)
                is Boolean -> JsonPrimitive(value)
                is List<*> -> Json.encodeToJsonElement(value)
                is Map<*, *> -> Json.encodeToJsonElement(value as Map<String, Any>)
                else -> JsonPrimitive(value.toString())
            }
        }
        return JsonObject(jsonMap)
    }
}
