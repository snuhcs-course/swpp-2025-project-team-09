package com.example.storybridge_android.network

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

// Retrofit API Interface
interface ProcessApi {

    @POST("/process/upload/")
    fun uploadImage(
        @Body request: UploadImageRequest
    ): Call<UploadImageResponse>

    @GET("/process/check_ocr/")
    fun checkOcrTranslationStatus(
        @Query("session_id") session_id: String,
        @Query("page_index") page_index: Int
    ): Call<CheckOcrTranslationResponse>

    @GET("/process/check_tts/")
    fun checkTtsStatus(
        @Query("session_id") session_id: String,
        @Query("page_index") page_index: Int
    ): Call<CheckTtsResponse>
}

// --------------------
// Request / Response data classes
// --------------------

// 3-1. Upload Image
data class UploadImageRequest(
    val session_id: String,
    val page_index: Int,
    val lang: String,
    val image_base64: String
)

data class UploadImageResponse(
    val session_id: String,
    val page_index: Int,
    val status: String,
    val submitted_at: String // datetime as ISO string
)

// 3-2. Check OCR, Translation Status
data class CheckOcrTranslationRequest(
    val session_id: String,
    val page_index: Int
)

data class CheckOcrTranslationResponse(
    val session_id: String,
    val page_index: Int,
    val status: String,           // "pending", "processing", "ready"
    val progress: Int,            // 0-100
    val submitted_at: String,
    val processed_at: String?     // nullable, if not ready
)

// 3-3. Check TTS Status
data class CheckTtsRequest(
    val session_id: String,
    val page_index: Int
)

data class CheckTtsResponse(
    val session_id: String,
    val page_index: Int,
    val status: String,           // "pending", "processing", "ready"
    val progress: Int,            // 0-100
    val submitted_at: String,
    val processed_at: String?     // nullable, if not ready
)
