package com.example.storybridge_android.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.storybridge_android.ServiceLocator

class DecideSaveViewModelFactory(
    private val repo: com.example.storybridge_android.data.SessionRepository =
        ServiceLocator.sessionRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DecideSaveViewModel::class.java)) {
            return DecideSaveViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
