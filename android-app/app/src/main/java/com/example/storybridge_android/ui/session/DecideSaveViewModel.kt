package com.example.storybridge_android.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storybridge_android.data.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class DecideSaveUiState {
    object Idle : DecideSaveUiState()
    object Saving : DecideSaveUiState()
    object SaveSuccess : DecideSaveUiState()
    data class SaveError(val msg: String) : DecideSaveUiState()

    object Discarding : DecideSaveUiState()
    object DiscardSuccess : DecideSaveUiState()
    data class DiscardError(val msg: String) : DecideSaveUiState()
}

class DecideSaveViewModel(
    private val repo: SessionRepository
) : ViewModel() {

    private val _state = MutableStateFlow<DecideSaveUiState>(DecideSaveUiState.Idle)
    val state = _state.asStateFlow()

    fun save() {
        _state.value = DecideSaveUiState.Saving
        _state.value = DecideSaveUiState.SaveSuccess
    }

    fun discard(sessionId: String) {
        _state.value = DecideSaveUiState.Discarding

        viewModelScope.launch {
            val result = repo.discardSession(sessionId)
            if (result.isSuccess) {
                _state.value = DecideSaveUiState.DiscardSuccess
            } else {
                val msg = result.exceptionOrNull()?.message ?: "Unknown error"
                _state.value = DecideSaveUiState.DiscardError(msg)
            }
        }
    }
}
