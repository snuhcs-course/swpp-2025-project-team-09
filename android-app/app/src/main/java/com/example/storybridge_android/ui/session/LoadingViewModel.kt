package com.example.storybridge_android.ui.session

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storybridge_android.network.*
import com.example.storybridge_android.data.ProcessRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max

data class CoverResult(val title: String, val maleTts: String?, val femaleTts: String?)

class LoadingViewModel(private val repo: ProcessRepository) : ViewModel() {

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

    private var rampJob: Job? = null

    fun uploadImage(sessionId: String, pageIndex: Int, lang: String, path: String) {
        viewModelScope.launch {
            _status.value = "uploading"
            startRampTo(80, 12000L)
            val base64 = encodeBase64(path)
            if (base64 == null) {
                stopRamp()
                _error.value = "Failed to process image"
                return@launch
            }

            val req = UploadImageRequest(sessionId, pageIndex, lang, base64)
            repo.uploadImage(req).fold(
                onSuccess = {
                    stopRamp()
                    _progress.value = 80
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
            startRampTo(80, 12000L)

            val base64 = encodeBase64(path)
            if (base64 == null) {
                stopRamp()
                _error.value = "Failed to process image"
                return@launch
            }

            val req = UploadImageRequest(sessionId, 0, lang, base64)
            repo.uploadCoverImage(req).fold(
                onSuccess = {
                    stopRamp()
                    _progress.value = 100

                    val result = CoverResult(
                        it.title,
                        it.tts_male.takeIf { t -> t.isNotEmpty() },
                        it.tts_female.takeIf { t -> t.isNotEmpty() }
                    )

                    _cover.value = result
                    _navigateToVoice.value = result  // Activity에서 collect해서 VoiceSelectActivity로 이동
                    _status.value = "cover_ready"
                },
                onFailure = {
                    stopRamp()
                    _error.value = it.message
                }
            )
        }
    }

    private suspend fun pollOcr(sessionId: String, pageIndex: Int) {
        _status.value = "polling"
        repeat(60) {
            val res = repo.checkOcrStatus(sessionId, pageIndex)
            var done = false
            res.fold(
                onSuccess = {
                    val p = it.progress
                    _progress.value = 80 + (p * 20 / 100)
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
