package com.example.storybridge_android.data

import com.example.storybridge_android.network.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProcessRepositoryImpl(
    private val api: ProcessApi = RetrofitClient.processApi
) : ProcessRepository {

    override suspend fun uploadImage(req: UploadImageRequest): Result<UploadImageResponse> =
        withContext(Dispatchers.IO) {
            try {
                val res = api.uploadImage(req)
                if (res.isSuccessful && res.body() != null)
                    Result.success(res.body()!!)
                else
                    Result.failure(Exception("Upload failed: ${res.code()}"))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun uploadCoverImage(req: UploadImageRequest): Result<UploadCoverResponse> =
        withContext(Dispatchers.IO) {
            try {
                val res = api.uploadCoverImage(req)
                if (res.isSuccessful && res.body() != null)
                    Result.success(res.body()!!)
                else
                    Result.failure(Exception("Upload cover failed: ${res.code()}"))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun checkOcrStatus(sessionId: String, pageIndex: Int): Result<CheckOcrResponse> =
        withContext(Dispatchers.IO) {
            try {
                val res = api.checkOcrStatus(sessionId, pageIndex)
                if (res.isSuccessful && res.body() != null)
                    Result.success(res.body()!!)
                else
                    Result.failure(Exception("OCR check failed: ${res.code()}"))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun checkTtsStatus(sessionId: String, pageIndex: Int): Result<CheckTtsResponse> =
        withContext(Dispatchers.IO) {
            try {
                val res = api.checkTtsStatus(sessionId, pageIndex)
                if (res.isSuccessful && res.body() != null)
                    Result.success(res.body()!!)
                else
                    Result.failure(Exception("TTS check failed: ${res.code()}"))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
