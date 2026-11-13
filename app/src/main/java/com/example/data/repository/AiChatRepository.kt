package com.example.data.repository

import android.util.Log
import com.example.data.api.ApiClient
import com.example.data.api.model.ChatApiRequest
import com.example.data.api.model.ChatHistoryItem
import com.example.data.local.ChatDao
import com.example.data.local.ChatMessageEntity
import com.example.data.model.AppLanguage
import com.example.data.model.ChatRole
import com.example.data.preferences.UserPreferencesManager
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.net.UnknownServiceException
import javax.net.ssl.SSLException

private const val TAG = "AiChatRepository"

class AiChatRepository(
    private val chatDao: ChatDao,
    private val preferencesManager: UserPreferencesManager
) {

    // Lazily initialized Firebase AI Logic Gemini model (authenticates via App Check, no API key needed)
    private val firebaseModel: GenerativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(SYSTEM_INSTRUCTION)
            }
        )
    }

    fun getMessages(sessionId: String = "default_session"): Flow<List<ChatMessageEntity>> {
        return chatDao.getMessagesForSession(sessionId)
    }

    suspend fun saveUserMessage(content: String, language: AppLanguage, sessionId: String = "default_session"): Long {
        return chatDao.insertMessage(
            ChatMessageEntity(
                sessionId = sessionId,
                role = ChatRole.USER.name,
                content = content,
                language = language.code
            )
        )
    }

    suspend fun saveAssistantMessage(content: String, language: AppLanguage, sessionId: String = "default_session"): Long {
        return chatDao.insertMessage(
            ChatMessageEntity(
                sessionId = sessionId,
                role = ChatRole.ASSISTANT.name,
                content = content,
                language = language.code
            )
        )
    }

    suspend fun clearHistory(sessionId: String = "default_session") {
        chatDao.clearSession(sessionId)
    }

    suspend fun clearAllHistory() {
        chatDao.clearAll()
    }

    /**
     * Checks backend readiness:
     * - When no custom backend URL is set, Firebase AI Logic is ready by default.
     * - When a custom HTTPS backend URL is configured, checks /api/health.
     */
    suspend fun checkBackendHealth(): Result<Boolean> = withContext(Dispatchers.IO) {
        val backendUrl = preferencesManager.backendApiUrl.value.trim()
        if (backendUrl.isBlank()) {
            return@withContext Result.success(true)
        }
        val language = preferencesManager.language.value
        return@withContext try {
            val apiService = ApiClient.getApiService(backendUrl)
            val response = apiService.checkHealth()
            if (response.isSuccessful && response.body()?.status == "ok") {
                Result.success(response.body()?.keyConfigured == true)
            } else {
                Result.failure(Exception(mapNetworkException(Exception("HTTP ${response.code()}"), backendUrl, language)))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(Exception(mapNetworkException(e, backendUrl, language), e))
        }
    }

    /**
     * Sends message to Gemini:
     * - By default (when custom backend URL is empty), uses Firebase AI Logic (Gemini 3.8 Flash + App Check),
     *   requiring zero API keys and working on both Streaming Emulator and physical phones.
     * - When a custom HTTPS backend URL is provided in Settings, calls that backend via Retrofit.
     */
    suspend fun sendMessage(
        userPrompt: String,
        language: AppLanguage,
        sessionId: String = "default_session"
    ): Result<String> = withContext(Dispatchers.IO) {
        val trimmedPrompt = userPrompt.trim()
        if (trimmedPrompt.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Message cannot be empty"))
        }

        // 1. Save user message to local Room database
        saveUserMessage(trimmedPrompt, language, sessionId)

        // 2. Build multi-turn history from past Room messages
        val pastMessages = chatDao.getMessagesForSession(sessionId).firstOrNull().orEmpty()
        val recentHistory = pastMessages.takeLast(10).filter { msg ->
            msg.content.isNotBlank() && msg.content != trimmedPrompt
        }

        val backendUrl = preferencesManager.backendApiUrl.value.trim()
        val isBn = language == AppLanguage.BENGALI

        // Default path: Firebase AI Logic (serverless, HTTPS, no API key in APK)
        if (backendUrl.isBlank()) {
            return@withContext sendViaFirebaseAiLogic(
                prompt = trimmedPrompt,
                historyMessages = recentHistory,
                language = language,
                sessionId = sessionId
            )
        }

        // Optional custom backend path when user explicitly provides an HTTPS URL in Settings
        val historyItems = recentHistory.map { msg ->
            ChatHistoryItem(
                role = if (msg.role == ChatRole.USER.name) "user" else "model",
                text = msg.content
            )
        }

        try {
            val apiService = ApiClient.getApiService(backendUrl)
            val request = ChatApiRequest(
                message = trimmedPrompt,
                language = language.code,
                history = historyItems
            )

            val response = apiService.sendChatMessage(request)

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val replyText = body.reply?.trim()
                if (!replyText.isNullOrBlank()) {
                    saveAssistantMessage(replyText, language, sessionId)
                    return@withContext Result.success(replyText)
                } else {
                    val err = if (isBn) "সার্ভার থেকে কোনো টেক্সট পাওয়া যায়নি।" else "Empty response received from AI server."
                    return@withContext Result.failure(Exception(err))
                }
            } else {
                val errorBodyStr = response.errorBody()?.string().orEmpty()
                val parsedMessage = try {
                    val json = JSONObject(errorBodyStr)
                    json.optString("message").ifEmpty { json.optString("error") }
                } catch (_: Exception) {
                    ""
                }

                val finalErrorMessage = if (parsedMessage.isNotBlank()) {
                    parsedMessage
                } else {
                    if (isBn) {
                        "সার্ভার ত্রুটি: কোড ${response.code()} ($backendUrl)"
                    } else {
                        "Server error HTTP ${response.code()} from $backendUrl"
                    }
                }

                return@withContext Result.failure(Exception(finalErrorMessage))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Custom backend chat failed", e)
            return@withContext Result.failure(Exception(mapNetworkException(e, backendUrl, language), e))
        }
    }

    private suspend fun sendViaFirebaseAiLogic(
        prompt: String,
        historyMessages: List<ChatMessageEntity>,
        language: AppLanguage,
        sessionId: String
    ): Result<String> {
        val isBn = language == AppLanguage.BENGALI
        return try {
            val firebaseHistory = historyMessages.map { msg ->
                val role = if (msg.role == ChatRole.USER.name) "user" else "model"
                content(role) {
                    text(msg.content)
                }
            }
            val chat = firebaseModel.startChat(history = firebaseHistory)
            val response = withTimeout(60_000L) {
                chat.sendMessage(prompt)
            }
            val replyText = response.text?.trim()
            if (!replyText.isNullOrBlank()) {
                saveAssistantMessage(replyText, language, sessionId)
                Result.success(replyText)
            } else {
                val emptyMsg = if (isBn) {
                    "Gemini থেকে কোনো উত্তর পাওয়া যায়নি। অনুগ্রহ করে আবার চেষ্টা করুন।"
                } else {
                    "Gemini returned an empty response. Please try again."
                }
                Result.failure(Exception(emptyMsg))
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Firebase AI Logic generation timed out", e)
            val timeoutMsg = if (isBn) {
                "Gemini থেকে উত্তর পেতে সময় বেশি লাগছে (৬০ সেকেন্ড)। ইন্টারনেট সংযোগ দেখে আবার চেষ্টা করুন।"
            } else {
                "Gemini generation timed out after 60 seconds. Please check your internet connection and retry."
            }
            Result.failure(Exception(timeoutMsg, e))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI Logic generation failed", e)
            val errDetail = e.localizedMessage?.takeIf { it.isNotBlank() } ?: "Unknown error"
            val userMsg = if (isBn) {
                "Gemini সংযোগ ত্রুটি: $errDetail"
            } else {
                "Gemini error: $errDetail"
            }
            Result.failure(Exception(userMsg, e))
        }
    }

    companion object {
        private const val SYSTEM_INSTRUCTION =
            "You are ARIFA AI (আরিফা এআই), an intelligent, polite, and futuristic female AI companion. " +
                "You are fully bilingual and speak fluent Bengali (বাংলা) and English. " +
                "When the user writes or asks in Bengali, respond in authentic, natural, and warm Bengali. " +
                "When the user writes in English, respond in polished English. " +
                "Keep formatting clean and helpful."

        fun mapNetworkException(
            error: Throwable,
            backendUrl: String,
            language: AppLanguage
        ): String {
            val isBn = language == AppLanguage.BENGALI
            val rawMessage = error.message.orEmpty()

            return when {
                error is ApiClient.BackendNotConfiguredException -> {
                    if (isBn) {
                        "কোনো কাস্টম HTTPS ব্যাকএন্ড URL দেওয়া হয়নি।"
                    } else {
                        "No custom HTTPS backend URL is configured."
                    }
                }
                error is UnknownServiceException || rawMessage.contains("CLEARTEXT", ignoreCase = true) -> {
                    if (isBn) {
                        "নিরাপদ HTTPS সংযোগ প্রয়োজন। এই বিল্ডে HTTP (cleartext) ট্র্যাফিক অনুমোদিত নয় ($backendUrl)।"
                    } else {
                        "Secure HTTPS connection required. Cleartext HTTP traffic to $backendUrl is blocked by network security policy."
                    }
                }
                error is JsonDataException || error is JsonEncodingException -> {
                    if (isBn) {
                        "সার্ভার JSON উত্তরের বদলে HTML/লগইন পেজ পাঠিয়েছে ($backendUrl)।"
                    } else {
                        "Backend returned HTML or invalid JSON instead of API JSON ($backendUrl)."
                    }
                }
                error is IllegalArgumentException -> {
                    if (isBn) {
                        "ব্যাকএন্ড সার্ভারের URL সঠিক নয় ($backendUrl)। সেটিংস থেকে সঠিক https:// URL দিন অথবা ফাঁকা রাখুন।"
                    } else {
                        "Invalid backend URL ($backendUrl). Enter a valid https:// URL or leave blank to use built-in Gemini AI."
                    }
                }
                error is SSLException -> {
                    if (isBn) {
                        "সার্ভারের HTTPS/SSL সিকিউরিটি যাচাই করা যায়নি ($backendUrl)।"
                    } else {
                        "SSL/TLS handshake failed while connecting to $backendUrl."
                    }
                }
                error is ConnectException -> {
                    if (isBn) {
                        "ব্যাকএন্ড সার্ভারের সাথে সংযোগ করা যাচ্ছে না ($backendUrl)। অথবা সেটিংস থেকে URL ফাঁকা রেখে বিল্ট-ইন Gemini ব্যবহার করুন।"
                    } else {
                        "Unable to connect to backend server ($backendUrl). Clear the URL in Settings to use built-in Gemini AI."
                    }
                }
                error is UnknownHostException -> {
                    if (isBn) {
                        "নেটওয়ার্ক সংযোগ নেই বা সার্ভার হোস্ট খুঁজে পাওয়া যায়নি ($backendUrl)।"
                    } else {
                        "No internet connection or backend host not found ($backendUrl)."
                    }
                }
                error is SocketTimeoutException -> {
                    if (isBn) {
                        "সার্ভারের সাড়া পেতে অতিরিক্ত সময় লেগেছে (Timeout)। অনুগ্রহ করে পুনরায় চেষ্টা করুন।"
                    } else {
                        "Connection timed out waiting for Gemini backend response. Please try again."
                    }
                }
                error is IOException -> {
                    if (isBn) {
                        "নেটওয়ার্ক ত্রুটি: ${error.localizedMessage ?: "সার্ভারের সাথে সংযোগ বিচ্ছিন্ন"}"
                    } else {
                        "Network error: ${error.localizedMessage ?: "Unable to reach backend"}"
                    }
                }
                else -> error.localizedMessage ?: if (isBn) "অজানা ত্রুটি হয়েছে।" else "Unexpected error occurred."
            }
        }
    }
}
