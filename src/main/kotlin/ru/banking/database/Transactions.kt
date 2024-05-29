package ru.banking.database

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.datetime

object Transactions : IntIdTable() {
    val fromCard: Column<String> = varchar("from_card", 16)
    val toCard: Column<String> = varchar("to_card", 16)
    val amount: Column<Double> = double("amount")
    val transactionDate: Column<java.time.LocalDateTime> = datetime("transaction_date")
    val status: Column<String> = varchar("status", 50)
}
