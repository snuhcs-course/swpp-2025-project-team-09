package com.example.storybridge_android.ui.session.voice

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storybridge_android.data.ProcessRepository
import com.example.storybridge_android.data.SessionRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VoiceSelectViewModel(
    private val sessionRepo: SessionRepository,
    private val processRepo: ProcessRepository
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
                val result = sessionRepo.selectVoice(sessionId, voice)
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
}
