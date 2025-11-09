package com.example.storybridge_android.data

import com.example.storybridge_android.network.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PageRepositoryImpl : PageRepository {

    override suspend fun getImage(sessionId: String, pageIndex: Int): Result<GetImageResponse> =
        withContext(Dispatchers.IO) {
            try {
                val res = RetrofitClient.pageApi.getImage(sessionId, pageIndex)
                if (res.isSuccessful && res.body() != null)
                    Result.success(res.body()!!)
                else
                    Result.failure(Exception("Image fetch failed: ${res.code()}"))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun getOcrResults(sessionId: String, pageIndex: Int): Result<GetOcrTranslationResponse> =
        withContext(Dispatchers.IO) {
            try {
                val res = RetrofitClient.pageApi.getOcrResults(sessionId, pageIndex)
                if (res.isSuccessful && res.body() != null)
                    Result.success(res.body()!!)
                else
                    Result.failure(Exception("OCR fetch failed: ${res.code()}"))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun getTtsResults(sessionId: String, pageIndex: Int): Result<GetTtsResponse> =
        withContext(Dispatchers.IO) {
            try {
                val res = RetrofitClient.pageApi.getTtsResults(sessionId, pageIndex)
                if (res.isSuccessful && res.body() != null)
                    Result.success(res.body()!!)
                else
                    Result.failure(Exception("TTS fetch failed: ${res.code()}"))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
