package com.example.storybridge_android.network

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query


// Retrofit API Interface
interface PageApi {

    @GET("/page/get_image/")
    fun getImage(
        @Query("session_id") session_id: String,
        @Query("page_index") page_index: Int
    ): Call<GetImageResponse>

    @GET("/page/get_ocr/")
    fun getOcrResults(
        @Query("session_id") session_id: String,
        @Query("page_index") page_index: Int
    ): Call<GetOcrTranslationResponse>

    @GET("/page/get_tts/")
    fun getTtsResults(
        @Query("session_id") session_id: String,
        @Query("page_index") page_index: Int
    ): Call<GetTtsResponse>
}


// --------------------
// Request / Response data classes
// --------------------

// 4-1. Get Image
data class GetImageRequest(
    val session_id: String,
    val page_index: Int
)

data class GetImageResponse(
    val session_id: String,
    val page_index: Int,
    val image_base64: String,
    val stored_at: String // datetime as ISO string
)

// 4-2. Get OCR Results
data class GetOcrTranslationRequest(
    val session_id: String,
    val page_index: Int
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
    // 편의 프로퍼티: 좌상단(x,y)와 width/height 계산
    val x: Int get() = minOf(x1, x2, x3, x4)
    val y: Int get() = minOf(y1, y2, y3, y4)
    val width: Int get() = maxOf(x1, x2, x3, x4) - x
    val height: Int get() = maxOf(y1, y2, y3, y4) - y
}

data class GetOcrTranslationResponse(
    val session_id: String,
    val page_index: Int,
    val ocr_results: List<OcrBox>,
    val processed_at: String // datetime as ISO string
)

// 4-3. Get TTS Results
data class GetTtsRequest(
    val session_id: String,
    val page_index: Int
)

data class AudioResult(
    val bbox_index: Int,
    val audio_base64: String
)

data class GetTtsResponse(
    val session_id: String,
    val page_index: Int,
    val audio_results: List<AudioResult>,
    val generated_at: String // datetime as ISO string
)
