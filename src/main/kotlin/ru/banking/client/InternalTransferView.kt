package ru.banking.client

import javafx.geometry.Pos
import tornadofx.*
import java.net.Socket
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import ru.banking.models.Request
import ru.banking.models.TransferResponse
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class InternalTransferView : View("Internal Transfer") {
    private val amountField = textfield()
    private val messageLabel = label()
    private val fromCardChoices = mutableListOf<String>().asObservable()
    private val toCardChoices = mutableListOf<String>().asObservable()
    private val fromCardField = combobox<String> { items = fromCardChoices }
    private val toCardField = combobox<String> { items = toCardChoices }

    init {
        populateCards()
    }

    override val root = form {
        fieldset("Internal Transfer") {
            field("From Card") {
                add(fromCardField)
            }
            field("To Card") {
                add(toCardField)
            }
            field("Amount") {
                add(amountField)
            }
        }
        hbox(10) {
            alignment = Pos.CENTER
            button("Submit") {
                action {
                    val amount = amountField.text.toDouble()
                    val fromCard = fromCardField.selectedItem
                    val toCard = toCardField.selectedItem
                    if (fromCard != null && toCard != null) {
                        val response = handleTransfer(fromCard, toCard, amount)
                        messageLabel.text = response.message
                    } else {
                        messageLabel.text = "Please select both cards"
                    }
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
            toCardChoices.setAll(cards)
        }
    }

    private fun fetchCards(): List<String> {
        return try {
            val client = Socket("127.0.0.1", 9999)
            val output = ObjectOutputStream(client.getOutputStream())
            val input = ObjectInputStream(client.getInputStream())

            val requestData = buildJsonObject {
                put("userId", Session.userId as Int)
            }
            val request = Request("GET_CARDS", requestData)

            output.writeObject(Json.encodeToString(Request.serializer(), request))
            output.flush()

            val response = input.readObject() as String
            client.close()

            val cards = Json.decodeFromString<List<Map<String, Any>>>(response)
            cards.map { it["cardNumber"].toString() }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun handleTransfer(fromCard: String, toCard: String, amount: Double): TransferResponse {
        return try {
            val client = Socket("127.0.0.1", 9999)
            val output = ObjectOutputStream(client.getOutputStream())
            val input = ObjectInputStream(client.getInputStream())

            val transferData = buildJsonObject {
                put("userId", Session.userId as Int)
                put("fromCard", fromCard)
                put("toCard", toCard)
                put("amount", amount)
            }
            val request = Request("INTERNAL_TRANSFER", transferData)

            output.writeObject(Json.encodeToString(Request.serializer(), request))
            output.flush()

            val response = input.readObject() as String

            client.close()
            Json.decodeFromString(response)
        } catch (e: Exception) {
            e.printStackTrace()
            TransferResponse("error", "Unable to complete the transfer")
        }
    }
}
