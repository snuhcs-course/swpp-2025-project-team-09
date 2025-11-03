package com.example.storybridge_android.data

import com.example.storybridge_android.network.*
import retrofit2.Response

interface UserRepository {
    suspend fun login(request: UserLoginRequest): Response<UserLoginResponse>
    suspend fun register(request: UserRegisterRequest): Response<UserRegisterResponse>
    suspend fun getUserInfo(deviceInfo: String): Response<UserInfoResponse>
    suspend fun userLang(request: UserLangRequest): Response<UserLangResponse>
}