package com.example.data.api

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.net.UnknownServiceException
import java.util.concurrent.TimeUnit

object ApiClient {

    class BackendNotConfiguredException(message: String) : IllegalStateException(message)

    private val LOCAL_DEV_HOSTS = setOf("10.0.2.2", "localhost", "127.0.0.1")

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .build()

    private var currentBaseUrl: String = BuildConfig.BACKEND_BASE_URL.trim()
    private var cachedService: ArifaApiService? = null

    /**
     * Validates and normalizes the configured backend URL for physical devices & production.
     * - Blank URLs throw BackendNotConfiguredException (never falls back to 10.0.2.2 or localhost).
     * - Production/physical device communication requires HTTPS.
     * - Debug builds only permit cleartext HTTP if explicitly targeting loopback hosts during local tests.
     */
    fun normalizeAndValidateUrl(rawUrl: String, isDebug: Boolean = BuildConfig.DEBUG): String {
        val trimmed = rawUrl.trim()
        if (trimmed.isBlank()) {
            throw BackendNotConfiguredException("No HTTPS backend deployment URL is configured.")
        }
        val normalized = if (trimmed.endsWith("/")) trimmed else "$trimmed/"
        val parsed = normalized.toHttpUrlOrNull()
            ?: throw IllegalArgumentException("Invalid backend URL format: $trimmed")

        if (!parsed.isHttps) {
            if (!isDebug) {
                throw UnknownServiceException(
                    "HTTPS is required. Cleartext HTTP to ${parsed.host} is not permitted."
                )
            }
            if (parsed.host !in LOCAL_DEV_HOSTS) {
                throw UnknownServiceException(
                    "Cleartext HTTP is blocked for ${parsed.host}. Use a deployed HTTPS backend URL."
                )
            }
        }
        return normalized
    }

    @Synchronized
    fun getApiService(baseUrl: String): ArifaApiService {
        val normalizedUrl = normalizeAndValidateUrl(baseUrl, BuildConfig.DEBUG)
        if (cachedService != null && currentBaseUrl == normalizedUrl) {
            return cachedService!!
        }

        currentBaseUrl = normalizedUrl
        val retrofit = Retrofit.Builder()
            .baseUrl(normalizedUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        val service = retrofit.create(ArifaApiService::class.java)
        cachedService = service
        return service
    }
}
