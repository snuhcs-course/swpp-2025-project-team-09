package com.example.storybridge_android.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storybridge_android.data.UserRepository
import com.example.storybridge_android.data.SessionRepository
import com.example.storybridge_android.network.UserInfoResponse
import com.example.storybridge_android.network.DiscardSessionResponse
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import retrofit2.Response

class MainViewModel(
    private val userRepository: UserRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {
    private val _userInfo = MutableStateFlow<Response<List<UserInfoResponse>>?>(null)
    val userInfo: StateFlow<Response<List<UserInfoResponse>>?> = _userInfo

    private val _startSessionEvent = MutableSharedFlow<Result<String>>(replay = 0)
    val startSessionEvent: SharedFlow<Result<String>> = _startSessionEvent

    private val _discardEvent = MutableSharedFlow<Result<DiscardSessionResponse>>(replay = 0)

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

    fun startSession(deviceInfo: String) {
        viewModelScope.launch {
            try {
                val result = sessionRepository.startSession(deviceInfo)
                val mapped = result.map { it.session_id }

                _startSessionEvent.emit(mapped)
            } catch (e: Exception) {
                _startSessionEvent.emit(Result.failure(e))
            }
        }
    }

    fun discardSession(sessionId: String, deviceInfo: String) {
        viewModelScope.launch {
            try {
                val result = sessionRepository.discardSession(sessionId)

                _discardEvent.emit(result)

                if (result.isSuccess) {
                    val response = userRepository.getUserInfo(deviceInfo)
                    _userInfo.value = response
                }
            } catch (e: Exception) {
                _discardEvent.emit(Result.failure(e))
            }
        }
    }
}
