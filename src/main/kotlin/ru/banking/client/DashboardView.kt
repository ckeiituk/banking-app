package ru.banking.client

import tornadofx.*
import java.net.Socket
import javafx.beans.property.SimpleStringProperty
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
import ru.banking.models.Request

class DashboardView : View("Dashboard") {
    private val cardList = mutableListOf<CardModel>().asObservable()

    override val root = vbox(10) {
        label("Welcome to your dashboard!")
        tableview(cardList) {
            column("Card Number", CardModel::cardNumberProperty)
            column("Balance", CardModel::balanceProperty)
            column("Main", CardModel::isMainProperty)
        }

        hbox(10) {
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
            button("View Transaction Log") {
                action {
                    replaceWith<TransactionLogView>()
                }
            }
        }

        button("Refresh Cards") {
            action {
                populateCards()
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

    private fun fetchCards(): List<CardModel> {
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
                val cardId = it.jsonObject["cardId"]?.jsonPrimitive?.int ?: 0
                val cardNumber = it.jsonObject["cardNumber"]?.jsonPrimitive?.content ?: ""
                val expirationDate = it.jsonObject["expirationDate"]?.jsonPrimitive?.content ?: ""
                val cvv = it.jsonObject["cvv"]?.jsonPrimitive?.int ?: 0
                val balance = it.jsonObject["balance"]?.jsonPrimitive?.double ?: 0.0
                CardModel(cardId, cardNumber, expirationDate, cvv, balance, false) // isMain is not used
            } ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}

class CardModel(cardId: Int, cardNumber: String, expirationDate: String, cvv: Int, balance: Double, isMain: Boolean) {
    val cardIdProperty = SimpleStringProperty(cardId.toString())
    var cardId by cardIdProperty

    val cardNumberProperty = SimpleStringProperty(cardNumber)
    var cardNumber by cardNumberProperty

    val expirationDateProperty = SimpleStringProperty(expirationDate)
    var expirationDate by expirationDateProperty

    val cvvProperty = SimpleStringProperty(cvv.toString())
    var cvv by cvvProperty

    val balanceProperty = SimpleStringProperty(balance.toString())
    var balance by balanceProperty

    val isMainProperty = SimpleStringProperty(if (isMain) "Yes" else "No")
    var isMain by isMainProperty
}