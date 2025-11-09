package com.example.storybridge_android.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.storybridge_android.data.SessionRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VoiceSelectViewModel(
    private val repo: SessionRepository
) : ViewModel() {

    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()

    private val _error = MutableSharedFlow<String>()
    val error = _error.asSharedFlow()

    private val _success = MutableSharedFlow<Unit>()
    val success = _success.asSharedFlow()

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
    }
}
