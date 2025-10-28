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

    /*
    @POST("/user/register")
    fun userRegister(
        @Body request: UserRegisterRequest
    ): Call<UserRegisterResponse>

    @POST("/user/login")
    fun userLogin(
        @Body request: UserLoginRequest
    ): Call<UserLoginResponse>
     */
    @POST("/user/register")
    suspend fun userRegister(
        @Body request: UserRegisterRequest
    ): Response<UserRegisterResponse>

    @POST("/user/login")
    suspend fun userLogin(
        @Body request: UserLoginRequest
    ): Response<UserLoginResponse>


    @PATCH("/user/lang")
    fun userLang(
        @Body request: UserLangRequest
    ): Call<UserLangResponse>

    @GET("/user/info")
    fun userInfo(
        @Query("device_info") device_info: String
    ): Call<UserInfoResponse>
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
    val title: String,
    val image_base64: String,
    val started_at: String // datetime as ISO string
)
