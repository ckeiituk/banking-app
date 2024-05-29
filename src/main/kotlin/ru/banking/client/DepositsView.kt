package ru.banking.client

import tornadofx.*
import java.net.Socket
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import ru.banking.models.Request
import java.math.BigDecimal
import java.time.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class DepositsView : View("Deposits") {
    private val amountField = textfield()
    private val startDateField = datepicker()
    private val endDateField = datepicker()
    private val messageLabel = label()

    override val root = form {
        fieldset("View and manage your deposits") {
            field("Amount") {
                add(amountField)
            }
            field("Start Date") {
                add(startDateField)
            }
            field("End Date") {
                add(endDateField)
            }
        }
        button("Add Deposit") {
            action {
                val amount = BigDecimal(amountField.text)
                val startDate = startDateField.value
                val endDate = endDateField.value
                val response = addDeposit(amount, startDate, endDate)
                messageLabel.text = response
            }
        }
        add(messageLabel)
        button("Back") {
            action {
                replaceWith<DashboardView>()
            }
        }
    }

    private fun addDeposit(amount: BigDecimal, startDate: LocalDate, endDate: LocalDate): String {
        val userId = Session.userId ?: return "User ID not found"
        return try {
            val client = Socket("127.0.0.1", 9999)
            val output = ObjectOutputStream(client.getOutputStream())
            val input = ObjectInputStream(client.getInputStream())

            val depositData = buildJsonObject {
                put("userId", userId)
                put("amount", amount.toString())
                put("startDate", startDate.toString())
                put("endDate", endDate.toString())
            }
            val request = Request("ADD_DEPOSIT", depositData)

            output.writeObject(Json.encodeToString(Request.serializer(), request))
            output.flush()

            val response = input.readObject() as String

            client.close()
            response
        } catch (e: Exception) {
            e.printStackTrace()
            "Error: Unable to connect to server"
        }
    }
}
