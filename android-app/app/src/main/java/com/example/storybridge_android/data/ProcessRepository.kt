package com.example.storybridge_android.data

import com.example.storybridge_android.network.*
import retrofit2.Response

interface ProcessRepository {
    suspend fun uploadImage(req: UploadImageRequest): Result<UploadImageResponse>
    suspend fun uploadCoverImage(req: UploadImageRequest): Result<UploadCoverResponse>
    suspend fun checkOcrStatus(sessionId: String, pageIndex: Int): Result<CheckOcrResponse>
    suspend fun checkTtsStatus(sessionId: String, pageIndex: Int): Result<CheckTtsResponse>
}
