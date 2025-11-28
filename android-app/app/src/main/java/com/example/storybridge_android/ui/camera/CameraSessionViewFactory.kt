package com.example.storybridge_android.ui.camera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.storybridge_android.ServiceLocator
import com.example.storybridge_android.data.ProcessRepository
import com.example.storybridge_android.data.SessionRepository

class CameraSessionViewModelFactory(
    private val sessionRepository: SessionRepository = ServiceLocator.sessionRepository,
    private val processRepository: ProcessRepository = ServiceLocator.processRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CameraSessionViewModel::class.java)) {
            return CameraSessionViewModel(sessionRepository, processRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}