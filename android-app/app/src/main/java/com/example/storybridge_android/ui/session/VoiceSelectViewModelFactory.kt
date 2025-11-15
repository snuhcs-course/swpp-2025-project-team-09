package com.example.storybridge_android.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.storybridge_android.ServiceLocator
import com.example.storybridge_android.data.SessionRepository
import com.example.storybridge_android.data.ProcessRepository

class VoiceSelectViewModelFactory(
    private val sessionRepo: SessionRepository = ServiceLocator.sessionRepository,
    private val processRepo: ProcessRepository = ServiceLocator.processRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VoiceSelectViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return VoiceSelectViewModel(sessionRepo, processRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
