package com.example.storybridge_android.ui.session

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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max

data class CoverResult(val title: String, val maleTts: String?, val femaleTts: String?)
data class SessionResumeResult(val session_id: String, val page_index: Int, val total_pages: Int = page_index + 1)

class LoadingViewModel(
    private val processRepo: ProcessRepository,
    private val pageRepo: PageRepository,
    private val userRepo: UserRepository,
    private val sessionRepo: SessionRepository
) : ViewModel() {

    private val _progress = MutableStateFlow(0)
    val progress = _progress.asStateFlow()

    private val _status = MutableStateFlow("idle")
    val status = _status.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _cover = MutableStateFlow<CoverResult?>(null)
    val cover = _cover.asStateFlow()

    private val _navigateToVoice = MutableStateFlow<CoverResult?>(null)
    val navigateToVoice = _navigateToVoice.asStateFlow()

    private val _navigateToReading = MutableStateFlow<SessionResumeResult?>(null)
    val navigateToReading = _navigateToReading.asStateFlow()

    private val _userInfo = MutableStateFlow<retrofit2.Response<List<UserInfoResponse>>?>(null)
    val userInfo = _userInfo.asStateFlow()

    private var rampJob: Job? = null

    // ---------------- 기존 업로드 ----------------
    fun uploadImage(sessionId: String, pageIndex: Int, lang: String, path: String) {
        viewModelScope.launch {
            _status.value = "uploading"

            val base64 = encodeBase64(path)
            if (base64 == null) {
                _error.value = "Failed to process image"
                return@launch
            }
            startRampTo(40, 2000L)

            val req = UploadImageRequest(sessionId, pageIndex, lang, base64)
            processRepo.uploadImage(req).fold(
                onSuccess = {
                    pollOcr(sessionId, it.page_index)
                },
                onFailure = {
                    stopRamp()
                    _error.value = it.message
                }
            )
        }
    }

    fun uploadCover(sessionId: String, lang: String, path: String) {
        viewModelScope.launch {
            _status.value = "uploading_cover"

            val base64 = encodeBase64(path)
            if (base64 == null) {
                _error.value = "Failed to process image"
                return@launch
            }
            startRampTo(40, 2000L)

            val req = UploadImageRequest(sessionId, 0, lang, base64)
            processRepo.uploadCoverImage(req).fold(
                onSuccess = {
                    stopRamp()
                    _progress.value = 100

                    val result = CoverResult(
                        it.title,
                        it.tts_male.takeIf { t -> t.isNotEmpty() },
                        it.tts_female.takeIf { t -> t.isNotEmpty() }
                    )
                    _cover.value = result
                    _navigateToVoice.value = result
                    _status.value = "cover_ready"
                },
                onFailure = {
                    stopRamp()
                    _error.value = it.message
                }
            )
        }
    }

    // ---------------- OCR Polling ----------------
    /*
    private suspend fun pollOcr(sessionId: String, pageIndex: Int) {
        _status.value = "polling"
        repeat(60) {
            val res = processRepo.checkOcrStatus(sessionId, pageIndex)
            var done = false
            res.fold(
                onSuccess = {
                    val p = it.progress
                    _progress.value = 40 + (p * 60 / 100)
                    if (it.status == "ready") {
                        _progress.value = 100
                        _status.value = "ready"
                        done = true
                    }
                },
                onFailure = { _error.value = it.message }
            )
            if (done) return
            delay(1000)
        }
        _error.value = "Timeout while waiting for OCR"
    }
     */
    private suspend fun pollOcr(sessionId: String, pageIndex: Int) {
        _status.value = "polling"

        if (_progress.value < 41) _progress.value = 41

        var ocrStarted = false

        repeat(60) { i ->
            val res = processRepo.checkOcrStatus(sessionId, pageIndex)
            var done = false

            res.fold(
                onSuccess = {
                    ocrStarted = true

                    val p = it.progress
                    val mapped = 40 + (p * 60 / 100)
                    _progress.value = mapped.coerceIn(41, 99)

                    if (it.status == "ready") {
                        _progress.value = 100
                        _status.value = "ready"
                        done = true
                    }
                },
                onFailure = {
                    _error.value = it.message
                }
            )
            if (done) return

            if (!ocrStarted) {
                val next = (_progress.value + 1).coerceAtMost(49)
                _progress.value = next
            }

            delay(300)
        }

        _error.value = "Timeout while waiting for OCR"
    }

    // ---------------- Progress ----------------
    private fun startRampTo(target: Int, durationMs: Long) {
        stopRamp()
        rampJob = viewModelScope.launch {
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

    // ---------------- Bitmap ----------------
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

    // ---------------- 사용자 정보 ----------------
    fun loadUserInfo(deviceInfo: String) {
        viewModelScope.launch {
            val response = userRepo.getUserInfo(deviceInfo)
            _userInfo.value = response
        }
    }

    // ---------------- 이어보기 ----------------
    fun reloadAllSession(startedAt: String, context: Context) {
        viewModelScope.launch {
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

}