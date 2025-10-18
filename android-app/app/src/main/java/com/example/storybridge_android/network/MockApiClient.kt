package com.example.storybridge_android.network
// MockApiClient.kt

import okio.Timeout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.mock.Calls

object MockApiClient : ProcessApi, UserApi {

    private var fakeProgress = 0

    // --- ProcessApi 모킹 ---
    override fun uploadImage(request: UploadImageRequest): Call<UploadImageResponse> {
        val fakeResponse = UploadImageResponse(
            session_id = request.session_id,
            page_index = request.page_index,
            status = "ready",
            submitted_at = "2025-10-18T00:00:00Z"
        )
        return Calls.response(fakeResponse)
    }

    override fun checkOcrTranslationStatus(session_id: String, page_index: Int): Call<CheckOcrTranslationResponse> {
        fakeProgress = (fakeProgress + 20).coerceAtMost(100)
        val status = if (fakeProgress >= 100) "ready" else "processing"

        val fakeResponse = CheckOcrTranslationResponse(
            session_id = session_id,
            page_index = page_index,
            status = status,
            progress = fakeProgress,
            submitted_at = "2025-10-18T00:00:00Z",
            processed_at = if (status == "ready") "2025-10-18T00:00:05Z" else null
        )
        return Calls.response(fakeResponse)
    }

    override fun checkTtsStatus(session_id: String, page_index: Int): Call<CheckTtsResponse> {
        val fakeResponse = CheckTtsResponse(
            session_id = session_id,
            page_index = page_index,
            status = "ready",
            progress = 100,
            submitted_at = "2025-10-18T00:00:00Z",
            processed_at = "2025-10-18T00:00:07Z"
        )
        return Calls.response(fakeResponse)
    }

    // --- UserApi 모킹 ---
    override fun userInfo(device_info: String): Call<UserInfoResponse> {
        val fakeResponse = UserInfoResponse(
            user_id = "mock_user_001",
            title = "The Very Hungry Caterpillar",
            image_base64 = "<base64 샘플 or 빈 문자열>",
            started_at = "2025-10-18T10:00:00Z"
        )
        return Calls.response(fakeResponse)
    }

    override fun userLogin(request: UserLoginRequest): Call<UserLoginResponse> {
        val fakeResponse = UserLoginResponse("mock_user_001", "en")
        return object : Call<UserLoginResponse> {
            override fun enqueue(callback: Callback<UserLoginResponse>) {
                callback.onResponse(this, Response.success(fakeResponse))
            }

            override fun isExecuted() = false
            override fun clone(): Call<UserLoginResponse> = this
            override fun isCanceled() = false
            override fun cancel() {}
            override fun request() = okhttp3.Request.Builder().url("http://mock").build()
            override fun timeout(): Timeout {
                TODO("Not yet implemented")
            }

            override fun execute(): Response<UserLoginResponse> = Response.success(fakeResponse)
        }
    }


    override fun userRegister(request: UserRegisterRequest): Call<UserRegisterResponse> {
        val fakeResponse = UserRegisterResponse("mock_user_001", request.language_preference)
        return object : Call<UserRegisterResponse> {
            override fun enqueue(callback: Callback<UserRegisterResponse>) {
                callback.onResponse(this, Response.success(fakeResponse))
            }
            override fun isExecuted() = false
            override fun clone(): Call<UserRegisterResponse> = this
            override fun isCanceled() = false
            override fun cancel() {}
            override fun request() = okhttp3.Request.Builder().url("http://mock").build()
            override fun timeout(): Timeout {
                TODO("Not yet implemented")
            }

            override fun execute(): Response<UserRegisterResponse> = Response.success(fakeResponse)
        }
    }

    override fun userLang(request: UserLangRequest) = Calls.response(
        UserLangResponse("mock_user_001", request.language_preference, "2025-10-18T12:00:00Z")
    )
}
