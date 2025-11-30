package com.example.storybridge_android.ui.landing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storybridge_android.data.UserRepository
import com.example.storybridge_android.network.UserLoginRequest
import com.example.storybridge_android.network.UserRegisterRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class LandingUiState {
    object Loading : LandingUiState()
    object NavigateMain : LandingUiState()
    object ShowLanguageSelect : LandingUiState()
    data class Error(val message: String) : LandingUiState()
}

class LandingViewModel(private val repository: UserRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<LandingUiState>(LandingUiState.Loading)
    val uiState: StateFlow<LandingUiState> = _uiState

    fun checkUser(deviceId: String) {
        viewModelScope.launch {
            try {
                /*
                val loginRes = repository.login(UserLoginRequest(deviceId))
                if (loginRes.isSuccessful && loginRes.body() != null) {
                    _uiState.value = LandingUiState.NavigateMain
                } else {
                    val regRes = repository.register(UserRegisterRequest(deviceId, "en"))
                    if (regRes.isSuccessful) {
                        _uiState.value = LandingUiState.ShowLanguageSelect
                    } else {
                        _uiState.value = LandingUiState.Error("Register failed")
                    }
                }

                 */
                val loginRes = repository.login(UserLoginRequest(deviceId))
                if (loginRes.isSuccessful && loginRes.body() != null) {
                    _uiState.value = LandingUiState.ShowLanguageSelect
                } else {
                    repository.register(UserRegisterRequest(deviceId, "en"))
                    _uiState.value = LandingUiState.ShowLanguageSelect
                }
            } catch (e: Exception) {
                _uiState.value = LandingUiState.Error(e.message ?: "Network error")
            }
        }
    }
}
