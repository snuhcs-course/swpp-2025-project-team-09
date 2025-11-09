package com.example.storybridge_android.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storybridge_android.data.UserRepository
import com.example.storybridge_android.network.UserInfoResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.Response

class MainViewModel(private val userRepository: UserRepository) : ViewModel() {
    private val _userInfo = MutableStateFlow<Response<List<UserInfoResponse>>?>(null)
    val userInfo: StateFlow<Response<List<UserInfoResponse>>?> = _userInfo

    fun loadUserInfo(deviceInfo: String) {
        viewModelScope.launch {
            val response = userRepository.getUserInfo(deviceInfo)
            _userInfo.value = response
        }
    }
}
