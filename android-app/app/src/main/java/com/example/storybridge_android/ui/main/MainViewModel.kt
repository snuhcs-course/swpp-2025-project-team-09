package com.example.storybridge_android.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storybridge_android.data.UserRepository
import com.example.storybridge_android.network.UserInfoResponse
import com.example.storybridge_android.data.SessionRepository
import com.example.storybridge_android.network.DiscardSessionResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.Response

class MainViewModel(
    private val userRepository: UserRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {
    private val _userInfo = MutableStateFlow<Response<List<UserInfoResponse>>?>(null)
    val userInfo: StateFlow<Response<List<UserInfoResponse>>?> = _userInfo

    private val _discardResult = MutableStateFlow<Result<DiscardSessionResponse>?>(null)
    val discardResult: StateFlow<Result<DiscardSessionResponse>?> = _discardResult

    fun loadUserInfo(deviceInfo: String) {
        viewModelScope.launch {
            val response = userRepository.getUserInfo(deviceInfo)
            _userInfo.value = response
        }
    }

    fun discardSession(sessionId: String, deviceInfo: String) {
        viewModelScope.launch {
            val result = sessionRepository.discardSession(sessionId)
            _discardResult.value = result
            if (result.isSuccess) {
                // 삭제 성공 시 user info 다시 불러오기
                val response = userRepository.getUserInfo(deviceInfo)
                _userInfo.value = response
            }
        }
    }
}
