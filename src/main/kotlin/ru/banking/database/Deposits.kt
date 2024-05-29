package ru.banking.database

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.date

object Deposits : IntIdTable() {
    val userId = reference("user_id", Users)
    val amount = decimal("amount", 14, 2)
    val startDate = date("start_date")
    val endDate = date("end_date")
    val status = varchar("status", 50).default("active")
}