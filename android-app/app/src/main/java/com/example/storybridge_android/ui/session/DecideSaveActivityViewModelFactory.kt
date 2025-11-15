package com.example.storybridge_android.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.storybridge_android.data.SessionRepositoryImpl

class DecideSaveActivityViewModelFactory(
    private val repository: SessionRepositoryImpl
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DecideSaveActivityViewModel::class.java)) {
            return DecideSaveActivityViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
