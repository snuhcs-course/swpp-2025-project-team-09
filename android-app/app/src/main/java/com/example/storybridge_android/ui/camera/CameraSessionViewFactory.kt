package com.example.storybridge_android.ui.camera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class CameraSessionViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CameraSessionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CameraSessionViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
