package com.example.api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

// Retrofit API Interface
interface SessionApi {

    @POST("/session/start")
    fun startSession(
        @Body request: StartSessionRequest
    ): Call<StartSessionResponse>

    @POST("/session/voice")
    fun selectVoice(
        @Body request: SelectVoiceRequest
    ): Call<SelectVoiceResponse>

    @POST("/session/end")
    fun endSession(
        @Body request: EndSessionRequest
    ): Call<EndSessionResponse>

    @GET("/session/stats")
    fun getSessionStats(
        @Body request: SessionStatsRequest
    ): Call<SessionStatsResponse>

    @GET("/session/info")
    fun getSessionInfo(
        @Body request: SessionInfoRequest
    ): Call<SessionInfoResponse>

    @GET("/session/review")
    fun getSessionReview(
        @Body request: SessionReviewRequest
    ): Call<SessionReviewResponse>
}

// --------------------
// Request / Response data classes
// --------------------

// 2-1. Start Reading Session
data class StartSessionRequest(
    val user_id: String,
    val page_index: Int
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
