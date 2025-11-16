package com.example.storybridge_android.network

import com.google.gson.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit

class FlexibleUserInfoAdapter : JsonDeserializer<UserInfoResponse> {
    private val plainGson = Gson()

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): UserInfoResponse {
        return if (json.isJsonArray) {
            val array = json.asJsonArray
            plainGson.fromJson(array[0], UserInfoResponse::class.java)
        } else {
            plainGson.fromJson(json, UserInfoResponse::class.java)
        }
    }
}

object RetrofitClient {
    private var BASE_URL = "http://ec2-3-36-206-206.ap-northeast-2.compute.amazonaws.com:8000"
    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private val gson = GsonBuilder()
        .registerTypeAdapter(UserInfoResponse::class.java, FlexibleUserInfoAdapter())
        .create()

    private fun createRetrofit(url: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create(gson))
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
