package com.example.storybridge_android.ui.session.start

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.storybridge_android.ServiceLocator
import com.example.storybridge_android.data.SessionRepository

class StartSessionViewModelFactory(
    private val repository: SessionRepository = ServiceLocator.sessionRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StartSessionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StartSessionViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
