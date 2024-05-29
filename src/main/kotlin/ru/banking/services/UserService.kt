package ru.banking.services

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import ru.banking.database.Users
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

object UserService {
    fun authenticate(data: JsonObject): Map<String, Any> {
        val email = (data["email"] as JsonPrimitive).content
        val password = (data["password"] as JsonPrimitive).content

        val user = transaction {
            Users.select { Users.email eq email }.singleOrNull()
        } ?: return mapOf("status" to "error", "message" to "User not found")

        return if (BCrypt.checkpw(password, user[Users.passwordHash])) {
            mapOf("status" to "success", "message" to "Authenticated successfully", "userId" to user[Users.id].value)
        } else {
            mapOf("status" to "error", "message" to "Invalid password")
        }
    }

    fun register(data: JsonObject): Map<String, Any> {
        val name = (data["name"] as JsonPrimitive).content
        val email = (data["email"] as JsonPrimitive).content
        val password = (data["password"] as JsonPrimitive).content

        val passwordHash = BCrypt.hashpw(password, BCrypt.gensalt())

        transaction {
            Users.insert {
                it[Users.name] = name
                it[Users.email] = email
                it[Users.passwordHash] = passwordHash
            }
        }

        return mapOf("status" to "success", "message" to "User registered successfully")
    }
}
