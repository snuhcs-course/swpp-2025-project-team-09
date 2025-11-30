package com.example.storybridge_android.ui.session.decide

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.storybridge_android.ServiceLocator
import com.example.storybridge_android.data.SessionRepository

class DecideSaveActivityViewModelFactory(
    private val repository: SessionRepository = ServiceLocator.sessionRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DecideSaveActivityViewModel::class.java)) {
            return DecideSaveActivityViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
