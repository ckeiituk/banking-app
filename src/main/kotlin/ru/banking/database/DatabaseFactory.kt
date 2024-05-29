package ru.banking.database

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    fun init() {
        val url = "jdbc:mysql://147.78.67.31:3306/banking"
        val driver = "com.mysql.cj.jdbc.Driver"
        val user = "meur"
        val password = "meur"

        Database.connect(url, driver, user, password)

        transaction {
            SchemaUtils.create(Users)
            SchemaUtils.create(Cards)
            SchemaUtils.create(Deposits)
            SchemaUtils.create(Transactions)
        }
    }
}
