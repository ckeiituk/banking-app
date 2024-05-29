package ru.banking.database

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.datetime

object Accounts : IntIdTable() {
    val userId = reference("user_id", Users)
    val createdDate = datetime("created_date")
}
