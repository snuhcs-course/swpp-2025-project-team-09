package com.example.storybridge_android.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storybridge_android.data.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StartSessionViewModel(
    private val repo: SessionRepository
) : ViewModel() {

    private val _state = MutableStateFlow<StartSessionUiState>(StartSessionUiState.Idle)
    val state = _state.asStateFlow()

    fun startSession(deviceId: String) {
        viewModelScope.launch {
            _state.value = StartSessionUiState.Loading
            val result = repo.startSession(deviceId)
            result.fold(
                onSuccess = { session ->
                    _state.value = StartSessionUiState.Success(session.session_id)
                },
                onFailure = { e ->
                    _state.value = StartSessionUiState.Error(e.message ?: "Failed to start session")
                }
            )
        }
    }
}

sealed class StartSessionUiState {
    data object Idle : StartSessionUiState()
    data object Loading : StartSessionUiState()
    data class Success(val sessionId: String) : StartSessionUiState()
    data class Error(val message: String) : StartSessionUiState()
}
