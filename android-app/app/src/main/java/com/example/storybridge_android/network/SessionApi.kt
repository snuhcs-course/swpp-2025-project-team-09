package com.example.storybridge_android.network

import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

// Retrofit API Interface
interface SessionApi {

    @POST("/session/start")
    suspend fun startSession(
        @Body request: StartSessionRequest
    ): Response<StartSessionResponse>

    @POST("/session/voice")
    suspend fun selectVoice(
        @Body request: SelectVoiceRequest
    ): Response<SelectVoiceResponse>

    @POST("/session/end")
    suspend fun endSession(
        @Body request: EndSessionRequest
    ): Response<EndSessionResponse>

    @GET("/session/stats")
    suspend fun getSessionStats(
        @Query("session_id") session_id: String
    ): Response<SessionStatsResponse>

    // 두 개는 안 쓰는 중인듯
    @GET("/session/info")
    fun getSessionInfo(
        @Query("session_id") session_id: String
    ): Call<SessionInfoResponse>

    @GET("/session/review")
    fun getSessionReview(
        @Query("session_id") session_id: String
    ): Call<SessionReviewResponse>

    @GET("/session/reload")
    suspend fun reloadSession(
        @Query("user_id") userId: String,
        @Query("started_at") startedAt: String,
        @Query("page_index") pageIndex: Int
    ): Response<ReloadSessionResponse>

    @GET("/session/reload_all")
    suspend fun reloadAllSession(
        @Query("user_id") userId: String,
        @Query("started_at") startedAt: String
    ): Response<ReloadAllSessionResponse>

    @POST("/session/discard")
    suspend fun discardSession(
        @Body request: DiscardSessionRequest
    ): Response<DiscardSessionResponse>

}

// --------------------
// Request / Response data classes
// --------------------

// 2-1. Start Reading Session
data class StartSessionRequest(
    val user_id: String
)

data class StartSessionResponse(
    val session_id: String,
    val started_at: String, // datetime as ISO string
    val page_index: Int
)

// 2-2. Select Voice for Session
data class SelectVoiceRequest(
    val session_id: String,
    val voice_style: String
)

data class SelectVoiceResponse(
    val session_id: String,
    val voice_style: String
)

// 2-3. End Reading Session
data class EndSessionRequest(
    val session_id: String
)

data class EndSessionResponse(
    val session_id: String,
    val ended_at: String, // datetime as ISO string
    val total_pages: Int
)

// 2-4. Get Session Statistics
data class SessionStatsRequest(
    val session_id: String
)

data class SessionStatsResponse(
    val session_id: String,
    val user_id: String,
    val isOngoing: Boolean,
    val started_at: String,
    val ended_at: String,
    val total_pages: Int,
    val total_time_spent: Int,
    val total_words_read: Int
    // TODO: add other optional fields
)

// 2-5. Get Session Info
data class SessionInfoRequest(
    val session_id: String
)

data class SessionInfoResponse(
    val session_id: String,
    val user_id: String,
    val page_index: Int,
    val voice_style: String,
    val started_at: String,
    val ended_at: String?, // nullable
    val total_pages: Int?  // nullable
)

// 2-6. Review
data class SessionReviewRequest(
    val session_id: String
)

data class SessionReviewResponse(
    val session_id: String,
    val user_id: String,
    val started_at: String,
    val ended_at: String,
    val total_pages: Int
)

// 2-7. Reload Session (이어보기)
data class ReloadSessionResponse(
    val session_id: String,
    val page_index: Int,
    val image_base64: String?,         // 이미지 base64 문자열
    val translation_text: String?,     // 번역 텍스트
    val audio_url: String?             // 오디오 파일 경로 or base64
)

data class ReloadAllSessionResponse(
    val session_id: String,
    val started_at: String,
    val pages: List<ReloadedPage>
)

data class ReloadedPage(
    val page_index: Int,
    val img_url: String?,
    val translation_text: String?,
    val audio_url: String?,
    val ocr_results: List<OcrBox>?
)

// 2-8. Discard Session
data class DiscardSessionRequest(
    val session_id: String
)

data class DiscardSessionResponse(
    val message: String
)

