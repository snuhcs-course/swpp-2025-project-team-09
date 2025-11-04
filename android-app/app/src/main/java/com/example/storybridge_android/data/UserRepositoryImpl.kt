package com.example.storybridge_android.data

import com.example.storybridge_android.network.*
import retrofit2.Response

class UserRepositoryImpl : UserRepository {
    override suspend fun login(request: UserLoginRequest): Response<UserLoginResponse> {
        return RetrofitClient.userApi.userLogin(request)
    }

    override suspend fun register(request: UserRegisterRequest): Response<UserRegisterResponse> {
        return RetrofitClient.userApi.userRegister(request)
    }

    override suspend fun getUserInfo(deviceInfo: String): Response<UserInfoResponse> {
        return RetrofitClient.userApi.userInfo(deviceInfo)
    }

    override suspend fun userLang(request: UserLangRequest): Response<UserLangResponse> {
        return RetrofitClient.userApi.userLang(request)
    }
}
