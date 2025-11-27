package com.example.storybridge_android.ui.camera
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storybridge_android.data.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CameraSessionViewModel(
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SessionUiState>(SessionUiState.Idle)
    val uiState = _uiState.asStateFlow()

    companion object {
        private const val TAG = "CameraSessionViewModel"
    }

    fun handleCameraResult(resultCode: Int, imagePath: String?) {
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
    }

    fun resetState() {
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
    data class Success(val imagePath: String) : SessionUiState()
    data object Cancelled : SessionUiState()
    data class Error(val message: String) : SessionUiState()
}