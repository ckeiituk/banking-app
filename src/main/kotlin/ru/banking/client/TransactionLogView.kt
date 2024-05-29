package ru.banking.client

import tornadofx.*
import java.net.Socket
import java.io.BufferedReader
import java.io.BufferedWriter
import ru.banking.models.Request
import javafx.geometry.Pos
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleStringProperty
import tornadofx.ItemViewModel
import tornadofx.getValue
import tornadofx.setValue

class TransactionLogView : View("Transaction Log") {
    private val transactionList = mutableListOf<TransactionModel>().asObservable()

    override val root = vbox {
        alignment = Pos.CENTER
        tableview(transactionList) {
            column("From Card", TransactionModel::fromCard)
            column("To Card", TransactionModel::toCard)
            column("Amount", TransactionModel::amount)
            column("Date", TransactionModel::date)
            column("Status", TransactionModel::status)
        }
        hbox(10) {
            alignment = Pos.CENTER
            button("Refresh Transactions") {
                action {
                    println("Refresh Transactions button clicked")
                    populateTransactions()
                }
            }
            button("Back") {
                action {
                    replaceWith<DashboardView>()
                }
            }
        }
    }

    init {
        populateTransactions()
    }

    private fun populateTransactions() {
        runAsync {
            fetchTransactions()
        } ui { transactions ->
            if (transactions != null) {
                transactionList.setAll(transactions)
            }
        }
    }

    private fun fetchTransactions(): List<TransactionModel>? {
        val userId = Session.userId ?: return null
        return try {
            println("Attempting to connect to server...")
            val client = Socket("127.0.0.1", 9999)
            println("Connected to server")

            val output = client.getOutputStream().bufferedWriter()
            val input = client.getInputStream().bufferedReader()

            val requestData = JsonObject(mapOf("userId" to JsonPrimitive(userId)))
            val request = Request("GET_TRANSACTIONS", requestData)

            // Вывод запроса в консоль
            val requestJson = Json.encodeToString(Request.serializer(), request)
            println("Sending request: $requestJson")

            output.write(requestJson)
            output.newLine()
            output.flush()
            println("Request sent")

            val responseJson = input.readLine()
            println("Received response: $responseJson")
            client.close()

            if (responseJson != null && responseJson.isNotEmpty()) {
                val jsonObject = Json.decodeFromString<JsonObject>(responseJson)
                val transactionsJson = jsonObject["transactions"]?.jsonArray
                if (transactionsJson != null) {
                    transactionsJson.map {
                        val jsonObject = it.jsonObject
                        TransactionModel(
                            fromCard = jsonObject["fromCard"]?.jsonPrimitive?.content ?: "",
                            toCard = jsonObject["toCard"]?.jsonPrimitive?.content ?: "",
                            amount = jsonObject["amount"]?.jsonPrimitive?.double ?: 0.0,
                            date = jsonObject["transactionDate"]?.jsonPrimitive?.content ?: "",
                            status = jsonObject["status"]?.jsonPrimitive?.content ?: ""
                        )
                    }
                } else {
                    println("No transactions found in response")
                    emptyList()
                }
            } else {
                println("Received null or empty response from server")
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    class TransactionModel(
        fromCard: String,
        toCard: String,
        amount: Double,
        date: String,
        status: String
    ) {
        val fromCardProperty = SimpleStringProperty(fromCard)
        var fromCard by fromCardProperty

        val toCardProperty = SimpleStringProperty(toCard)
        var toCard by toCardProperty

        val amountProperty = SimpleDoubleProperty(amount)
        var amount by amountProperty

        val dateProperty = SimpleStringProperty(date)
        var date by dateProperty

        val statusProperty = SimpleStringProperty(status)
        var status by statusProperty
    }

    class TransactionModelViewModel : ItemViewModel<TransactionModel>() {
        val fromCard = bind(TransactionModel::fromCardProperty)
        val toCard = bind(TransactionModel::toCardProperty)
        val amount = bind(TransactionModel::amountProperty)
        val date = bind(TransactionModel::dateProperty)
        val status = bind(TransactionModel::statusProperty)
    }
}
