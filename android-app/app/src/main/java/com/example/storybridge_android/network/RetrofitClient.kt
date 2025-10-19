package com.example.storybridge_android.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    //const val BASE_URL = "http://10.0.2.2:8000"
    private const val BASE_URL = "https://flavia-mitotic-positively.ngrok-free.dev" // 사용할 때마다 바꿔줘야 하는듯


    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
    }

    val userApi: UserApi by lazy { retrofit.create(UserApi::class.java) }
    val sessionApi: SessionApi by lazy { retrofit.create(SessionApi::class.java) }
    val processApi: ProcessApi by lazy { retrofit.create(ProcessApi::class.java) }
    val pageApi: PageApi by lazy { retrofit.create(PageApi::class.java) }
}
