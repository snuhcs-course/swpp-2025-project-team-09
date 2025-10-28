package com.example.storybridge_android.data

import com.example.storybridge_android.network.UserLoginRequest
import com.example.storybridge_android.network.UserLoginResponse
import com.example.storybridge_android.network.UserRegisterRequest
import com.example.storybridge_android.network.UserRegisterResponse
import retrofit2.Response

interface UserRepository {
    suspend fun login(request: UserLoginRequest): Response<UserLoginResponse>
    suspend fun register(request: UserRegisterRequest): Response<UserRegisterResponse>
}
