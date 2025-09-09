package com.example.trust_bubble.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
object RetrofitClient {
    // IMPORTANT: REPLACE WITH YOUR IP ADDRESS OR NGROK URL
    private const val BASE_URL = "https://ccdf4596fbe2.ngrok-free.app/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS) // Wait 60 seconds to connect
        .readTimeout(30, TimeUnit.SECONDS)    // Wait 60 seconds for a response
        .writeTimeout(10, TimeUnit.SECONDS)   // Wait 60 seconds to send data
        .build()


    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}