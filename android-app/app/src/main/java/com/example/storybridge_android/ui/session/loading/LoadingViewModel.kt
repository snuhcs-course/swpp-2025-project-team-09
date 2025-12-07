package com.example.storybridge_android.ui.session.loading

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.provider.Settings
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storybridge_android.data.*
import com.example.storybridge_android.network.UploadImageRequest
import com.example.storybridge_android.network.UserInfoResponse
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max

class LoadingViewModel(
    private val processRepo: ProcessRepository,
    private val userRepo: UserRepository,
    private val sessionRepo: SessionRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main
) : ViewModel() {

    private val scope = viewModelScope + dispatcher

    // ========================================
    // State Flows
    // ========================================
    private val _progress = MutableStateFlow(0)
    val progress = _progress.asStateFlow()

    private val _status = MutableStateFlow("idle")
    val status = _status.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _cover = MutableStateFlow<CoverResult?>(null)
    val cover = _cover.asStateFlow()

    private val _navigateToReading = MutableStateFlow<SessionResumeResult?>(null)
    val navigateToReading = _navigateToReading.asStateFlow()

    private val _userInfo = MutableStateFlow<Response<List<UserInfoResponse>>?>(null)
    val userInfo = _userInfo.asStateFlow()

    private var rampJob: Job? = null

    // --------------------------
    // 1. resume session
    // --------------------------
    fun loadUserInfo(deviceInfo: String) {
        scope.launch {
            val response = userRepo.getUserInfo(deviceInfo)
            _userInfo.value = response
        }
    }

    @SuppressLint("HardwareIds")
    fun reloadAllSession(startedAt: String, context: Context) {
        scope.launch {
            _status.value = "reloading"
            startRampTo(100, 1000L)

            val deviceInfo = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            )

            val result = sessionRepo.reloadAllSession(deviceInfo, startedAt)
            result.fold(
                onSuccess = { data ->
                    stopRamp()
                    _progress.value = 100
                    val totalPages = data.pages.size
                    _navigateToReading.emit(SessionResumeResult(data.session_id, 0, totalPages))
                },
                onFailure = { e ->
                    stopRamp()
                    _error.emit("ReloadAll failed: ${e.message}")
                }
            )
        }
    }

    // --------------------------
    // 2. new session
    // --------------------------
    fun uploadCover(sessionId: String, lang: String, path: String) {
        scope.launch {
            _status.value = "uploading_cover"

            val base64 = encodeBase64(path)
            if (base64 == null) {
                _error.value = "Failed to process image"
                return@launch
            }
            startRampTo(100, 3000L)

            val req = UploadImageRequest(sessionId, 0, lang, base64)
            processRepo.uploadCoverImage(req).fold(
                onSuccess = {
                    while (_progress.value < 100) {
                        delay(100)
                    }

                    val result = CoverResult(it.title)
                    _cover.value = result
                    _status.value = "cover_ready"
                },
                onFailure = {
                    stopRamp()
                    _error.value = it.message
                }
            )
        }
    }

    fun uploadImage(sessionId: String, pageIndex: Int, lang: String, path: String) {
        scope.launch {
            _status.value = "uploading"

            val base64 = encodeBase64(path)
            if (base64 == null) {
                _error.value = "Failed to process image"
                return@launch
            }

            startRampTo(100, 8000L)

            val req = UploadImageRequest(sessionId, pageIndex, lang, base64)
            processRepo.uploadImage(req).fold(
                onSuccess = {
                    pollOcr(sessionId, it.page_index)
                },
                onFailure = {
                    _error.value = it.message
                }
            )
        }
    }

    private suspend fun pollOcr(sessionId: String, pageIndex: Int) {
        _status.value = "polling"

        repeat(60) { i ->
            val res = processRepo.checkOcrStatus(sessionId, pageIndex)
            var done = false

            res.fold(
                onSuccess = {
                    if (it.status == "ready") {
                        while (_progress.value < 100) {
                            delay(100)
                        }
                        _status.value = "ready"
                        done = true
                    }
                },
                onFailure = {
                    _error.value = it.message
                    return@pollOcr
                }
            )
            if (done) return

            delay(300)
        }

        _error.value = "Timeout while waiting for OCR"
    }

    // --------------------
    // Utility Functions
    // --------------------
    private fun startRampTo(target: Int, durationMs: Long) {
        stopRamp()
        rampJob = scope.launch {
            val start = _progress.value
            val diff = (target - start).coerceAtLeast(0)
            if (diff == 0) return@launch

            val steps = 40
            val stepMs = durationMs / steps
            val inc = diff.toFloat() / steps

            repeat(steps) { i ->
                delay(stepMs)
                _progress.value = (start + inc * (i + 1)).toInt().coerceAtMost(target)
            }
        }
    }

    private fun stopRamp() {
        rampJob?.cancel()
        rampJob = null
    }

    private fun encodeBase64(path: String): String? {
        val file = File(path)
        if (!file.exists()) return null

        val bmp = BitmapFactory.decodeFile(path) ?: return null
        val scaled = scaleBitmap(bmp)
        val stream = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 80, stream)
        val base64 = Base64.encodeToString(stream.toByteArray(), Base64.DEFAULT)
        bmp.recycle()
        scaled.recycle()
        return base64
    }

    private fun scaleBitmap(bitmap: Bitmap): Bitmap {
        val maxDim = 1920
        val ratio = max(bitmap.width, bitmap.height).toFloat() / maxDim
        if (ratio <= 1f) return bitmap
        val newW = (bitmap.width / ratio).toInt()
        val newH = (bitmap.height / ratio).toInt()
        return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
    }
}

// Data classes
data class CoverResult(val title: String)
data class SessionResumeResult(val session_id: String, val page_index: Int, val total_pages: Int = page_index + 1)