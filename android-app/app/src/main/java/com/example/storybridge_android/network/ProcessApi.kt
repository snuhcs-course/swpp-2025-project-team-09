package com.example.storybridge_android.network

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

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
    val submitted_at: String
)

data class UploadCoverResponse(
    val session_id: String,
    val page_index: Int,
    val status: String,
    val submitted_at: String,
    val title: String,
    val translated_title: String,
)

data class CheckOcrResponse(
    val session_id: String,
    val page_index: Int,
    val status: String,           // "pending", "processing", "ready"
    val progress: Int,            // 0-100
    val submitted_at: String,
    val processed_at: String?     // nullable, if not ready
)

data class CheckTtsResponse(
    val session_id: String,
    val page_index: Int,
    val status: String,           // "pending", "processing", "ready"
    val progress: Int,            // 0-100
    @SerializedName("bb_status")
    val bb_status: List<BBoxStatus>? = null
)

data class BBoxStatus(
    @SerializedName("bbox_index")
    val bbox_index: Int,
    val status: String,  // "pending", "processing", "ready", "failed"
    @SerializedName("has_audio")
    val has_audio: Boolean
)