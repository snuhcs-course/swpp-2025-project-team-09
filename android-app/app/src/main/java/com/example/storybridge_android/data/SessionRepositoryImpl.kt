package com.example.storybridge_android.data

import com.example.storybridge_android.network.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SessionRepositoryImpl : SessionRepository {

    override suspend fun startSession(userId: String): Result<StartSessionResponse> =
        withContext(Dispatchers.IO) {
            try {
                val res = RetrofitClient.sessionApi.startSession(StartSessionRequest(userId))
                if (res.isSuccessful && res.body() != null)
                    Result.success(res.body()!!)
                else
                    Result.failure(Exception("Start session failed: ${res.code()}"))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun selectVoice(sessionId: String, voiceStyle: String): Result<SelectVoiceResponse> =
        withContext(Dispatchers.IO) {
            try {
                val res = RetrofitClient.sessionApi.selectVoice(SelectVoiceRequest(sessionId, voiceStyle))
                if (res.isSuccessful && res.body() != null)
                    Result.success(res.body()!!)
                else
                    Result.failure(Exception("Voice selection failed: ${res.code()}"))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun endSession(sessionId: String): Result<EndSessionResponse> =
        withContext(Dispatchers.IO) {
            try {
                val res = RetrofitClient.sessionApi.endSession(EndSessionRequest(sessionId))
                if (res.isSuccessful && res.body() != null)
                    Result.success(res.body()!!)
                else
                    Result.failure(Exception("End session failed: ${res.code()}"))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun getSessionStats(sessionId: String): Result<SessionStatsResponse> =
        withContext(Dispatchers.IO) {
            try {
                val res = RetrofitClient.sessionApi.getSessionStats(sessionId)
                if (res.isSuccessful && res.body() != null)
                    Result.success(res.body()!!)
                else
                    Result.failure(Exception("Get stats failed: ${res.code()}"))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun reloadSession(
        userId: String,
        startedAt: String,
        pageIndex: Int
    ): Result<ReloadSessionResponse> =
        withContext(Dispatchers.IO) {
            try {
                val res = RetrofitClient.sessionApi.reloadSession(userId, startedAt, pageIndex)
                if (res.isSuccessful && res.body() != null)
                    Result.success(res.body()!!)
                else
                    Result.failure(Exception("Reload session failed: ${res.code()}"))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun reloadAllSession(
        userId: String,
        startedAt: String
    ): Result<ReloadAllSessionResponse> =
        withContext(Dispatchers.IO) {
            try {
                val res = RetrofitClient.sessionApi.reloadAllSession(userId, startedAt)
                if (res.isSuccessful && res.body() != null)
                    Result.success(res.body()!!)
                else
                    Result.failure(Exception("Reload all session failed: ${res.code()}"))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun discardSession(sessionId: String): Result<DiscardSessionResponse> =
        withContext(Dispatchers.IO) {
            try {
                val res = RetrofitClient.sessionApi.discardSession(DiscardSessionRequest(sessionId))
                if (res.isSuccessful && res.body() != null)
                    Result.success(res.body()!!)
                else
                    Result.failure(Exception("Discard session failed: ${res.code()}"))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}