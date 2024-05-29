package ru.banking.client

import tornadofx.*
import java.net.Socket
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import ru.banking.models.Request

class DashboardView : View("Dashboard") {
    private val cardList = mutableListOf<String>().asObservable()

    override val root = vbox(10) {
        label("Welcome to your dashboard!")
        listview(cardList)

        button("Open Card") {
            action {
                replaceWith<OpenCardView>()
            }
        }
        button("View Deposits") {
            action {
                //replaceWith<DepositsView>()
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
        button("View Transaction Log") {
            action {
                replaceWith<TransactionLogView>()
            }
        }
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
}
