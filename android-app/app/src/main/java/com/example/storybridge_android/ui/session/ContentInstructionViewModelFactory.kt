package com.example.storybridge_android.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class ContentInstructionViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ContentInstructionViewModel::class.java)) {
            return ContentInstructionViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}