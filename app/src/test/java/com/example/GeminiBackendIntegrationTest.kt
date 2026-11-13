package com.example

import com.example.data.api.ApiClient
import com.example.data.api.model.ChatApiRequest
import com.example.data.api.model.ChatApiResponse
import com.example.data.api.model.ChatHistoryItem
import com.example.data.local.ChatDao
import com.example.data.local.ChatMessageEntity
import com.example.data.model.AppLanguage
import com.example.data.preferences.UserPreferencesManager
import com.example.data.repository.AiChatRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownServiceException

class GeminiBackendIntegrationTest {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    // Fake in-memory ChatDao for repository testing
    private val fakeChatDao = object : ChatDao {
        val messages = mutableListOf<ChatMessageEntity>()

        override fun getMessagesForSession(sessionId: String): Flow<List<ChatMessageEntity>> {
            return flowOf(messages.filter { it.sessionId == sessionId })
        }

        override fun getAllMessages(): Flow<List<ChatMessageEntity>> = flowOf(messages)

        override fun getAllSessionIds(): Flow<List<String>> = flowOf(listOf("default_session"))

        override suspend fun insertMessage(message: ChatMessageEntity): Long {
            messages.add(message)
            return messages.size.toLong()
        }

        override suspend fun clearSession(sessionId: String) {
            messages.removeAll { it.sessionId == sessionId }
        }

        override suspend fun clearAll() {
            messages.clear()
        }

        override suspend fun getMessageCount(): Int = messages.size
    }

    @Test
    fun testBengaliLanguageJsonSerialization() {
        val request = ChatApiRequest(
            message = "আজকের আবহাওয়া কেমন এবং আরিফা এআই এর পরিচয় কি?",
            language = "bn",
            history = listOf(
                ChatHistoryItem(role = "user", text = "নমস্কার!"),
                ChatHistoryItem(role = "model", text = "নমস্কার! আমি আরিফা এআই।")
            )
        )

        val adapter = moshi.adapter(ChatApiRequest::class.java)
        val json = adapter.toJson(request)

        assertTrue("JSON should contain Bengali prompt text", json.contains("আজকের আবহাওয়া"))
        assertTrue("JSON should contain Bengali history", json.contains("নমস্কার!"))

        val deserialized = adapter.fromJson(json)
        assertNotNull(deserialized)
        assertEquals("bn", deserialized?.language)
        assertEquals("আজকের আবহাওয়া কেমন এবং আরিফা এআই এর পরিচয় কি?", deserialized?.message)
        assertEquals(2, deserialized?.history?.size)
    }

    @Test
    fun testNetworkSecurityUrlValidationDebugVsRelease() {
        // Blank URL must fail fast with BackendNotConfiguredException (never fall back to 10.0.2.2 or localhost)
        assertThrows(ApiClient.BackendNotConfiguredException::class.java) {
            ApiClient.normalizeAndValidateUrl("", isDebug = true)
        }
        assertThrows(ApiClient.BackendNotConfiguredException::class.java) {
            ApiClient.normalizeAndValidateUrl("   ", isDebug = false)
        }

        // HTTPS works in both debug and release
        assertEquals(
            "https://api.arifa-ai.example.com/",
            ApiClient.normalizeAndValidateUrl("https://api.arifa-ai.example.com", isDebug = true)
        )
        assertEquals(
            "https://api.arifa-ai.example.com/",
            ApiClient.normalizeAndValidateUrl("https://api.arifa-ai.example.com", isDebug = false)
        )

        // Debug rejects unrestricted HTTP to external hosts
        assertThrows(UnknownServiceException::class.java) {
            ApiClient.normalizeAndValidateUrl("http://example.com:3000/", isDebug = true)
        }

        // Release strictly forbids ALL HTTP cleartext including 10.0.2.2
        assertThrows(UnknownServiceException::class.java) {
            ApiClient.normalizeAndValidateUrl("http://10.0.2.2:3000/", isDebug = false)
        }
    }

    @Test
    fun testMeaningfulErrorMessagesForNetworkAndSecurityFailures() {
        val cleartextMsgBn = AiChatRepository.mapNetworkException(
            UnknownServiceException("CLEARTEXT communication to 10.0.2.2 not permitted by network security policy"),
            "http://10.0.2.2:3000/",
            AppLanguage.BENGALI
        )
        assertTrue(cleartextMsgBn.contains("HTTPS"))

        val unreachableMsgBn = AiChatRepository.mapNetworkException(
            ConnectException("Failed to connect to /10.0.2.2:3000"),
            "http://10.0.2.2:3000/",
            AppLanguage.BENGALI
        )
        assertTrue(unreachableMsgBn.contains("3000"))

        val timeoutMsgEn = AiChatRepository.mapNetworkException(
            SocketTimeoutException("timeout"),
            "http://10.0.2.2:3000/",
            AppLanguage.ENGLISH
        )
        assertTrue(timeoutMsgEn.contains("timed out", ignoreCase = true))
    }

    @Test
    fun testBackendHealthEndpoint() = runBlocking {
        // Connect to the local running Node.js backend on port 3000
        val service = ApiClient.getApiService("http://localhost:3000/")
        val response = service.checkHealth()
        assertTrue("Health endpoint should respond with HTTP 200", response.isSuccessful)
        val body = response.body()
        assertNotNull("Health body should not be null", body)
        assertEquals("ok", body?.status)
        assertEquals("ARIFA AI Gemini Backend", body?.service)
    }

    @Test
    fun testChatEndpointWithBengaliMessage() = runBlocking {
        val service = ApiClient.getApiService("http://127.0.0.1:3000/")
        val response = service.sendChatMessage(
            ChatApiRequest(
                message = "কেমন আছো আরিফা?",
                language = "bn"
            )
        )
        if (response.isSuccessful) {
            val reply = response.body()?.reply?.trim().orEmpty()
            assertTrue("Expected non-blank Gemini reply", reply.isNotEmpty())
        } else {
            // Backend is reachable; when server/.env lacks GEMINI_API_KEY it returns 503 with explicit JSON error
            assertEquals(503, response.code())
            val errBody = response.errorBody()?.string().orEmpty()
            assertTrue(
                "Expected explicit GEMINI_API_KEY_NOT_CONFIGURED error, got: $errBody",
                errBody.contains("GEMINI_API_KEY_NOT_CONFIGURED")
            )
        }
    }

    @Test
    fun testNetworkErrorHandlingOnUnreachableHost() = runBlocking {
        // Connect to unreachable host
        val unreachableService = ApiClient.getApiService("http://127.0.0.1:59999/")
        var caughtException = false

        try {
            unreachableService.sendChatMessage(
                ChatApiRequest(message = "কেমন আছো?", language = "bn")
            )
        } catch (e: IOException) {
            caughtException = true
            assertTrue("Should catch IOException for network failure", true)
        } catch (_: Exception) {
            caughtException = true
        }

        assertTrue("Network error was successfully handled and caught", caughtException)
    }
}
