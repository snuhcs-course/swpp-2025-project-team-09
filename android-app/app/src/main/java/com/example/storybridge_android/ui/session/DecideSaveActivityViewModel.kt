package com.example.storybridge_android.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.example.storybridge_android.data.SessionRepositoryImpl

class DecideSaveActivityViewModel(
    private val repository: SessionRepositoryImpl
) : ViewModel() {

    private val _state = MutableStateFlow<DecideSaveUiState>(DecideSaveUiState.Idle)
    val state: StateFlow<DecideSaveUiState> = _state

    fun saveSession() {
        _state.value = DecideSaveUiState.Saved
    }

    fun discardSession(sessionId: String) {
        _state.value = DecideSaveUiState.Loading

        viewModelScope.launch {
            val result = repository.discardSession(sessionId)
            if (result.isSuccess) {
                _state.value = DecideSaveUiState.Discarded
            } else {
                _state.value = DecideSaveUiState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
            }
        }
    }
}

sealed class DecideSaveUiState {
    object Idle : DecideSaveUiState()
    object Loading : DecideSaveUiState()
    object Saved : DecideSaveUiState()
    object Discarded : DecideSaveUiState()
    data class Error(val message: String) : DecideSaveUiState()
}
