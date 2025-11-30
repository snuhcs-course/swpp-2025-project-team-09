package com.example.storybridge_android.ui.tutorial // 패키지 확인

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storybridge_android.data.UserRepository
import com.example.storybridge_android.network.UserLoginRequest
import com.example.storybridge_android.network.UserRegisterRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// 상태 정의
sealed class TutorialUiState {
    object Idle : TutorialUiState()            // 아무것도 안 한 상태
    object Loading : TutorialUiState()         // 로딩 중 (서버 통신 중)
    object NavigateMain : TutorialUiState()    // 메인으로 이동
    object NavigateLanguageSelect : TutorialUiState() // 언어 선택으로 이동 (필요하다면)
    data class Error(val message: String) : TutorialUiState()
}

class TutorialViewModel(private val repository: UserRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<TutorialUiState>(TutorialUiState.Idle)
    val uiState: StateFlow<TutorialUiState> = _uiState

    // Start 버튼 누를 때 호출할 함수
    fun startApp(deviceId: String) {
        viewModelScope.launch {
            _uiState.value = TutorialUiState.Loading

            try {
                // 1. 로그인 시도
                val loginRes = repository.login(UserLoginRequest(deviceId))
                if (loginRes.isSuccessful && loginRes.body() != null) {
                    _uiState.value = TutorialUiState.NavigateMain
                } else {
                    // 2. 로그인 실패 시 회원가입 시도
                    // (기본 언어를 "en"으로 설정하거나, 튜토리얼에서 선택한 값을 넣을 수도 있음)
                    val regRes = repository.register(UserRegisterRequest(deviceId, "en"))

                    if (regRes.isSuccessful) {
                        // 회원가입 성공 -> 언어 선택 화면 or 메인으로
                        // 기존 로직대로라면 언어 선택 화면으로?
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