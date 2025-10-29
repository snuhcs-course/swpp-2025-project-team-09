package com.example.storybridge_android.ui.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storybridge_android.data.UserRepository
import com.example.storybridge_android.network.UserLangRequest
import com.example.storybridge_android.network.UserLangResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.Response

class SettingViewModel(private val userRepository: UserRepository) : ViewModel() {
    private val _langResponse = MutableStateFlow<Response<UserLangResponse>?>(null)
    val langResponse: StateFlow<Response<UserLangResponse>?> = _langResponse

    fun updateLanguage(request: UserLangRequest) {
        viewModelScope.launch {
            val response = userRepository.userLang(request)
            _langResponse.value = response
        }
    }
}
