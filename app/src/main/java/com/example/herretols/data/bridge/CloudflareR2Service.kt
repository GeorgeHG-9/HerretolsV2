package com.example.herretols.data.bridge

import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface CloudflareR2Service {
    // Ahora enviamos el archivo directamente al Worker por POST
    @POST("/")
    suspend fun uploadImage(
        @Header("Authorization") token: String,
        @Query("fileName") fileName: String,
        @Body file: RequestBody
    ): Response<UploadResponse>
}

