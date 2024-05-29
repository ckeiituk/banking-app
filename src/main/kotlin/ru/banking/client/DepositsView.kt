package ru.banking.client

import tornadofx.*
import java.net.Socket
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.SimpleIntegerProperty
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import ru.banking.models.Request

class DepositsView : View("Deposits") {
    private val depositsList = mutableListOf<DepositModel>().asObservable()
    private val selectedDeposit = DepositModel().toProperty()

    override val root = vbox(10) {
        label("Your Deposits")
        tableview(depositsList) {
            column("ID", DepositModel::idProperty)
            column("Amount", DepositModel::amountProperty)
            column("Start Date", DepositModel::startDateProperty)
            column("End Date", DepositModel::endDateProperty)
            column("Status", DepositModel::statusProperty)
            bindSelected(selectedDeposit)
        }
        hbox(10) {
            button("Open Deposit") {
                action {
                    openDepositDialog()
                }
            }
            button("Close Deposit") {
                enableWhen(selectedDeposit.isNotNull)
                action {
                    selectedDeposit.value?.let {
                        closeDeposit(it.id, it.status == "active")
                    }
                }
            }
            button("Refresh") {
                action {
                    populateDeposits()
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
        populateDeposits()
    }

    fun populateDeposits() {
        runAsync {
            fetchDeposits()
        } ui { deposits ->
            depositsList.setAll(deposits)
        }
    }

    private fun fetchDeposits(): List<DepositModel> {
        val userId = Session.userId ?: return emptyList()
        return try {
            val client = Socket("127.0.0.1", 9999)
            val output = client.getOutputStream().bufferedWriter()
            val input = client.getInputStream().bufferedReader()

            val requestData = buildJsonObject {
                put("userId", userId)
            }
            val request = Request("GET_DEPOSITS", requestData)
            output.write(Json.encodeToString(request))
            output.newLine()
            output.flush()

            val response = input.readLine()
            client.close()

            val jsonResponse = Json.decodeFromString<JsonObject>(response)
            val deposits = jsonResponse["deposits"]?.jsonArray

            deposits?.map {
                val jsonObject = it.jsonObject
                DepositModel(
                    id = jsonObject["id"]?.jsonPrimitive?.int ?: 0,
                    amount = jsonObject["amount"]?.jsonPrimitive?.double ?: 0.0,
                    startDate = jsonObject["startDate"]?.jsonPrimitive?.content ?: "",
                    endDate = jsonObject["endDate"]?.jsonPrimitive?.content ?: "",
                    status = jsonObject["status"]?.jsonPrimitive?.content ?: ""
                )
            } ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun openDepositDialog() {
        val dialog = find<OpenDepositDialog>()
        dialog.openModal(block = true)
    }

    private fun closeDeposit(depositId: Int, isEarlyClosure: Boolean) {
        runAsync {
            closeDepositRequest(depositId, isEarlyClosure)
        } ui { response ->
            populateDeposits()
            information("Deposit Closed", response)
        }
    }

    private fun closeDepositRequest(depositId: Int, isEarlyClosure: Boolean): String {
        val userId = Session.userId ?: return "User ID not found"
        return try {
            val client = Socket("127.0.0.1", 9999)
            val output = client.getOutputStream().bufferedWriter()
            val input = client.getInputStream().bufferedReader()

            val closeData = buildJsonObject {
                put("depositId", depositId)
                put("isEarlyClosure", isEarlyClosure)
                put("userId", userId)
            }
            val request = Request("CLOSE_DEPOSIT", closeData)

            output.write(Json.encodeToString(request))
            output.newLine()
            output.flush()

            val response = input.readLine()
            client.close()
            response
        } catch (e: Exception) {
            e.printStackTrace()
            "Error: Unable to connect to server"
        }
    }

    class DepositModel(
        id: Int = 0,
        amount: Double = 0.0,
        startDate: String = "",
        endDate: String = "",
        status: String = ""
    ) {
        val idProperty = SimpleIntegerProperty(id)
        var id by idProperty

        val amountProperty = SimpleDoubleProperty(amount)
        var amount by amountProperty

        val startDateProperty = SimpleStringProperty(startDate)
        var startDate by startDateProperty

        val endDateProperty = SimpleStringProperty(endDate)
        var endDate by endDateProperty

        val statusProperty = SimpleStringProperty(status)
        var status by statusProperty
    }
}
