package com.example

import android.content.Context
import android.os.Bundle
import android.speech.SpeechRecognizer
import android.speech.tts.Voice
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ChatDao
import com.example.data.local.ChatMessageEntity
import com.example.data.model.AppLanguage
import com.example.data.model.VoiceState
import com.example.data.preferences.UserPreferencesManager
import com.example.data.repository.AiChatRepository
import com.example.data.repository.VoiceManager
import com.example.data.repository.VoiceProfileInfo
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.VoiceScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class VoiceManagerRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `selectBestTtsVoice prefers female bn-BD voice over male and bn-IN`() {
        val maleBd = Voice(
            "bn-bd-x-male-local",
            Locale.forLanguageTag("bn-BD"),
            Voice.QUALITY_HIGH,
            Voice.LATENCY_NORMAL,
            false,
            setOf("male")
        )
        val femaleIn = Voice(
            "bn-in-x-bnf-local",
            Locale.forLanguageTag("bn-IN"),
            Voice.QUALITY_HIGH,
            Voice.LATENCY_NORMAL,
            false,
            setOf("female")
        )
        val femaleBd = Voice(
            "bn-bd-x-female-local",
            Locale.forLanguageTag("bn-BD"),
            Voice.QUALITY_HIGH,
            Voice.LATENCY_NORMAL,
            false,
            setOf("female")
        )

        val selected = VoiceManager.selectBestTtsVoice(
            listOf(maleBd, femaleIn, femaleBd),
            AppLanguage.BENGALI
        )

        assertNotNull(selected)
        assertEquals("bn-bd-x-female-local", selected?.name)
        assertTrue(VoiceManager.isFemaleVoice(selected!!))
    }

    @Test
    fun `selectBestTtsVoice falls back to best available Bengali voice when no female voice`() {
        val genericBnIn = Voice(
            "bn-in-x-generic",
            Locale.forLanguageTag("bn-IN"),
            Voice.QUALITY_NORMAL,
            Voice.LATENCY_NORMAL,
            false,
            emptySet()
        )
        val genericBnBd = Voice(
            "bn-bd-x-standard",
            Locale.forLanguageTag("bn-BD"),
            Voice.QUALITY_HIGH,
            Voice.LATENCY_NORMAL,
            false,
            emptySet()
        )

        val selected = VoiceManager.selectBestTtsVoice(
            listOf(genericBnIn, genericBnBd),
            AppLanguage.BENGALI
        )

        assertNotNull(selected)
        assertEquals("bn-bd-x-standard", selected?.name)
    }

    @Test
    fun `selectBestTtsVoice returns null when no Bengali voice available`() {
        val enVoice = Voice(
            "en-us-x-sfg-local",
            Locale.US,
            Voice.QUALITY_HIGH,
            Voice.LATENCY_NORMAL,
            false,
            setOf("female")
        )

        val selected = VoiceManager.selectBestTtsVoice(listOf(enVoice), AppLanguage.BENGALI)
        assertNull(selected)

        val enSelected = VoiceManager.selectBestTtsVoice(listOf(enVoice), AppLanguage.ENGLISH)
        assertEquals("en-us-x-sfg-local", enSelected?.name)
    }

    @Test
    fun `VoiceManager handles speech recognition results and errors properly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val manager = VoiceManager(context, scope)

        try {
            var capturedResult: String? = null
            manager.onSpeechResult = { text ->
                capturedResult = text
            }

            manager.onReadyForSpeech(null)
            assertEquals(VoiceState.LISTENING, manager.voiceState.value)

            manager.onRmsChanged(5f)
            assertTrue(manager.audioAmplitude.value > 0.1f)

            val partialBundle = Bundle().apply {
                putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION, arrayListOf("আজকের"))
            }
            manager.onPartialResults(partialBundle)
            assertEquals("আজকের", manager.recognizedText.value)

            val finalBundle = Bundle().apply {
                putStringArrayList(
                    SpeechRecognizer.RESULTS_RECOGNITION,
                    arrayListOf("আজকের আবহাওয়া কেমন?")
                )
            }
            manager.onResults(finalBundle)

            assertEquals("আজকের আবহাওয়া কেমন?", capturedResult)
            assertEquals("আজকের আবহাওয়া কেমন?", manager.recognizedText.value)
            assertEquals(VoiceState.THINKING, manager.voiceState.value)
            assertNull(manager.voiceErrorMessage.value)

            // Test error handling
            manager.onError(SpeechRecognizer.ERROR_NO_MATCH)
            assertEquals(VoiceState.ERROR, manager.voiceState.value)
            assertNotNull(manager.voiceErrorMessage.value)

            manager.clearError()
            assertEquals(VoiceState.IDLE, manager.voiceState.value)
            assertNull(manager.voiceErrorMessage.value)

            // Test permission denial error mapping
            val permMsgBn = VoiceManager.mapSpeechErrorToMessage(
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS,
                AppLanguage.BENGALI
            )
            assertTrue(permMsgBn.contains("মাইক্রোফোন"))
            val permMsgEn = VoiceManager.mapSpeechErrorToMessage(
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS,
                AppLanguage.ENGLISH
            )
            assertTrue(permMsgEn.contains("Microphone", ignoreCase = true))
        } finally {
            manager.destroy()
            scope.cancel()
        }
    }

    @Test
    fun `VoiceScreen displays speaking stop button and triggers stop callback`() {
        var stopSpeakingCalled = false

        composeTestRule.setContent {
            MyApplicationTheme {
                VoiceScreen(
                    language = AppLanguage.BENGALI,
                    voiceState = VoiceState.SPEAKING,
                    audioAmplitude = 0.6f,
                    recognizedText = "হ্যালো আরিফা",
                    lastAiResponse = "হ্যালো! আমি আরিফা, আপনাকে কীভাবে সাহায্য করতে পারি?",
                    wakeWordEnabled = false,
                    voiceProfileInfo = VoiceProfileInfo(
                        voiceName = "bn-bd-x-female-local",
                        localeTag = "bn-BD",
                        isFemaleSelected = true,
                        isBengaliSupported = true
                    ),
                    onToggleListening = {},
                    onStopSpeaking = { stopSpeakingCalled = true },
                    onReplay = {},
                    onToggleWakeWord = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("voice_status_badge").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("voice_profile_badge").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("card_stop_speaking_button").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("stop_speaking_button").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("stop_speaking_button").performClick()

        assertTrue(stopSpeakingCalled)
    }

    @Test
    fun `VoiceScreen displays error banner on speech or permission error`() {
        composeTestRule.setContent {
            MyApplicationTheme {
                VoiceScreen(
                    language = AppLanguage.BENGALI,
                    voiceState = VoiceState.ERROR,
                    audioAmplitude = 0f,
                    recognizedText = "",
                    lastAiResponse = null,
                    wakeWordEnabled = false,
                    errorMessage = "মাইক্রোফোন পারমিশন দেওয়া হয়নি।",
                    permissionDenied = true,
                    onToggleListening = {},
                    onReplay = {},
                    onToggleWakeWord = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("voice_error_banner").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `ChatScreen displays voice status bar when speaking and stops playback`() {
        var stopVoiceCalled = false

        composeTestRule.setContent {
            MyApplicationTheme {
                ChatScreen(
                    messages = emptyList(),
                    inputText = "",
                    isSending = false,
                    language = AppLanguage.BENGALI,
                    voiceState = VoiceState.SPEAKING,
                    onInputTextChanged = {},
                    onSendMessage = {},
                    onSpeakMessage = {},
                    onStopSpeaking = { stopVoiceCalled = true },
                    onToggleVoice = {},
                    onClearChat = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("chat_voice_status_bar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("chat_stop_voice_button").assertIsDisplayed().performClick()
        assertTrue(stopVoiceCalled)
    }

    @Test
    fun `AiChatRepository removes emulator 10_0_2_2 default and reports clear errors when unconfigured or unreachable`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Simulate legacy 10.0.2.2 saved from prior install on a physical phone
        context.getSharedPreferences("arifa_ai_preferences", Context.MODE_PRIVATE)
            .edit()
            .putString("key_backend_api_url", "http://10.0.2.2:3000/")
            .commit()

        val prefs = UserPreferencesManager(context)
        assertEquals("", prefs.backendApiUrl.value)

        val fakeDao = object : ChatDao {
            private val items = mutableListOf<ChatMessageEntity>()
            override fun getMessagesForSession(sessionId: String): Flow<List<ChatMessageEntity>> =
                flowOf(items.filter { it.sessionId == sessionId })
            override fun getAllMessages(): Flow<List<ChatMessageEntity>> = flowOf(items)
            override fun getAllSessionIds(): Flow<List<String>> = flowOf(listOf("default_session"))
            override suspend fun insertMessage(message: ChatMessageEntity): Long {
                items.add(message)
                return items.size.toLong()
            }
            override suspend fun clearSession(sessionId: String) {
                items.removeAll { it.sessionId == sessionId }
            }
            override suspend fun clearAll() {
                items.clear()
            }
            override suspend fun getMessageCount(): Int = items.size
        }

        val repo = AiChatRepository(fakeDao, prefs)

        // 0. Default empty backendUrl uses built-in Firebase AI Logic cleanly (no 10.0.2.2 error)
        val builtInHealth = repo.checkBackendHealth()
        assertTrue("Firebase AI Logic should be ready when custom URL is blank", builtInHealth.isSuccess)

        // Verify configureAppCheck persists debug token synchronously from launch Intent
        val testIntent = android.content.Intent().apply {
            putExtra("FIREBASE_APPCHECK_DEBUG_TOKEN", "test-debug-token-1234")
        }
        configureAppCheck(context, testIntent)

        // 1. Reach custom running backend on port 3000 without any CLEARTEXT failure
        prefs.setBackendApiUrl("http://127.0.0.1:3000/")
        val healthResult = repo.checkBackendHealth()
        assertTrue("Backend on port 3000 should be reachable", healthResult.isSuccess)

        val chatResult = repo.sendMessage("কেমন আছো আরিফা?", AppLanguage.BENGALI)
        if (chatResult.isFailure) {
            val errMsg = chatResult.exceptionOrNull()?.message.orEmpty()
            assertTrue(
                "Failure should be explicit backend key notice, not CLEARTEXT: $errMsg",
                errMsg.contains("GEMINI_API_KEY") && !errMsg.contains("CLEARTEXT", ignoreCase = true)
            )
        }

        // 2. Unreachable custom backend port returns clear localized Bengali error
        prefs.setBackendApiUrl("http://127.0.0.1:59999/")
        val unreachableResult = repo.sendMessage("হ্যালো", AppLanguage.BENGALI)
        assertTrue(unreachableResult.isFailure)
        val unreachableMsg = unreachableResult.exceptionOrNull()?.message.orEmpty()
        assertTrue(
            "Expected localized connection error, got: $unreachableMsg",
            unreachableMsg.contains("সংযোগ") || unreachableMsg.contains("connect", ignoreCase = true)
        )
    }
}
