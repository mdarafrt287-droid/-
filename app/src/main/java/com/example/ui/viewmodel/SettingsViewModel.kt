package com.example.ui.viewmodel

import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ArifaApplication
import com.example.data.model.AppLanguage
import com.example.service.ArifaOverlayService
import com.example.service.ArifaWakeWordService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val app: ArifaApplication = ArifaApplication.instance
) : ViewModel() {

    private val prefs = app.preferencesManager

    val language = prefs.language
    val voicePitch = prefs.voicePitch
    val voiceSpeed = prefs.voiceSpeed
    val edgeLightingEnabled = prefs.edgeLightingEnabled
    val gyroscopeEnabled = prefs.gyroscopeEnabled
    val wakeWordEnabled = prefs.wakeWordEnabled
    val overlayEnabled = prefs.overlayEnabled
    val backendApiUrl = prefs.backendApiUrl

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    fun setLanguage(lang: AppLanguage) {
        prefs.setLanguage(lang)
    }

    fun setVoicePitch(pitch: Float) {
        prefs.setVoicePitch(pitch)
    }

    fun setVoiceSpeed(speed: Float) {
        prefs.setVoiceSpeed(speed)
    }

    fun setEdgeLighting(enabled: Boolean) {
        prefs.setEdgeLighting(enabled)
    }

    fun setGyroscope(enabled: Boolean) {
        prefs.setGyroscope(enabled)
    }

    fun setWakeWord(enabled: Boolean) {
        prefs.setWakeWord(enabled)
        if (enabled) {
            ArifaWakeWordService.start(app)
            _snackbarMessage.value = if (language.value == AppLanguage.BENGALI) {
                "ভয়েস ওয়েক-ওয়ার্ড সার্ভিস চালু হয়েছে"
            } else {
                "Wake-word service active"
            }
        } else {
            ArifaWakeWordService.stop(app)
            _snackbarMessage.value = if (language.value == AppLanguage.BENGALI) {
                "ভয়েস ওয়েক-ওয়ার্ড সার্ভিস বন্ধ করা হয়েছে"
            } else {
                "Wake-word service stopped"
            }
        }
    }

    fun setOverlay(enabled: Boolean, context: Context) {
        if (enabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                _snackbarMessage.value = if (language.value == AppLanguage.BENGALI) {
                    "দয়া করে 'Display over other apps' পারমিশন দিন"
                } else {
                    "Please grant 'Display over other apps' permission"
                }
                return
            }
            prefs.setOverlay(true)
            ArifaOverlayService.start(context)
        } else {
            prefs.setOverlay(false)
            ArifaOverlayService.stop(context)
        }
    }

    fun setBackendApiUrl(url: String) {
        prefs.setBackendApiUrl(url)
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            app.chatRepository.clearAllHistory()
            _snackbarMessage.value = if (language.value == AppLanguage.BENGALI) {
                "সকল চ্যাট হিস্ট্রি মুছে ফেলা হয়েছে"
            } else {
                "All chat history cleared"
            }
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel() as T
            }
        }
    }
}
