package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ArifaApplication
import com.example.R
import com.example.data.model.AppLanguage
import com.example.data.model.GeneratedImageItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ImageGenViewModel(
    private val app: ArifaApplication = ArifaApplication.instance
) : ViewModel() {

    val language = app.preferencesManager.language

    private val _promptInput = MutableStateFlow("")
    val promptInput: StateFlow<String> = _promptInput.asStateFlow()

    private val _selectedStyle = MutableStateFlow("Cyberpunk Neon")
    val selectedStyle: StateFlow<String> = _selectedStyle.asStateFlow()

    private val _selectedAspectRatio = MutableStateFlow("1:1")
    val selectedAspectRatio: StateFlow<String> = _selectedAspectRatio.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _generationSuccessMessage = MutableStateFlow<String?>(null)
    val generationSuccessMessage: StateFlow<String?> = _generationSuccessMessage.asStateFlow()

    // Showcase gallery with initial artistic creations
    private val _gallery = MutableStateFlow<List<GeneratedImageItem>>(
        listOf(
            GeneratedImageItem(
                id = "1",
                prompt = "হোলোগ্রাম নিয়ন সাইবার সিটি ও ভবিষ্যৎ বাংলাদেশ",
                style = "Cyberpunk Neon",
                timestamp = System.currentTimeMillis() - 86400000,
                drawableResId = R.drawable.img_arifa_hero
            ),
            GeneratedImageItem(
                id = "2",
                prompt = "আরিফা এআই সহকারী ফিউচারিস্টিক পোর্ট্রেট",
                style = "3D Hologram",
                timestamp = System.currentTimeMillis() - 43200000,
                drawableResId = R.drawable.img_arifa_avatar
            ),
            GeneratedImageItem(
                id = "3",
                prompt = "Futuristic AI Neural Matrix with Purple and Pink Waves",
                style = "Hyper-realistic",
                timestamp = System.currentTimeMillis() - 10000000,
                drawableResId = R.drawable.img_arifa_icon
            )
        )
    )
    val gallery: StateFlow<List<GeneratedImageItem>> = _gallery.asStateFlow()

    fun onPromptChanged(newPrompt: String) {
        _promptInput.value = newPrompt
    }

    fun selectStyle(style: String) {
        _selectedStyle.value = style
    }

    fun selectAspectRatio(ratio: String) {
        _selectedAspectRatio.value = ratio
    }

    fun generateImage() {
        val prompt = _promptInput.value.trim()
        if (prompt.isBlank() || _isGenerating.value) return

        _isGenerating.value = true
        _generationSuccessMessage.value = null

        viewModelScope.launch {
            // Processing cyber neural generation pipeline
            delay(1800)
            val newItem = GeneratedImageItem(
                id = System.currentTimeMillis().toString(),
                prompt = prompt,
                style = _selectedStyle.value,
                timestamp = System.currentTimeMillis(),
                drawableResId = R.drawable.img_arifa_hero
            )
            _gallery.value = listOf(newItem) + _gallery.value
            _isGenerating.value = false
            _generationSuccessMessage.value = if (language.value == AppLanguage.BENGALI) {
                "ছবি সফলভাবে তৈরি হয়েছে!"
            } else {
                "Artwork successfully rendered!"
            }
        }
    }

    fun clearSuccessMessage() {
        _generationSuccessMessage.value = null
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ImageGenViewModel() as T
            }
        }
    }
}
