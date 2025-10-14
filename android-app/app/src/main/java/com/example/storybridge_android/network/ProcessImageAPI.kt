package com.example.storybridge_android.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ProcessImageAPI {
    @Multipart
    @POST("/api/process_image/") // 백엔드 라우팅과 동일하게!
    fun uploadImage(
        @Part image: MultipartBody.Part,
        @Part("lang") lang: RequestBody?,
        @Part("uid") uid: RequestBody?,
        @Part("book_title") bookTitle: RequestBody?
    ): Call<ProcessResponse>
}
