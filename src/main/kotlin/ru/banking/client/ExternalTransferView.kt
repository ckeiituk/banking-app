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

class ExternalTransferView : View("External Transfer") {
    private val cardNumberField = textfield()
    private val amountField = textfield()
    private val messageLabel = label()
    private val fromCardChoices = mutableListOf<String>().asObservable()
    private val fromCardField = combobox<String> { items = fromCardChoices }

    init {
        populateCards()
    }

    override val root = form {
        fieldset("Transfer to Another Client") {
            field("From Card") {
                add(fromCardField)
            }
            field("Card Number") {
                add(cardNumberField)
            }
            field("Amount") {
                add(amountField)
            }
        }
        hbox(10) {
            alignment = Pos.CENTER
            button("Submit") {
                action {
                    val cardNumber = cardNumberField.text
                    val amount = amountField.text.toDouble()
                    val fromCard = fromCardField.selectedItem
                    if (fromCard != null) {
                        val response = handleTransfer(fromCard, cardNumber, amount)
                        messageLabel.text = response.message
                    } else {
                        messageLabel.text = "Please select a card"
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

    private fun handleTransfer(fromCard: String, toCardNumber: String, amount: Double): TransferResponse {
        return try {
            val client = Socket("127.0.0.1", 9999)
            val output = ObjectOutputStream(client.getOutputStream())
            val input = ObjectInputStream(client.getInputStream())

            val transferData = buildJsonObject {
                put("userId", Session.userId as Int)
                put("fromCard", fromCard)
                put("toCardNumber", toCardNumber)
                put("amount", amount)
            }
            val request = Request("EXTERNAL_TRANSFER", transferData)

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
