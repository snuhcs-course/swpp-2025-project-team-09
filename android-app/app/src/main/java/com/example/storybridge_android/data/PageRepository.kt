package com.example.storybridge_android.data

import com.example.storybridge_android.network.*

interface PageRepository {
    suspend fun getImage(sessionId: String, pageIndex: Int): Result<GetImageResponse>
    suspend fun getOcrResults(sessionId: String, pageIndex: Int): Result<GetOcrTranslationResponse>
    suspend fun getTtsResults(sessionId: String, pageIndex: Int): Result<GetTtsResponse>
}
