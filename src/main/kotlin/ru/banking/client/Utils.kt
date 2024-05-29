package ru.banking.client

import java.net.Socket
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import ru.banking.models.Request

object Utils {
    fun getAccountId(userId: Int): Int? {
        return try {
            val client = Socket("127.0.0.1", 9999)
            val output = client.getOutputStream().bufferedWriter()
            val input = client.getInputStream().bufferedReader()

            val requestData = buildJsonObject {
                put("userId", userId)
            }
            val request = Request("GET_ACCOUNT_ID", requestData)
            val requestJson = Json.encodeToString(request)
            output.write(requestJson)
            output.newLine()
            output.flush()

            val response = input.readLine()
            client.close()

            val jsonResponse = Json.decodeFromString<JsonObject>(response)
            jsonResponse["accountId"]?.jsonPrimitive?.intOrNull
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
