package com.example.herretols.data.bridge

import com.example.herretols.data.dto.OllamaRequest
import com.example.herretols.data.dto.OllamaResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface OllamaService {
    @POST("api/generate")
    suspend fun generate(
        @Body request: OllamaRequest
    ): OllamaResponse
}