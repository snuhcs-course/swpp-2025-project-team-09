package com.example.storybridge_android.ui.session

import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.storybridge_android.data.ProcessRepositoryImpl
import com.example.storybridge_android.data.SessionRepository
import com.example.storybridge_android.network.UploadImageRequest
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class VoiceSelectViewModel(
    private val repo: SessionRepository
) : ViewModel() {

    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()

    private val _error = MutableSharedFlow<String>()
    val error = _error.asSharedFlow()

    private val _success = MutableSharedFlow<Unit>()
    val success = _success.asSharedFlow()

    private val processRepo = ProcessRepositoryImpl()

    fun selectVoice(sessionId: String, voice: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val result = repo.selectVoice(sessionId, voice)
                result.fold(
                    onSuccess = {
                        _success.emit(Unit)
                    },
                    onFailure = {
                        _error.emit(it.message ?: "Voice selection failed")
                    }
                )
            } catch (e: Exception) {
                _error.emit(e.message ?: "Unexpected error")
            } finally {
                _loading.value = false
            }
        }
        Log.d("VoiceSelection", "Selecting voice=$voice for session=$sessionId")
    }

    fun uploadCoverInBackground(sessionId: String, lang: String, imagePath: String) {
        viewModelScope.launch {
            try {
                Log.d("VoiceSelectViewModel", "Starting background cover upload")

                // 이미지를 Base64로 인코딩
                val imageFile = File(imagePath)
                val bytes = imageFile.readBytes()
                val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)

                // 서버에 업로드
                val request = UploadImageRequest(sessionId, 0, lang, base64)
                processRepo.uploadCoverImage(request).fold(
                    onSuccess = {
                        Log.d("VoiceSelectViewModel", "✓ Cover uploaded successfully in background")
                        Log.d("VoiceSelectViewModel", "Title: ${it.title}")
                    },
                    onFailure = {
                        Log.e("VoiceSelectViewModel", "✗ Cover upload failed: ${it.message}")
                    }
                )
            } catch (e: Exception) {
                Log.e("VoiceSelectViewModel", "✗ Error during cover upload: ${e.message}", e)
            }
        }
    }
}
