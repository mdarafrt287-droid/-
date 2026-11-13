package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ArifaApplication
import com.example.data.model.AppLanguage
import com.example.data.repository.GyroscopeOffset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val app: ArifaApplication = ArifaApplication.instance
) : ViewModel() {

    private val _currentRoute = MutableStateFlow("home")
    val currentRoute: StateFlow<String> = _currentRoute.asStateFlow()

    val language = app.preferencesManager.language
    val edgeLightingEnabled = app.preferencesManager.edgeLightingEnabled
    val gyroscopeEnabled = app.preferencesManager.gyroscopeEnabled
    val wakeWordEnabled = app.preferencesManager.wakeWordEnabled
    val overlayEnabled = app.preferencesManager.overlayEnabled

    val isOnline: StateFlow<Boolean> = app.networkMonitor.isOnline
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val gyroOffset: StateFlow<GyroscopeOffset> = app.sensorHelper.offset

    init {
        app.sensorHelper.startListening()
    }

    fun navigateTo(route: String) {
        _currentRoute.value = route
    }

    fun toggleLanguage(newLang: AppLanguage) {
        app.preferencesManager.setLanguage(newLang)
    }

    override fun onCleared() {
        super.onCleared()
        app.sensorHelper.stopListening()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel() as T
            }
        }
    }
}
