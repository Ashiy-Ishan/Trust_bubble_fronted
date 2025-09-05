package com.yourdomain.bubbletrust.network

import com.yourdomain.bubbletrust.data.AnalysisResponse
import com.yourdomain.bubbletrust.data.HistoryResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {
    @Multipart
    @POST("analyze")
    suspend fun analyzeScreenshot(
        @Part file: MultipartBody.Part
    ): Response<AnalysisResponse>

    @GET("history")
    suspend fun getHistory(): Response<HistoryResponse>
}