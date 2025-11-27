package com.example.storybridge_android.network

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface PageApi {
    @GET("/page/get_image/")
    suspend fun getImage(
        @Query("session_id") session_id: String,
        @Query("page_index") page_index: Int
    ): Response<GetImageResponse>

    @GET("/page/get_ocr/")
    suspend fun getOcrResults(
        @Query("session_id") session_id: String,
        @Query("page_index") page_index: Int
    ): Response<GetOcrTranslationResponse>

    @GET("/page/get_tts/")
    suspend fun getTtsResults(
        @Query("session_id") session_id: String,
        @Query("page_index") page_index: Int
    ): Response<GetTtsResponse>
}


// --------------------
// Request / Response data classes
// --------------------

data class GetImageResponse(
    val session_id: String,
    val page_index: Int,
    val image_base64: String,
    val stored_at: String
)

data class OcrBox(
    val bbox: BBox,
    val original_txt: String,
    val translation_txt: String
)

data class BBox(
    val x1: Int,
    val y1: Int,
    val x2: Int,
    val y2: Int,
    val x3: Int,
    val y3: Int,
    val x4: Int,
    val y4: Int
) {
    val x: Int get() = minOf(x1, x2, x3, x4)
    val y: Int get() = minOf(y1, y2, y3, y4)
    val width: Int get() = maxOf(x1, x2, x3, x4) - x
    val height: Int get() = maxOf(y1, y2, y3, y4) - y
}

data class GetOcrTranslationResponse(
    val session_id: String,
    val page_index: Int,
    val ocr_results: List<OcrBox>,
    val processed_at: String
)

data class AudioResult(
    val bbox_index: Int,
    val audio_base64_list: List<String>
)

data class GetTtsResponse(
    val session_id: String,
    val page_index: Int,
    val audio_results: List<AudioResult>,
    val generated_at: String
)