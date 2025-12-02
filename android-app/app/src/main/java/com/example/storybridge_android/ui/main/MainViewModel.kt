package com.example.storybridge_android.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storybridge_android.data.UserRepository
import com.example.storybridge_android.data.SessionRepository
import com.example.storybridge_android.network.UserInfoResponse
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

    private val _startSessionResult = MutableStateFlow<Result<String>?>(null)
    val startSessionResult: StateFlow<Result<String>?> = _startSessionResult

    private val _discardResult = MutableStateFlow<Result<DiscardSessionResponse>?>(null)
    val discardResult: StateFlow<Result<DiscardSessionResponse>?> = _discardResult

    fun loadUserInfo(deviceInfo: String) {
        viewModelScope.launch {
            try {
                val response = userRepository.getUserInfo(deviceInfo)
                _userInfo.value = response
            } catch (e: Exception) {
                _userInfo.value = null
            }
        }
    }

    fun startSession(deviceId: String) {
        viewModelScope.launch {
            try {
                val result = sessionRepository.startSession(deviceId)
                val mapped = result.map { it.session_id }
                _startSessionResult.value = mapped
            } catch (e: Exception) {
                _startSessionResult.value = Result.failure(e)
            }
        }
    }

    fun discardSession(sessionId: String, deviceInfo: String) {
        viewModelScope.launch {
            try {
                val result = sessionRepository.discardSession(sessionId)
                _discardResult.value = result

                if (result.isSuccess) {
                    val response = userRepository.getUserInfo(deviceInfo)
                    _userInfo.value = response
                }
            } catch (e: Exception) {
                _discardResult.value = Result.failure(e)
            }
        }
    }
}
