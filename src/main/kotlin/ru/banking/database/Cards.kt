package ru.banking.database

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.datetime

object Cards : IntIdTable() {
    val userId: Column<Int> = integer("user_id").references(Users.id)
    val cardNumber: Column<String> = varchar("card_number", 16).uniqueIndex()
    val expirationDate: Column<java.time.LocalDateTime> = datetime("expiration_date")
    val cvv: Column<Int> = integer("cvv")
    val balance: Column<Double> = double("balance").default(0.00)
}
