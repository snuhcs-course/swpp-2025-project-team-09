package com.example.storybridge_android.viewmodel

import com.example.storybridge_android.data.UserRepository
import com.example.storybridge_android.network.*
import com.example.storybridge_android.ui.landing.LandingUiState
import com.example.storybridge_android.ui.landing.LandingViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LandingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setupDispatcher() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDownDispatcher() {
        Dispatchers.resetMain()
    }

    class FakeRepo(
        private val loginSuccess: Boolean = true,
        private val registerSuccess: Boolean = true
    ) : UserRepository {
        override suspend fun login(req: UserLoginRequest): Response<UserLoginResponse> {
            return if (loginSuccess)
                Response.success(UserLoginResponse("uid", "en"))
            else
                Response.error(400, "{}".toResponseBody(null))
        }

        override suspend fun register(req: UserRegisterRequest): Response<UserRegisterResponse> {
            return if (registerSuccess)
                Response.success(UserRegisterResponse("uid", "en"))
            else
                Response.error(400, "{}".toResponseBody(null))
        }

        override suspend fun getUserInfo(deviceInfo: String): Response<List<UserInfoResponse>> {
            return Response.success(listOf(UserInfoResponse("uid", "title", "", "", "2025-01-01T00:00:00")))
        }

        override suspend fun userLang(req: UserLangRequest): Response<UserLangResponse> {
            return Response.success(UserLangResponse("uid", req.language_preference, "2025-01-01T00:00:00"))
        }
    }


    @Test
    fun loginSuccess_navigatesToMain() = runTest {
        val vm = LandingViewModel(FakeRepo(loginSuccess = true))
        vm.checkUser("device_123")
        advanceUntilIdle()
        assertTrue(vm.uiState.value is LandingUiState.NavigateMain)
    }

    @Test
    fun loginFails_registerSuccess_showsLanguageSelect() = runTest {
        val vm = LandingViewModel(FakeRepo(loginSuccess = false, registerSuccess = true))
        vm.checkUser("device_123")
        advanceUntilIdle()
        assertTrue(vm.uiState.value is LandingUiState.ShowLanguageSelect)
    }

    @Test
    fun bothFail_showsError() = runTest {
        val vm = LandingViewModel(FakeRepo(loginSuccess = false, registerSuccess = false))
        vm.checkUser("device_123")
        advanceUntilIdle()
        assertTrue(vm.uiState.value is LandingUiState.Error)
    }
}
