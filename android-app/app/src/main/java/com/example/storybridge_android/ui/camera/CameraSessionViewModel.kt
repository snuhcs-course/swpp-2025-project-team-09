package com.example.storybridge_android.ui.camera

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class CameraSessionViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<SessionUiState>(SessionUiState.Idle)
    val uiState = _uiState.asStateFlow()

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
}

sealed class SessionUiState {
    data object Idle : SessionUiState()
    data class Success(val imagePath: String) : SessionUiState()
    data object Cancelled : SessionUiState()
    data class Error(val message: String) : SessionUiState()
}
