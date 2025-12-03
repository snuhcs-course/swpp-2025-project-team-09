package com.example.storybridge_android.ui.session.instruction

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storybridge_android.data.SessionRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class ContentInstructionViewModel(
    private val sessionRepo: SessionRepository
) : ViewModel() {

    private var sessionId: String? = null

    private val _navigateToCamera = MutableLiveData<Boolean>()
    val navigateToCamera: LiveData<Boolean> = _navigateToCamera

    private val _discardSuccess = MutableSharedFlow<Unit>()
    val discardSuccess = _discardSuccess.asSharedFlow()

    fun setSessionId(id: String) {
        sessionId = id
    }

    fun onStartClicked() {
        _navigateToCamera.value = sessionId != null
    }

    fun discardSession() {
        val id = sessionId ?: return
        viewModelScope.launch {
            sessionRepo.discardSession(id)
            _discardSuccess.emit(Unit)
        }
    }
}
