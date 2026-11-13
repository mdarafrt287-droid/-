package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ArifaApplication
import com.example.data.local.ChatMessageEntity
import com.example.data.model.AppLanguage
import com.example.data.model.VoiceState
import com.example.data.repository.VoiceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(
    private val app: ArifaApplication = ArifaApplication.instance
) : ViewModel() {

    private val chatRepo = app.chatRepository
    val voiceManager = VoiceManager(app, viewModelScope)

    val messages: StateFlow<List<ChatMessageEntity>> = chatRepo.getMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val language = app.preferencesManager.language

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _backendErrorMessage = MutableStateFlow<String?>(null)
    private val _permissionDenied = MutableStateFlow(false)
    val permissionDenied: StateFlow<Boolean> = _permissionDenied.asStateFlow()

    val errorMessage: StateFlow<String?> = combine(
        _backendErrorMessage,
        voiceManager.voiceErrorMessage
    ) { backendErr, voiceErr ->
        backendErr ?: voiceErr
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private var lastFailedPrompt: String? = null
    private var speakNextResponse: Boolean = false

    val voiceState: StateFlow<VoiceState> = voiceManager.voiceState
    val audioAmplitude: StateFlow<Float> = voiceManager.audioAmplitude
    val partialVoiceText: StateFlow<String> = voiceManager.recognizedText

    init {
        voiceManager.onSpeechResult = { recognized ->
            _inputText.value = recognized
            speakNextResponse = true
            sendMessage(recognized, autoSpeakResponse = true)
        }
    }

    fun onInputTextChanged(newText: String) {
        _inputText.value = newText
    }

    fun clearError() {
        _backendErrorMessage.value = null
        _permissionDenied.value = false
        voiceManager.clearError()
    }

    fun onPermissionDenied() {
        _permissionDenied.value = true
        val msg = if (language.value == AppLanguage.BENGALI) {
            "ভয়েস ইনপুটের জন্য মাইক্রোফোন অনুমতি প্রয়োজন।"
        } else {
            "Microphone permission is required for voice input."
        }
        voiceManager.setVoiceError(msg)
    }

    fun retryLastMessage() {
        val prompt = lastFailedPrompt
        if (!prompt.isNullOrBlank()) {
            _backendErrorMessage.value = null
            sendMessage(prompt, autoSpeakResponse = speakNextResponse)
        }
    }

    fun sendMessage(promptText: String? = null, autoSpeakResponse: Boolean = false) {
        val textToSend = (promptText ?: _inputText.value).trim()
        if (textToSend.isBlank() || _isSending.value) return

        _inputText.value = ""
        _backendErrorMessage.value = null
        _permissionDenied.value = false
        _isSending.value = true
        lastFailedPrompt = textToSend
        speakNextResponse = autoSpeakResponse

        if (autoSpeakResponse) {
            voiceManager.setVoiceState(VoiceState.THINKING)
        }

        viewModelScope.launch {
            val currentLang = language.value
            val result = chatRepo.sendMessage(textToSend, currentLang)
            _isSending.value = false

            result.onSuccess { reply ->
                _backendErrorMessage.value = null
                lastFailedPrompt = null
                if (autoSpeakResponse) {
                    speakMessage(reply)
                }
            }.onFailure { err ->
                _backendErrorMessage.value = err.localizedMessage ?: "Failed to get response"
                if (autoSpeakResponse) {
                    voiceManager.setVoiceState(VoiceState.ERROR)
                }
            }
        }
    }

    fun speakMessage(content: String) {
        val currentLang = language.value
        val pitch = app.preferencesManager.voicePitch.value
        val speed = app.preferencesManager.voiceSpeed.value
        voiceManager.speak(content, currentLang, pitch, speed)
    }

    fun stopSpeaking() {
        voiceManager.stopSpeaking()
    }

    fun toggleVoiceInput() {
        _permissionDenied.value = false
        _backendErrorMessage.value = null
        when (voiceState.value) {
            VoiceState.LISTENING -> voiceManager.stopListening()
            VoiceState.SPEAKING -> voiceManager.stopSpeaking()
            VoiceState.THINKING -> { /* Wait for response */ }
            VoiceState.IDLE, VoiceState.ERROR -> voiceManager.startListening(language.value)
        }
    }

    fun clearMessages() {
        viewModelScope.launch {
            chatRepo.clearHistory()
            _backendErrorMessage.value = null
            lastFailedPrompt = null
            voiceManager.stopSpeaking()
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
                return ChatViewModel() as T
            }
        }
    }
}
