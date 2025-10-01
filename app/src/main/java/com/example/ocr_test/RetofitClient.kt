package com.example.ocr_test


import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "baseurl" //replace this to your base url

    val instance: NaverOcrService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NaverOcrService::class.java)
    }
}
data class OcrRequest(
    val version: String = "V1",
    val requestId: String = "sample-request",
    val timestamp: Long = System.currentTimeMillis(),
    val images: List<OcrImage>
)

data class OcrImage(
    val format: String,
    val name: String,
    val data: String
)