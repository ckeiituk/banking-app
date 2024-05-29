package ru.banking.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class Request(val type: String, val data: JsonObject)
