package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import com.example.data.model.AppLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserPreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("arifa_ai_preferences", Context.MODE_PRIVATE)

    private val _language = MutableStateFlow(loadLanguage())
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    private val _voicePitch = MutableStateFlow(prefs.getFloat(KEY_VOICE_PITCH, 1.15f))
    val voicePitch: StateFlow<Float> = _voicePitch.asStateFlow()

    private val _voiceSpeed = MutableStateFlow(prefs.getFloat(KEY_VOICE_SPEED, 1.0f))
    val voiceSpeed: StateFlow<Float> = _voiceSpeed.asStateFlow()

    private val _edgeLightingEnabled = MutableStateFlow(prefs.getBoolean(KEY_EDGE_LIGHTING, true))
    val edgeLightingEnabled: StateFlow<Boolean> = _edgeLightingEnabled.asStateFlow()

    private val _gyroscopeEnabled = MutableStateFlow(prefs.getBoolean(KEY_GYROSCOPE, true))
    val gyroscopeEnabled: StateFlow<Boolean> = _gyroscopeEnabled.asStateFlow()

    private val _wakeWordEnabled = MutableStateFlow(prefs.getBoolean(KEY_WAKE_WORD, false))
    val wakeWordEnabled: StateFlow<Boolean> = _wakeWordEnabled.asStateFlow()

    private val _overlayEnabled = MutableStateFlow(prefs.getBoolean(KEY_OVERLAY, false))
    val overlayEnabled: StateFlow<Boolean> = _overlayEnabled.asStateFlow()

    private val _backendApiUrl = MutableStateFlow(loadBackendApiUrl())
    val backendApiUrl: StateFlow<String> = _backendApiUrl.asStateFlow()

    private fun loadBackendApiUrl(): String {
        val saved = prefs.getString(KEY_BACKEND_API_URL, null)?.trim().orEmpty()
        // Clear legacy emulator-only or placeholder URLs so physical phones never attempt 10.0.2.2
        if (saved.isBlank() || saved in LEGACY_UNREACHABLE_URLS) {
            if (saved in LEGACY_UNREACHABLE_URLS) {
                prefs.edit().remove(KEY_BACKEND_API_URL).apply()
            }
            return DEFAULT_BACKEND_URL
        }
        return saved
    }

    private fun loadLanguage(): AppLanguage {
        val code = prefs.getString(KEY_LANGUAGE, "bn") ?: "bn"
        return if (code == "en") AppLanguage.ENGLISH else AppLanguage.BENGALI
    }

    fun setLanguage(lang: AppLanguage) {
        prefs.edit().putString(KEY_LANGUAGE, lang.code).apply()
        _language.value = lang
    }

    fun setVoicePitch(pitch: Float) {
        prefs.edit().putFloat(KEY_VOICE_PITCH, pitch).apply()
        _voicePitch.value = pitch
    }

    fun setVoiceSpeed(speed: Float) {
        prefs.edit().putFloat(KEY_VOICE_SPEED, speed).apply()
        _voiceSpeed.value = speed
    }

    fun setEdgeLighting(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_EDGE_LIGHTING, enabled).apply()
        _edgeLightingEnabled.value = enabled
    }

    fun setGyroscope(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_GYROSCOPE, enabled).apply()
        _gyroscopeEnabled.value = enabled
    }

    fun setWakeWord(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_WAKE_WORD, enabled).apply()
        _wakeWordEnabled.value = enabled
    }

    fun setOverlay(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_OVERLAY, enabled).apply()
        _overlayEnabled.value = enabled
    }

    fun setBackendApiUrl(url: String) {
        val trimmed = url.trim()
        prefs.edit().putString(KEY_BACKEND_API_URL, trimmed).apply()
        _backendApiUrl.value = trimmed
    }

    companion object {
        val DEFAULT_BACKEND_URL: String = BuildConfig.BACKEND_BASE_URL.trim()

        private val LEGACY_UNREACHABLE_URLS = setOf(
            "http://10.0.2.2:3000",
            "http://10.0.2.2:3000/",
            "https://arifa-ai-backend.example.com",
            "https://arifa-ai-backend.example.com/"
        )

        private const val KEY_LANGUAGE = "key_app_language"
        private const val KEY_VOICE_PITCH = "key_voice_pitch"
        private const val KEY_VOICE_SPEED = "key_voice_speed"
        private const val KEY_EDGE_LIGHTING = "key_edge_lighting"
        private const val KEY_GYROSCOPE = "key_gyroscope_enabled"
        private const val KEY_WAKE_WORD = "key_wake_word_enabled"
        private const val KEY_OVERLAY = "key_overlay_enabled"
        private const val KEY_BACKEND_API_URL = "key_backend_api_url"
    }
}
