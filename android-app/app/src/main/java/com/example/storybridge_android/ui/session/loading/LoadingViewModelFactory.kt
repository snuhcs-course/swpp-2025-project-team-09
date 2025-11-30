package com.example.storybridge_android.ui.session.loading

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.storybridge_android.ServiceLocator
import com.example.storybridge_android.data.ProcessRepository
import com.example.storybridge_android.data.UserRepository
import com.example.storybridge_android.data.SessionRepository

class LoadingViewModelFactory(
    private val processRepo: ProcessRepository = ServiceLocator.processRepository,
    private val userRepo: UserRepository = ServiceLocator.userRepository,
    private val sessionRepo: SessionRepository = ServiceLocator.sessionRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoadingViewModel::class.java)) {
            return LoadingViewModel(processRepo, userRepo, sessionRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}