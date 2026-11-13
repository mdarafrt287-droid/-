package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.ui.components.ArifaBottomNav
import com.example.ui.components.ArifaTopBar
import com.example.ui.components.NeonEdgeLighting
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ImageGenScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VoiceScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.VoidGradientBackground
import com.example.ui.viewmodel.ChatViewModel
import com.example.ui.viewmodel.ImageGenViewModel
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.SettingsViewModel
import com.example.ui.viewmodel.VoiceViewModel

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels { MainViewModel.Factory }
    private val chatViewModel: ChatViewModel by viewModels { ChatViewModel.Factory }
    private val voiceViewModel: VoiceViewModel by viewModels { VoiceViewModel.Factory }
    private val imageGenViewModel: ImageGenViewModel by viewModels { ImageGenViewModel.Factory }
    private val settingsViewModel: SettingsViewModel by viewModels { SettingsViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Configure Firebase App Check with launch intent before any UI or Firebase calls
        configureAppCheck(this, intent)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                ArifaMainApp(
                    mainViewModel = mainViewModel,
                    chatViewModel = chatViewModel,
                    voiceViewModel = voiceViewModel,
                    imageGenViewModel = imageGenViewModel,
                    settingsViewModel = settingsViewModel
                )
            }
        }
    }
}

@Composable
fun ArifaMainApp(
    mainViewModel: MainViewModel,
    chatViewModel: ChatViewModel,
    voiceViewModel: VoiceViewModel,
    imageGenViewModel: ImageGenViewModel,
    settingsViewModel: SettingsViewModel
) {
    val context = LocalContext.current

    val currentRoute by mainViewModel.currentRoute.collectAsState()
    val language by mainViewModel.language.collectAsState()
    val isOnline by mainViewModel.isOnline.collectAsState()
    val edgeLightingEnabled by mainViewModel.edgeLightingEnabled.collectAsState()
    val gyroscopeEnabled by mainViewModel.gyroscopeEnabled.collectAsState()
    val gyroOffset by mainViewModel.gyroOffset.collectAsState()

    // Back button returns to Home screen if on secondary screen
    if (currentRoute != "home") {
        BackHandler {
            mainViewModel.navigateTo("home")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidGradientBackground)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            topBar = {
                ArifaTopBar(
                    isOnline = isOnline,
                    currentLanguage = language,
                    onLanguageToggle = { newLang ->
                        mainViewModel.toggleLanguage(newLang)
                    }
                )
            },
            bottomBar = {
                ArifaBottomNav(
                    currentRoute = currentRoute,
                    currentLanguage = language,
                    onNavigate = { route ->
                        mainViewModel.navigateTo(route)
                    }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                AnimatedContent(
                    targetState = currentRoute,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "screen_transition"
                ) { route ->
                    when (route) {
                        "home" -> {
                            HomeScreen(
                                language = language,
                                isOnline = isOnline,
                                gyroOffset = gyroOffset,
                                gyroEnabled = gyroscopeEnabled,
                                onNavigate = { dest -> mainViewModel.navigateTo(dest) },
                                onQuickPrompt = { prompt ->
                                    chatViewModel.onInputTextChanged(prompt)
                                }
                            )
                        }

                        "chat" -> {
                            val messages by chatViewModel.messages.collectAsState()
                            val inputText by chatViewModel.inputText.collectAsState()
                            val voiceState by chatViewModel.voiceState.collectAsState()
                            val partialVoiceText by chatViewModel.partialVoiceText.collectAsState()
                            val isSending by chatViewModel.isSending.collectAsState()
                            val errorMessage by chatViewModel.errorMessage.collectAsState()
                            val permissionDenied by chatViewModel.permissionDenied.collectAsState()

                            ChatScreen(
                                messages = messages,
                                inputText = inputText,
                                isSending = isSending,
                                language = language,
                                voiceState = voiceState,
                                partialVoiceText = partialVoiceText,
                                errorMessage = errorMessage,
                                permissionDenied = permissionDenied,
                                onInputTextChanged = chatViewModel::onInputTextChanged,
                                onSendMessage = { prompt -> chatViewModel.sendMessage(prompt) },
                                onSpeakMessage = chatViewModel::speakMessage,
                                onStopSpeaking = chatViewModel::stopSpeaking,
                                onToggleVoice = chatViewModel::toggleVoiceInput,
                                onClearChat = chatViewModel::clearMessages,
                                onRetry = chatViewModel::retryLastMessage,
                                onPermissionDenied = chatViewModel::onPermissionDenied,
                                onDismissError = chatViewModel::clearError
                            )
                        }

                        "voice" -> {
                            val voiceState by voiceViewModel.voiceState.collectAsState()
                            val audioAmplitude by voiceViewModel.audioAmplitude.collectAsState()
                            val recognizedText by voiceViewModel.recognizedText.collectAsState()
                            val lastAiResponse by voiceViewModel.lastAiResponse.collectAsState()
                            val wakeWordEnabled by voiceViewModel.wakeWordEnabled.collectAsState()
                            val errorMessage by voiceViewModel.errorMessage.collectAsState()
                            val permissionDenied by voiceViewModel.permissionDenied.collectAsState()
                            val ttsStatusMessage by voiceViewModel.ttsStatusMessage.collectAsState()
                            val voiceProfileInfo by voiceViewModel.voiceProfileInfo.collectAsState()

                            VoiceScreen(
                                language = language,
                                voiceState = voiceState,
                                audioAmplitude = audioAmplitude,
                                recognizedText = recognizedText,
                                lastAiResponse = lastAiResponse,
                                wakeWordEnabled = wakeWordEnabled,
                                errorMessage = errorMessage,
                                permissionDenied = permissionDenied,
                                ttsStatusMessage = ttsStatusMessage,
                                voiceProfileInfo = voiceProfileInfo,
                                onToggleListening = voiceViewModel::toggleListening,
                                onStopSpeaking = voiceViewModel::stopSpeaking,
                                onStopListening = voiceViewModel::stopListening,
                                onReplay = voiceViewModel::replayResponse,
                                onRetryQuery = voiceViewModel::retryLastQuery,
                                onPermissionDenied = voiceViewModel::onPermissionDenied,
                                onDismissError = voiceViewModel::clearError,
                                onToggleWakeWord = voiceViewModel::toggleWakeWord
                            )
                        }

                        "image_gen" -> {
                            val promptInput by imageGenViewModel.promptInput.collectAsState()
                            val selectedStyle by imageGenViewModel.selectedStyle.collectAsState()
                            val selectedAspectRatio by imageGenViewModel.selectedAspectRatio.collectAsState()
                            val isGenerating by imageGenViewModel.isGenerating.collectAsState()
                            val successMessage by imageGenViewModel.generationSuccessMessage.collectAsState()
                            val gallery by imageGenViewModel.gallery.collectAsState()

                            ImageGenScreen(
                                language = language,
                                promptInput = promptInput,
                                selectedStyle = selectedStyle,
                                selectedAspectRatio = selectedAspectRatio,
                                isGenerating = isGenerating,
                                successMessage = successMessage,
                                gallery = gallery,
                                onPromptChanged = imageGenViewModel::onPromptChanged,
                                onStyleSelected = imageGenViewModel::selectStyle,
                                onAspectRatioSelected = imageGenViewModel::selectAspectRatio,
                                onGenerate = imageGenViewModel::generateImage,
                                onClearSuccess = imageGenViewModel::clearSuccessMessage
                            )
                        }

                        "history" -> {
                            val messages by chatViewModel.messages.collectAsState()
                            HistoryScreen(
                                messages = messages,
                                language = language,
                                onNavigateToChat = { mainViewModel.navigateTo("chat") },
                                onClearAll = chatViewModel::clearMessages
                            )
                        }

                        "settings" -> {
                            val voicePitch by settingsViewModel.voicePitch.collectAsState()
                            val voiceSpeed by settingsViewModel.voiceSpeed.collectAsState()
                            val edgeLighting by settingsViewModel.edgeLightingEnabled.collectAsState()
                            val gyroEnabled by settingsViewModel.gyroscopeEnabled.collectAsState()
                            val wakeWord by settingsViewModel.wakeWordEnabled.collectAsState()
                            val overlay by settingsViewModel.overlayEnabled.collectAsState()
                            val backendApiUrl by settingsViewModel.backendApiUrl.collectAsState()
                            val snackbarMessage by settingsViewModel.snackbarMessage.collectAsState()

                            SettingsScreen(
                                language = language,
                                voicePitch = voicePitch,
                                voiceSpeed = voiceSpeed,
                                edgeLightingEnabled = edgeLighting,
                                gyroscopeEnabled = gyroEnabled,
                                wakeWordEnabled = wakeWord,
                                overlayEnabled = overlay,
                                backendApiUrl = backendApiUrl,
                                snackbarMessage = snackbarMessage,
                                onLanguageChange = settingsViewModel::setLanguage,
                                onVoicePitchChange = settingsViewModel::setVoicePitch,
                                onVoiceSpeedChange = settingsViewModel::setVoiceSpeed,
                                onEdgeLightingChange = settingsViewModel::setEdgeLighting,
                                onGyroscopeChange = settingsViewModel::setGyroscope,
                                onWakeWordChange = settingsViewModel::setWakeWord,
                                onOverlayChange = { enabled ->
                                    settingsViewModel.setOverlay(enabled, context)
                                },
                                onBackendApiUrlChange = settingsViewModel::setBackendApiUrl,
                                onClearChatHistory = settingsViewModel::clearChatHistory,
                                onTestVoice = {
                                    val testSentence = if (language == com.example.data.model.AppLanguage.BENGALI) {
                                        "নমস্কার! আমি আরিফা, আপনার বাংলা এআই সহকারী।"
                                    } else {
                                        "Hello! I am ARIFA, your futuristic AI assistant."
                                    }
                                    chatViewModel.speakMessage(testSentence)
                                },
                                onClearSnackbar = settingsViewModel::clearSnackbar
                            )
                        }
                    }
                }
            }
        }

        // Animated neon edge perimeter glow over entire screen when enabled
        NeonEdgeLighting(
            enabled = edgeLightingEnabled,
            strokeWidthDp = 2.5f
        )
    }
}
