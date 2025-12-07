package com.example.storybridge_android.viewmodel

import com.example.storybridge_android.ui.main.MainViewModel
import com.example.storybridge_android.data.UserRepository
import com.example.storybridge_android.data.SessionRepository
import com.example.storybridge_android.network.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setupDispatcher() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDownDispatcher() {
        Dispatchers.resetMain()
    }

    class FakeUserRepo(
        private val userInfoSuccess: Boolean = true
    ) : UserRepository {

        override suspend fun getUserInfo(deviceInfo: String): Response<List<UserInfoResponse>> {
            return if (userInfoSuccess) {
                Response.success(
                    listOf(
                        UserInfoResponse(
                            user_id = "39920",
                            session_id = "0",
                            title = "강아지",
                            translated_title = "dog",
                            image_base64 = "skedsk3949",
                            started_at = "2025-01-01T00:00:00"
                        )
                    )
                )
            } else {
                Response.error(400, "{}".toResponseBody())
            }
        }

        override suspend fun login(request: UserLoginRequest): Response<UserLoginResponse> {
            return Response.success(UserLoginResponse("uid", "en"))
        }

        override suspend fun register(request: UserRegisterRequest): Response<UserRegisterResponse> {
            return Response.success(UserRegisterResponse("uid", "en"))
        }

        override suspend fun userLang(request: UserLangRequest): Response<UserLangResponse> {
            return Response.success(
                UserLangResponse(
                    user_id = "uid",
                    language_preference = request.language_preference,
                    updated_at = "2025-01-01T00:00:00"
                )
            )
        }
    }

    class FakeSessionRepo(
        private val discardSuccess: Boolean = true
    ) : SessionRepository {

        override suspend fun startSession(userId: String): Result<StartSessionResponse> {
            return Result.success(
                StartSessionResponse(
                    session_id = "sid",
                    started_at = "2025-01-01T00:00:00",
                    page_index = 0
                )
            )
        }

        override suspend fun selectVoice(sessionId: String, voiceStyle: String): Result<SelectVoiceResponse> {
            return Result.success(
                SelectVoiceResponse(
                    session_id = sessionId,
                    voice_style = voiceStyle
                )
            )
        }

        override suspend fun endSession(sessionId: String): Result<EndSessionResponse> {
            return Result.success(
                EndSessionResponse(
                    session_id = sessionId,
                    ended_at = "2025-01-01T01:00:00",
                    total_pages = 5
                )
            )
        }

        override suspend fun getSessionStats(sessionId: String): Result<SessionStatsResponse> {
            return Result.success(
                SessionStatsResponse(
                    session_id = sessionId,
                    user_id = "uid",
                    isOngoing = false,
                    started_at = "2025-01-01T00:00:00",
                    ended_at = "2025-01-01T01:00:00",
                    total_pages = 7,
                    total_time_spent = 3600,
                    total_words_read = 500
                )
            )
        }

        override suspend fun reloadAllSession(
            userId: String,
            startedAt: String
        ): Result<ReloadAllSessionResponse> {
            return Result.success(
                ReloadAllSessionResponse(
                    session_id = "sid",
                    started_at = startedAt,
                    pages = listOf(
                        ReloadedPage(
                            page_index = 0,
                            img_url = "url",
                            translation_text = "hello",
                            audio_url = "audio.mp3",
                            ocr_results = emptyList()
                        )
                    )
                )
            )
        }

        override suspend fun discardSession(sessionId: String): Result<DiscardSessionResponse> {
            return if (discardSuccess) {
                Result.success(DiscardSessionResponse("deleted"))
            } else {
                Result.failure(Exception("fail"))
            }
        }
        override suspend fun pickWords(
            sessionId: String,
            lang: String
        ) = Result.failure<WordPickerResponse>(NotImplementedError())
    }

    class ExceptionUserRepo : UserRepository {
        override suspend fun getUserInfo(deviceInfo: String): Response<List<UserInfoResponse>> {
            throw RuntimeException("boom")
        }
        override suspend fun login(request: UserLoginRequest): Response<UserLoginResponse> {
            throw RuntimeException("boom")
        }
        override suspend fun register(request: UserRegisterRequest): Response<UserRegisterResponse> {
            throw RuntimeException("boom")
        }
        override suspend fun userLang(request: UserLangRequest): Response<UserLangResponse> {
            throw RuntimeException("boom")
        }
    }

    class ExceptionSessionRepo : SessionRepository {
        override suspend fun startSession(userId: String): Result<StartSessionResponse> {
            throw RuntimeException("boom")
        }
        override suspend fun selectVoice(sessionId: String, voiceStyle: String): Result<SelectVoiceResponse> {
            throw RuntimeException("boom")
        }
        override suspend fun endSession(sessionId: String): Result<EndSessionResponse> {
            throw RuntimeException("boom")
        }
        override suspend fun getSessionStats(sessionId: String): Result<SessionStatsResponse> {
            throw RuntimeException("boom")
        }
        override suspend fun reloadAllSession(
            userId: String,
            startedAt: String
        ): Result<ReloadAllSessionResponse> {
            throw RuntimeException("boom")
        }
        override suspend fun discardSession(sessionId: String): Result<DiscardSessionResponse> {
            throw RuntimeException("boom")
        }
        override suspend fun pickWords(
            sessionId: String,
            lang: String
        ) = Result.failure<WordPickerResponse>(NotImplementedError())
    }

    @Test
    fun loadUserInfo_success_updatesState() = runTest {
        val vm = MainViewModel(FakeUserRepo(true), FakeSessionRepo(true))
        vm.loadUserInfo("device")
        advanceUntilIdle()
        assertNotNull(vm.userInfo.value)
        assertTrue(vm.userInfo.value!!.isSuccessful)
    }

    @Test
    fun loadUserInfo_fail_updatesState() = runTest {
        val vm = MainViewModel(FakeUserRepo(false), FakeSessionRepo(true))
        vm.loadUserInfo("device")
        advanceUntilIdle()
        assertNotNull(vm.userInfo.value)
        assertTrue(vm.userInfo.value!!.isSuccessful.not())
    }

    @Test
    fun loadUserInfo_exception_setsNull() = runTest {
        val vm = MainViewModel(ExceptionUserRepo(), FakeSessionRepo(true))
        vm.loadUserInfo("device")
        advanceUntilIdle()
        assertEquals(null, vm.userInfo.value)
    }

    @Test
    fun discardSession_success_updatesDiscardResult_andReloadsUserInfo() = runTest {
        val vm = MainViewModel(FakeUserRepo(true), FakeSessionRepo(true))
        vm.discardSession("123", "device")
        advanceUntilIdle()
        assertNotNull(vm.userInfo.value)
        assertTrue(vm.userInfo.value!!.isSuccessful)
    }

    @Test
    fun discardSession_fail_updatesDiscardResult_only() = runTest {
        val vm = MainViewModel(FakeUserRepo(true), FakeSessionRepo(false))
        vm.discardSession("123", "device")
        advanceUntilIdle()
        assertEquals(null, vm.userInfo.value)
    }

    @Test
    fun discardSession_exception_setsFailure() = runTest {
        val vm = MainViewModel(FakeUserRepo(true), ExceptionSessionRepo())
        vm.discardSession("123", "device")
        advanceUntilIdle()
        assertEquals(null, vm.userInfo.value)
    }
}
