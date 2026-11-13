package com.example.data.api

import com.example.data.api.model.ChatApiRequest
import com.example.data.api.model.ChatApiResponse
import com.example.data.api.model.HealthResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ArifaApiService {

    @POST("api/chat")
    suspend fun sendChatMessage(
        @Body request: ChatApiRequest
    ): Response<ChatApiResponse>

    @GET("api/health")
    suspend fun checkHealth(): Response<HealthResponse>
}
