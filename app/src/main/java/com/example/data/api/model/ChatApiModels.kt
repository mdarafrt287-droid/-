package com.example.data.api.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ChatHistoryItem(
    @Json(name = "role") val role: String,
    @Json(name = "text") val text: String
)

@JsonClass(generateAdapter = true)
data class ChatApiRequest(
    @Json(name = "message") val message: String,
    @Json(name = "language") val language: String = "bn",
    @Json(name = "history") val history: List<ChatHistoryItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ChatApiResponse(
    @Json(name = "reply") val reply: String? = null,
    @Json(name = "model") val model: String? = null,
    @Json(name = "timestamp") val timestamp: Long? = null,
    @Json(name = "error") val error: String? = null,
    @Json(name = "message") val message: String? = null
)

@JsonClass(generateAdapter = true)
data class HealthResponse(
    @Json(name = "status") val status: String,
    @Json(name = "service") val service: String? = null,
    @Json(name = "keyConfigured") val keyConfigured: Boolean? = false,
    @Json(name = "timestamp") val timestamp: Long? = null
)
