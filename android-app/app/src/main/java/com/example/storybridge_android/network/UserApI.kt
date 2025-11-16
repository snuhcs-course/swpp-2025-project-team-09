package com.example.storybridge_android.network

import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query

// Retrofit API Interface
interface UserApi {
    @POST("/user/register")
    suspend fun userRegister(
        @Body request: UserRegisterRequest
    ): Response<UserRegisterResponse>

    @POST("/user/login")
    suspend fun userLogin(
        @Body request: UserLoginRequest
    ): Response<UserLoginResponse>

    @PATCH("/user/lang")
    suspend fun userLang(
        @Body request: UserLangRequest
    ): Response<UserLangResponse>

    @GET("/user/info")
    suspend fun userInfo(
        @Query("device_info") deviceInfo: String
    ): Response<List<UserInfoResponse>>
}

// --------------------
// Request / Response data classes
// --------------------

// 1-1. Register
data class UserRegisterRequest(
    val device_info: String,
    val language_preference: String
)

data class UserRegisterResponse(
    val user_id: String,
    val language_preference: String
)

// 1-2. Login
data class UserLoginRequest(
    val device_info: String
)

data class UserLoginResponse(
    val user_id: String,
    val language_preference: String
)

// 1-3. Change Language Preference
data class UserLangRequest(
    val device_info: String,
    val language_preference: String
)

data class UserLangResponse(
    val user_id: String,
    val language_preference: String,
    val updated_at: String // datetime as ISO string
)

// 1-4. Get user's full reading list
data class UserInfoRequest(
    val device_info: String
)


data class UserInfoResponse(
    val user_id: String,
    val session_id: String,
    val title: String,
    val translated_title: String,
    val image_base64: String,
    val started_at: String  // datetime as ISO string
)

typealias UserInfoListResponse = List<UserInfoResponse>