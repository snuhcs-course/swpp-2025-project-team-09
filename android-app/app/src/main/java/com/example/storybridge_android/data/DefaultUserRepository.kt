package com.example.storybridge_android.data

import com.example.storybridge_android.network.RetrofitClient
import com.example.storybridge_android.network.UserLoginRequest
import com.example.storybridge_android.network.UserLoginResponse
import com.example.storybridge_android.network.UserRegisterRequest
import com.example.storybridge_android.network.UserRegisterResponse
import retrofit2.Response

class DefaultUserRepository : UserRepository {
    override suspend fun login(request: UserLoginRequest): Response<UserLoginResponse> {
        return RetrofitClient.userApi.userLogin(request)
    }

    override suspend fun register(request: UserRegisterRequest): Response<UserRegisterResponse> {
        return RetrofitClient.userApi.userRegister(request)
    }
}
