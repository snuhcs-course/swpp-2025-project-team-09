package com.example.storybridge_android.network

import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

// Retrofit API Interface
interface ProcessApi {

    @POST("/process/upload/")
    suspend fun uploadImage(
        @Body request: UploadImageRequest
    ): Response<UploadImageResponse>

    @POST("/process/upload_cover/")
    suspend fun uploadCoverImage(
        @Body request: UploadImageRequest
    ): Response<UploadCoverResponse>

    @GET("/process/check_ocr/")
    suspend fun checkOcrStatus(
        @Query("session_id") session_id: String,
        @Query("page_index") page_index: Int
    ): Response<CheckOcrResponse>

    @GET("/process/check_tts/")
    suspend fun checkTtsStatus(
        @Query("session_id") session_id: String,
        @Query("page_index") page_index: Int
    ): Response<CheckTtsResponse>
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

data class UploadCoverResponse(
    val session_id: String,
    val page_index: Int,
    val status: String,
    val submitted_at: String, // datetime as ISO string
    val title: String,
    val tts_male: String,
    val tts_female: String
)

// 3-2. Check OCR, Translation Status
data class CheckOcrRequest(
    val session_id: String,
    val page_index: Int
)

data class CheckOcrResponse(
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

/**
 * Updated to match new backend response format
 * Backend now returns bb_status for granular TTS progress tracking
 */
data class CheckTtsResponse(
    val session_id: String,
    val page_index: Int,
    val status: String,           // "pending", "processing", "ready"
    val progress: Int,            // 0-100
    @SerializedName("bb_status")
    val bb_status: List<BBoxStatus>? = null  // New field for per-bbox status
)

/**
 * Status information for individual bounding boxes
 * Allows tracking which specific text boxes have audio ready
 */
data class BBoxStatus(
    @SerializedName("bbox_index")
    val bbox_index: Int,
    val status: String,  // "pending", "processing", "ready", "failed"
    @SerializedName("has_audio")
    val has_audio: Boolean
)