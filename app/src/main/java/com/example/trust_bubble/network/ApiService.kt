package com.example.trust_bubble.network

import com.example.trust_bubble.data.AnalysisResponse
import com.example.trust_bubble.data.HistoryResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import okhttp3.RequestBody
import retrofit2.http.Body

interface ApiService {

    @POST("analyze")
    suspend fun analyzeScreenshot(
        @Body image: RequestBody
    ): Response<AnalysisResponse>

    @GET("history")
    suspend fun getHistory(): Response<HistoryResponse>
    // Add this to network/ApiService.kt
    @POST("clear-history")
    suspend fun clearHistory(): Response<Unit> // We don't expect a detailed response
}