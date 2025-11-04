package com.example.storybridge_android.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.storybridge_android.data.ProcessRepository

class LoadingViewModelFactory(
    private val repo: ProcessRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoadingViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LoadingViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
