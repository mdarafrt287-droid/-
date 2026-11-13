package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ArifaApplication
import com.example.data.model.AppLanguage
import com.example.data.model.VoiceState
import com.example.data.repository.VoiceManager
import com.example.data.repository.VoiceProfileInfo
import com.example.service.ArifaWakeWordService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VoiceViewModel(
    private val app: ArifaApplication = ArifaApplication.instance
) : ViewModel() {

    val voiceManager = VoiceManager(app, viewModelScope)
    val language = app.preferencesManager.language
    val wakeWordEnabled = app.preferencesManager.wakeWordEnabled

    val voiceState: StateFlow<VoiceState> = voiceManager.voiceState
    val audioAmplitude: StateFlow<Float> = voiceManager.audioAmplitude
    val recognizedText: StateFlow<String> = voiceManager.recognizedText
    val ttsStatusMessage: StateFlow<String?> = voiceManager.ttsStatusMessage
    val voiceProfileInfo: StateFlow<VoiceProfileInfo?> = voiceManager.voiceProfileInfo

    private val _lastAiResponse = MutableStateFlow<String?>(null)
    val lastAiResponse: StateFlow<String?> = _lastAiResponse.asStateFlow()

    private val _backendErrorMessage = MutableStateFlow<String?>(null)
    private val _permissionDenied = MutableStateFlow(false)
    val permissionDenied: StateFlow<Boolean> = _permissionDenied.asStateFlow()

    val errorMessage: StateFlow<String?> = combine(
        voiceManager.voiceErrorMessage,
        _backendErrorMessage
    ) { voiceErr, backendErr ->
        backendErr ?: voiceErr
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private var lastSpokenQuery: String? = null

    init {
        voiceManager.onSpeechResult = { query ->
            handleSpokenQuery(query)
        }
    }

    fun handleSpokenQuery(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return

        lastSpokenQuery = trimmed
        _backendErrorMessage.value = null
        _permissionDenied.value = false
        voiceManager.setVoiceState(VoiceState.THINKING)

        viewModelScope.launch {
            val currentLang = language.value
            val result = app.chatRepository.sendMessage(trimmed, currentLang)
            result.onSuccess { reply ->
                _lastAiResponse.value = reply
                _backendErrorMessage.value = null
                val pitch = app.preferencesManager.voicePitch.value
                val speed = app.preferencesManager.voiceSpeed.value
                voiceManager.speak(reply, currentLang, pitch, speed)
            }.onFailure { err ->
                _backendErrorMessage.value = err.localizedMessage ?: if (currentLang == AppLanguage.BENGALI) {
                    "Gemini সার্ভার থেকে উত্তর পাওয়া যায়নি।"
                } else {
                    "Failed to receive response from Gemini server."
                }
                voiceManager.setVoiceState(VoiceState.ERROR)
            }
        }
    }

    fun toggleListening() {
        _permissionDenied.value = false
        _backendErrorMessage.value = null
        when (voiceState.value) {
            VoiceState.LISTENING -> voiceManager.stopListening()
            VoiceState.SPEAKING -> voiceManager.stopSpeaking()
            VoiceState.THINKING -> { /* Wait for in-flight Gemini call */ }
            VoiceState.IDLE, VoiceState.ERROR -> voiceManager.startListening(language.value)
        }
    }

    fun stopSpeaking() {
        voiceManager.stopSpeaking()
    }

    fun stopListening() {
        voiceManager.stopListening()
    }

    fun replayResponse() {
        val last = _lastAiResponse.value ?: return
        val pitch = app.preferencesManager.voicePitch.value
        val speed = app.preferencesManager.voiceSpeed.value
        voiceManager.speak(last, language.value, pitch, speed)
    }

    fun retryLastQuery() {
        val query = lastSpokenQuery ?: return
        handleSpokenQuery(query)
    }

    fun onPermissionDenied() {
        _permissionDenied.value = true
        val msg = if (language.value == AppLanguage.BENGALI) {
            "ভয়েস ইনপুট ব্যবহার করতে মাইক্রোফোন অনুমতি প্রয়োজন। অনুগ্রহ করে অনুমতি দিন অথবা অ্যাপ সেটিংস থেকে চালু করুন।"
        } else {
            "Microphone permission is required for voice input. Please grant permission or enable it in App Settings."
        }
        voiceManager.setVoiceError(msg)
    }

    fun clearError() {
        _backendErrorMessage.value = null
        _permissionDenied.value = false
        voiceManager.clearError()
    }

    fun toggleWakeWord(enabled: Boolean) {
        app.preferencesManager.setWakeWord(enabled)
        if (enabled) {
            ArifaWakeWordService.start(app)
        } else {
            ArifaWakeWordService.stop(app)
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.destroy()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return VoiceViewModel() as T
            }
        }
    }
}
