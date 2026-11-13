package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import com.example.data.model.AppLanguage
import com.example.data.model.VoiceState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

data class VoiceProfileInfo(
    val voiceName: String,
    val localeTag: String,
    val isFemaleSelected: Boolean,
    val isBengaliSupported: Boolean
)

private data class PendingSpeakRequest(
    val text: String,
    val language: AppLanguage,
    val pitch: Float,
    val speed: Float
)

class VoiceManager(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) : RecognitionListener, TextToSpeech.OnInitListener {

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false
    private var currentLanguage: AppLanguage = AppLanguage.BENGALI
    private var pendingSpeakRequest: PendingSpeakRequest? = null

    private val _voiceState = MutableStateFlow(VoiceState.IDLE)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private val _audioAmplitude = MutableStateFlow(0f)
    val audioAmplitude: StateFlow<Float> = _audioAmplitude.asStateFlow()

    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText.asStateFlow()

    private val _voiceErrorMessage = MutableStateFlow<String?>(null)
    val voiceErrorMessage: StateFlow<String?> = _voiceErrorMessage.asStateFlow()

    private val _ttsStatusMessage = MutableStateFlow<String?>(null)
    val ttsStatusMessage: StateFlow<String?> = _ttsStatusMessage.asStateFlow()

    private val _voiceProfileInfo = MutableStateFlow<VoiceProfileInfo?>(null)
    val voiceProfileInfo: StateFlow<VoiceProfileInfo?> = _voiceProfileInfo.asStateFlow()

    var onSpeechResult: ((String) -> Unit)? = null

    init {
        initSpeechRecognizer()
        initTextToSpeech()
    }

    private fun initSpeechRecognizer() {
        coroutineScope.launch(Dispatchers.Main) {
            try {
                if (SpeechRecognizer.isRecognitionAvailable(context)) {
                    speechRecognizer?.destroy()
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                        setRecognitionListener(this@VoiceManager)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize SpeechRecognizer", e)
            }
        }
    }

    private fun initTextToSpeech() {
        try {
            textToSpeech = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create TextToSpeech", e)
            _ttsStatusMessage.value = "TTS engine initialization failed"
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsReady = true
            configureVoiceForLanguage(currentLanguage)
            textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _voiceState.value = VoiceState.SPEAKING
                }

                override fun onDone(utteranceId: String?) {
                    _voiceState.value = VoiceState.IDLE
                    _audioAmplitude.value = 0f
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _voiceState.value = VoiceState.IDLE
                    _audioAmplitude.value = 0f
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    _voiceState.value = VoiceState.ERROR
                    _audioAmplitude.value = 0f
                    _voiceErrorMessage.value = if (currentLanguage == AppLanguage.BENGALI) {
                        "ভয়েস প্লেব্যাকে সমস্যা হয়েছে (কোড: $errorCode)।"
                    } else {
                        "Speech playback failed (code: $errorCode)."
                    }
                }
            })

            pendingSpeakRequest?.let { pending ->
                pendingSpeakRequest = null
                speak(pending.text, pending.language, pending.pitch, pending.speed)
            }
        } else {
            isTtsReady = false
            _ttsStatusMessage.value = if (currentLanguage == AppLanguage.BENGALI) {
                "টেক্সট-টু-স্পিচ ইঞ্জিন চালু করা যায়নি।"
            } else {
                "Text-to-Speech engine initialization failed."
            }
        }
    }

    fun configureVoiceForLanguage(
        language: AppLanguage,
        pitch: Float = 1.15f,
        speed: Float = 1.0f
    ) {
        currentLanguage = language
        val tts = textToSpeech ?: return
        if (!isTtsReady) return

        val availableVoices = try {
            tts.voices
        } catch (_: Exception) {
            null
        }

        val bestVoice = selectBestTtsVoice(availableVoices, language)
        if (bestVoice != null) {
            val setResult = try {
                tts.setVoice(bestVoice)
            } catch (_: Exception) {
                TextToSpeech.ERROR
            }
            if (setResult == TextToSpeech.SUCCESS) {
                val isFemale = isFemaleVoice(bestVoice)
                _voiceProfileInfo.value = VoiceProfileInfo(
                    voiceName = bestVoice.name,
                    localeTag = bestVoice.locale.toLanguageTag(),
                    isFemaleSelected = isFemale,
                    isBengaliSupported = bestVoice.locale.language.equals("bn", ignoreCase = true)
                )
                _ttsStatusMessage.value = null
                tts.setPitch(pitch)
                tts.setSpeechRate(speed)
                return
            }
        }

        // Fallback to locale-based selection if Voice object list is unavailable or failed
        if (language == AppLanguage.BENGALI) {
            val localesToTry = listOf(Locale("bn", "BD"), Locale("bn", "IN"), Locale("bn"))
            var matchedLocale: Locale? = null
            for (loc in localesToTry) {
                val res = tts.setLanguage(loc)
                if (res != TextToSpeech.LANG_MISSING_DATA && res != TextToSpeech.LANG_NOT_SUPPORTED) {
                    matchedLocale = loc
                    break
                }
            }
            if (matchedLocale != null) {
                _voiceProfileInfo.value = VoiceProfileInfo(
                    voiceName = matchedLocale.toLanguageTag(),
                    localeTag = matchedLocale.toLanguageTag(),
                    isFemaleSelected = false,
                    isBengaliSupported = true
                )
                _ttsStatusMessage.value = null
            } else {
                tts.setLanguage(Locale.US)
                _voiceProfileInfo.value = VoiceProfileInfo(
                    voiceName = "en-US-fallback",
                    localeTag = "en-US",
                    isFemaleSelected = false,
                    isBengaliSupported = false
                )
                _ttsStatusMessage.value = "ডিভাইসে বাংলা ভয়েস প্যাক ইনস্টল করা নেই; ইংরেজি ভয়েস ব্যবহার করা হচ্ছে।"
            }
        } else {
            val enRes = tts.setLanguage(Locale.US)
            val enSupported = enRes != TextToSpeech.LANG_MISSING_DATA && enRes != TextToSpeech.LANG_NOT_SUPPORTED
            _voiceProfileInfo.value = VoiceProfileInfo(
                voiceName = if (enSupported) "en-US" else Locale.getDefault().toLanguageTag(),
                localeTag = if (enSupported) "en-US" else Locale.getDefault().toLanguageTag(),
                isFemaleSelected = false,
                isBengaliSupported = false
            )
            _ttsStatusMessage.value = null
        }

        tts.setPitch(pitch)
        tts.setSpeechRate(speed)
    }

    fun startListening(language: AppLanguage) {
        currentLanguage = language
        _voiceErrorMessage.value = null

        if (_voiceState.value == VoiceState.SPEAKING) {
            stopSpeaking()
        }

        coroutineScope.launch(Dispatchers.Main) {
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                _voiceErrorMessage.value = if (language == AppLanguage.BENGALI) {
                    "এই ডিভাইসে স্পিচ রিকগনিশন সেবা পাওয়া যায়নি।"
                } else {
                    "Speech recognition is not available on this device."
                }
                _voiceState.value = VoiceState.ERROR
                return@launch
            }

            if (speechRecognizer == null) {
                try {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                        setRecognitionListener(this@VoiceManager)
                    }
                } catch (e: Exception) {
                    _voiceErrorMessage.value = if (language == AppLanguage.BENGALI) {
                        "ভয়েস রিকগনাইজার চালু করা যায়নি।"
                    } else {
                        "Could not initialize speech recognizer."
                    }
                    _voiceState.value = VoiceState.ERROR
                    return@launch
                }
            } else {
                try {
                    speechRecognizer?.cancel()
                } catch (_: Exception) {}
            }

            val localeTag = if (language == AppLanguage.BENGALI) "bn-BD" else "en-US"
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeTag)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, localeTag)
                putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("bn-BD", "bn-IN", "en-US"))
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }

            _recognizedText.value = ""
            _voiceState.value = VoiceState.LISTENING
            try {
                speechRecognizer?.startListening(intent)
            } catch (se: SecurityException) {
                _voiceErrorMessage.value = if (language == AppLanguage.BENGALI) {
                    "মাইক্রোফোন ব্যবহারের অনুমতি দেওয়া হয়নি।"
                } else {
                    "Microphone permission is required."
                }
                _voiceState.value = VoiceState.ERROR
            } catch (e: Exception) {
                _voiceErrorMessage.value = if (language == AppLanguage.BENGALI) {
                    "কথা শোনা শুরু করা যায়নি: ${e.localizedMessage ?: "অজানা ত্রুটি"}"
                } else {
                    "Failed to start listening: ${e.localizedMessage ?: "Unknown error"}"
                }
                _voiceState.value = VoiceState.ERROR
            }
        }
    }

    fun stopListening() {
        coroutineScope.launch(Dispatchers.Main) {
            try {
                speechRecognizer?.stopListening()
            } catch (_: Exception) {}
            if (_voiceState.value == VoiceState.LISTENING) {
                _voiceState.value = VoiceState.IDLE
            }
            _audioAmplitude.value = 0f
        }
    }

    fun speak(text: String, language: AppLanguage, pitch: Float = 1.15f, speed: Float = 1.0f) {
        val cleanText = text.replace(Regex("[*#_`~]"), "").trim()
        if (cleanText.isBlank()) return

        currentLanguage = language
        _voiceErrorMessage.value = null

        if (_voiceState.value == VoiceState.LISTENING) {
            stopListening()
        }

        if (!isTtsReady || textToSpeech == null) {
            pendingSpeakRequest = PendingSpeakRequest(cleanText, language, pitch, speed)
            return
        }

        configureVoiceForLanguage(language, pitch, speed)

        val utteranceId = "arifa_utterance_${System.currentTimeMillis()}"
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }
        _voiceState.value = VoiceState.SPEAKING
        val result = textToSpeech?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        if (result == TextToSpeech.ERROR) {
            _voiceState.value = VoiceState.ERROR
            _voiceErrorMessage.value = if (language == AppLanguage.BENGALI) {
                "ভয়েস প্লেব্যাক শুরু করা যায়নি।"
            } else {
                "Could not start voice playback."
            }
        }
    }

    fun stopSpeaking() {
        pendingSpeakRequest = null
        try {
            textToSpeech?.stop()
        } catch (_: Exception) {}
        if (_voiceState.value == VoiceState.SPEAKING) {
            _voiceState.value = VoiceState.IDLE
        }
        _audioAmplitude.value = 0f
    }

    fun clearError() {
        _voiceErrorMessage.value = null
        if (_voiceState.value == VoiceState.ERROR) {
            _voiceState.value = VoiceState.IDLE
        }
    }

    fun setVoiceError(message: String) {
        _voiceErrorMessage.value = message
        _voiceState.value = VoiceState.ERROR
        _audioAmplitude.value = 0f
    }

    fun setVoiceState(state: VoiceState) {
        _voiceState.value = state
    }

    // RecognitionListener implementation
    override fun onReadyForSpeech(params: Bundle?) {
        _voiceState.value = VoiceState.LISTENING
    }

    override fun onBeginningOfSpeech() {
        _voiceState.value = VoiceState.LISTENING
    }

    override fun onRmsChanged(rmsdB: Float) {
        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0.1f, 1f)
        _audioAmplitude.value = normalized
    }

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        _voiceState.value = VoiceState.THINKING
        _audioAmplitude.value = 0f
    }

    override fun onError(error: Int) {
        _voiceErrorMessage.value = mapSpeechErrorToMessage(error, currentLanguage)
        _voiceState.value = VoiceState.ERROR
        _audioAmplitude.value = 0f
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val text = matches?.firstOrNull().orEmpty().trim()
        _audioAmplitude.value = 0f

        if (text.isNotBlank()) {
            _recognizedText.value = text
            _voiceErrorMessage.value = null
            val callback = onSpeechResult
            if (callback != null) {
                _voiceState.value = VoiceState.THINKING
                callback.invoke(text)
            } else {
                _voiceState.value = VoiceState.IDLE
            }
        } else {
            _voiceErrorMessage.value = if (currentLanguage == AppLanguage.BENGALI) {
                "কোনো কথা বোঝা যায়নি, অনুগ্রহ করে আবার বলুন।"
            } else {
                "Could not recognize speech. Please try again."
            }
            _voiceState.value = VoiceState.ERROR
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val text = matches?.firstOrNull().orEmpty()
        if (text.isNotBlank()) {
            _recognizedText.value = text
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}

    fun destroy() {
        pendingSpeakRequest = null
        try {
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        try {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        } catch (_: Exception) {}
    }

    companion object {
        private const val TAG = "VoiceManager"

        fun isFemaleVoice(voice: Voice): Boolean {
            val name = voice.name.lowercase(Locale.ROOT)
            val features = voice.features?.map { it.lowercase(Locale.ROOT) }.orEmpty()
            val hasFemaleToken = name.contains("female") ||
                name.contains("woman") ||
                name.contains("girl") ||
                name.contains("-f-") ||
                name.contains("_f_") ||
                name.contains("bnf") ||
                name.contains("-ban-") ||
                name.contains("f00") ||
                name.contains("f01") ||
                name.contains("f02") ||
                name.contains("sfg") ||
                features.any { it.contains("female") }

            val hasMaleToken = !hasFemaleToken && (
                name.contains("male") ||
                    name.contains("-m-") ||
                    name.contains("_m_") ||
                    name.contains("bnm") ||
                    name.contains("m00") ||
                    name.contains("m01") ||
                    features.any { it.contains("male") && !it.contains("female") }
                )

            return hasFemaleToken && !hasMaleToken
        }

        fun selectBestTtsVoice(voices: Collection<Voice>?, language: AppLanguage): Voice? {
            if (voices.isNullOrEmpty()) return null
            val targetLangCode = if (language == AppLanguage.BENGALI) "bn" else "en"
            val candidates = voices.filter {
                it.locale != null && it.locale.language.equals(targetLangCode, ignoreCase = true)
            }
            if (candidates.isEmpty()) return null

            return candidates.maxByOrNull { voice ->
                var score = 0
                val name = voice.name.lowercase(Locale.ROOT)
                val country = voice.locale.country.uppercase(Locale.ROOT)

                if (language == AppLanguage.BENGALI) {
                    score += when (country) {
                        "BD" -> 60
                        "IN" -> 30
                        else -> 10
                    }
                } else {
                    score += when (country) {
                        "US" -> 40
                        "GB" -> 30
                        else -> 10
                    }
                }

                if (isFemaleVoice(voice)) {
                    score += 120
                } else if (
                    name.contains("male") ||
                    name.contains("-m-") ||
                    name.contains("_m_") ||
                    name.contains("bnm") ||
                    name.contains("m00") ||
                    name.contains("m01")
                ) {
                    score -= 100
                }

                score += when {
                    voice.quality >= Voice.QUALITY_VERY_HIGH -> 30
                    voice.quality >= Voice.QUALITY_HIGH -> 20
                    voice.quality >= Voice.QUALITY_NORMAL -> 10
                    else -> 0
                }

                if (!voice.isNetworkConnectionRequired) {
                    score += 15
                }

                if (voice.latency <= Voice.LATENCY_NORMAL) {
                    score += 5
                }

                score
            }
        }

        fun mapSpeechErrorToMessage(error: Int, language: AppLanguage): String {
            val isBn = language == AppLanguage.BENGALI
            return when (error) {
                SpeechRecognizer.ERROR_AUDIO ->
                    if (isBn) "অডিও রেকর্ডিংয়ে সমস্যা হয়েছে।" else "Audio recording error."
                SpeechRecognizer.ERROR_CLIENT ->
                    if (isBn) "স্পিচ ক্লায়েন্টে ত্রুটি ঘটেছে।" else "Speech recognition client error."
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                    if (isBn) "মাইক্রোফোন পারমিশন দেওয়া হয়নি।" else "Microphone permission denied."
                SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                    if (isBn) "ভয়েস চিনতে নেটওয়ার্ক সংযোগ ব্যর্থ হয়েছে।" else "Network error during speech recognition."
                SpeechRecognizer.ERROR_NO_MATCH ->
                    if (isBn) "কোনো কথা বোঝা যায়নি, স্পষ্টভাবে আবার বলুন।" else "Didn't catch that. Please speak clearly and try again."
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
                    if (isBn) "স্পিচ রিকগনাইজার ব্যস্ত, মুহূর্ত পরে চেষ্টা করুন।" else "Speech recognizer is busy, please wait a moment."
                SpeechRecognizer.ERROR_SERVER ->
                    if (isBn) "স্পিচ সার্ভারে সংযোগ ত্রুটি হয়েছে।" else "Speech recognition server error."
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                    if (isBn) "কথা বলার সময় শেষ হয়েছে, আবার মাইক চাপুন।" else "No speech input detected. Tap mic to try again."
                else ->
                    if (isBn) "ভয়েস ইনপুট ত্রুটি (কোড: $error)।" else "Speech recognition error (code: $error)."
            }
        }
    }
}
