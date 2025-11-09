package com.example.storybridge_android.data

import com.example.storybridge_android.network.*

interface SessionRepository {
    suspend fun startSession(userId: String): Result<StartSessionResponse>
    suspend fun selectVoice(sessionId: String, voiceStyle: String): Result<SelectVoiceResponse>
    suspend fun endSession(sessionId: String): Result<EndSessionResponse>
    suspend fun getSessionStats(sessionId: String): Result<SessionStatsResponse>
    suspend fun reloadSession(userId: String, startedAt: String, pageIndex: Int): Result<ReloadSessionResponse>
    suspend fun reloadAllSession(userId: String, startedAt: String): Result<ReloadAllSessionResponse>
    suspend fun discardSession(sessionId: String): Result<DiscardSessionResponse>
}
