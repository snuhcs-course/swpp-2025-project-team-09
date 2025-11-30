package com.example.storybridge_android.ui.tutorial

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storybridge_android.data.UserRepository
import com.example.storybridge_android.network.UserLoginRequest
import com.example.storybridge_android.network.UserRegisterRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class TutorialUiState {
    object Idle : TutorialUiState()
    object Loading : TutorialUiState()
    object NavigateMain : TutorialUiState()
    object NavigateLanguageSelect : TutorialUiState()
    data class Error(val message: String) : TutorialUiState()
}

class TutorialViewModel(private val repository: UserRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<TutorialUiState>(TutorialUiState.Idle)
    val uiState: StateFlow<TutorialUiState> = _uiState

    fun startApp(deviceId: String, lang: String) {
        viewModelScope.launch {
            _uiState.value = TutorialUiState.Loading

            try {
                val loginRes = repository.login(UserLoginRequest(deviceId))
                if (loginRes.isSuccessful && loginRes.body() != null) {
                    _uiState.value = TutorialUiState.NavigateMain
                } else {
                    val regRes = repository.register(UserRegisterRequest(deviceId, lang))
                    if (regRes.isSuccessful) {
                        _uiState.value = TutorialUiState.NavigateLanguageSelect
                    } else {
                        _uiState.value = TutorialUiState.Error("Register failed")
                    }
                }
            } catch (e: Exception) {
                _uiState.value = TutorialUiState.Error(e.message ?: "Network error")
            }
        }
    }
}