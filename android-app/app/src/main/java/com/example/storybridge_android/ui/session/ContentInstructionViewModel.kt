package com.example.storybridge_android.ui.session

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ContentInstructionViewModel : ViewModel() {

    private var sessionId: String? = null

    private val _navigateToCamera = MutableLiveData<Boolean>()
    val navigateToCamera: LiveData<Boolean> = _navigateToCamera

    fun setSessionId(id: String) {
        sessionId = id
    }

    fun onStartClicked() {
        if (sessionId != null) {
            _navigateToCamera.value = true
        } else {
            _navigateToCamera.value = false
        }
    }
}