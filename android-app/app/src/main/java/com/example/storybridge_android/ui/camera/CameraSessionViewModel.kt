package com.example.storybridge_android.ui.camera

import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storybridge_android.data.ProcessRepository
import com.example.storybridge_android.data.SessionRepository
import com.example.storybridge_android.network.UploadCoverResponse
import com.example.storybridge_android.network.UploadImageRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class CameraSessionViewModel(
    private val sessionRepository: SessionRepository,
    private val processRepository: ProcessRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SessionUiState>(SessionUiState.Idle)
    val uiState = _uiState.asStateFlow()

    companion object {
        private const val TAG = "CameraSessionViewModel"
    }

    fun uploadCoverImage(sessionId: String, lang: String, imagePath: String) {
        viewModelScope.launch {
            _uiState.value = SessionUiState.UploadLoading

            try {
                Log.d(TAG, "Starting cover upload for session: $sessionId")

                val imageFile = File(imagePath)
                val bytes = imageFile.readBytes()
                val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)

                val request = UploadImageRequest(
                    session_id = sessionId,
                    page_index = 0,
                    lang = lang,
                    image_base64 = base64
                )

                val result = processRepository.uploadCoverImage(request)

                result.fold(
                    onSuccess = { response ->
                        _uiState.value = SessionUiState.UploadSuccess(response)
                    },
                    onFailure = { error ->
                        if (error.message?.contains("422") == true) {
                            _uiState.value = SessionUiState.NoTextDetected
                        } else {
                            _uiState.value = SessionUiState.Error(error.message ?: "Upload failed")
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Exception during cover upload", e)
                _uiState.value = SessionUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun handleCameraResult(resultCode: Int, imagePath: String?) {
        Log.d(TAG, "handleCameraResult: resultCode=$resultCode, imagePath=$imagePath, currentState=${_uiState.value}")

        when {
            resultCode == android.app.Activity.RESULT_OK && !imagePath.isNullOrEmpty() -> {
                _uiState.value = SessionUiState.Success(imagePath)
            }
            resultCode == android.app.Activity.RESULT_CANCELED -> {
                _uiState.value = SessionUiState.Cancelled
            }
            else -> {
                _uiState.value = SessionUiState.Error("Failed to capture image")
            }
        }

        Log.d(TAG, "handleCameraResult: newState=${_uiState.value}")
    }

    // Call this before relaunching camera to ensure state transitions properly
    fun resetState() {
        Log.d(TAG, "resetState called, transitioning from ${_uiState.value} to Idle")
        _uiState.value = SessionUiState.Idle
    }

    fun discardSession(sessionId: String) {
        viewModelScope.launch {
            try {
                sessionRepository.discardSession(sessionId)
                Log.d(TAG, "Session discarded: $sessionId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to discard session", e)
            }
        }
    }
}

sealed class SessionUiState {
    data object Idle : SessionUiState()
    data object UploadLoading : SessionUiState()
    data class UploadSuccess(val response: UploadCoverResponse) : SessionUiState()
    data object NoTextDetected : SessionUiState()
    data class Success(val imagePath: String) : SessionUiState()
    data object Cancelled : SessionUiState()
    data class Error(val message: String) : SessionUiState()
}