package com.example.storybridge_android.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

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

    @GET("/session/reload_all")
    suspend fun reloadAllSession(
        @Query("user_id") userId: String,
        @Query("started_at") startedAt: String
    ): Response<ReloadAllSessionResponse>

    @POST("/session/discard")
    suspend fun discardSession(
        @Body request: DiscardSessionRequest
    ): Response<DiscardSessionResponse>

    @POST("/process/word_picker/")
    suspend fun pickWords(
        @Body request: WordPickerRequest
    ): Response<WordPickerResponse>
}

// --------------------
// Request / Response data classes
// --------------------

data class StartSessionRequest(
    val user_id: String
)

data class StartSessionResponse(
    val session_id: String,
    val started_at: String,
    val page_index: Int
)

data class SelectVoiceRequest(
    val session_id: String,
    val voice_style: String
)

data class SelectVoiceResponse(
    val session_id: String,
    val voice_style: String
)

data class EndSessionRequest(
    val session_id: String
)

data class EndSessionResponse(
    val session_id: String,
    val ended_at: String,
    val total_pages: Int
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

data class DiscardSessionRequest(
    val session_id: String
)

data class DiscardSessionResponse(
    val message: String
)

data class WordPickerRequest(
    val session_id: String,
    val lang: String
)

data class WordPickerResponse(
    val session_id: String,
    val status: String,
    val items: List<WordItem>
)

data class WordItem(
    val word: String,
    val meaning_ko: String
)
