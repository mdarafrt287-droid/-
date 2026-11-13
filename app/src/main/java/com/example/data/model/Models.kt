package com.example.data.model

enum class ChatRole {
    USER,
    ASSISTANT,
    SYSTEM
}

enum class AppLanguage(val code: String, val displayName: String, val nativeName: String) {
    BENGALI("bn", "Bengali", "বাংলা"),
    ENGLISH("en", "English", "English")
}

enum class VoiceState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING,
    ERROR
}

data class QuickActionItem(
    val id: String,
    val titleBn: String,
    val titleEn: String,
    val descBn: String,
    val descEn: String,
    val iconName: String,
    val route: String
)

data class GeneratedImageItem(
    val id: String,
    val prompt: String,
    val style: String,
    val timestamp: Long,
    val drawableResId: Int? = null,
    val localUri: String? = null
)
