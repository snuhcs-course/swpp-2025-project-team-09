package com.example.storybridge_android.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.storybridge_android.data.SessionRepository

class StartSessionViewModelFactory(
    private val repo: SessionRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StartSessionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StartSessionViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
