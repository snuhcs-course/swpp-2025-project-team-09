package com.example.storybridge_android.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.storybridge_android.data.*

class LoadingViewModelFactory(
    private val processRepo: ProcessRepository,
    private val userRepo: UserRepository,
    private val sessionRepo: SessionRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoadingViewModel::class.java)) {
            return LoadingViewModel(processRepo, userRepo, sessionRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
