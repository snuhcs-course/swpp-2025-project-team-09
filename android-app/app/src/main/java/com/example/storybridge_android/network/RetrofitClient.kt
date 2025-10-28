package com.example.storybridge_android.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private var BASE_URL = "https://flavia-mitotic-positively.ngrok-free.dev"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private fun createRetrofit(url: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
    }

    @Volatile private var retrofit: Retrofit = createRetrofit(BASE_URL)
    @Volatile var userApi: UserApi = retrofit.create(UserApi::class.java)
    @Volatile var sessionApi: SessionApi = retrofit.create(SessionApi::class.java)
    @Volatile var processApi: ProcessApi = retrofit.create(ProcessApi::class.java)
    @Volatile var pageApi: PageApi = retrofit.create(PageApi::class.java)

    @Synchronized
    fun overrideBaseUrl(url: String) {
        BASE_URL = url
        retrofit = createRetrofit(BASE_URL)
        userApi = retrofit.create(UserApi::class.java)
        sessionApi = retrofit.create(SessionApi::class.java)
        processApi = retrofit.create(ProcessApi::class.java)
        pageApi = retrofit.create(PageApi::class.java)
    }
}
