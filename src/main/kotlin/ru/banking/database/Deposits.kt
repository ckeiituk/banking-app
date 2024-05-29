package ru.banking.database

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.date

object Deposits : IntIdTable() {
    val accountId = reference("account_id", Accounts)
    val amount = decimal("amount", 14, 2)
    val startDate = date("start_date")
    val endDate = date("end_date")
}
