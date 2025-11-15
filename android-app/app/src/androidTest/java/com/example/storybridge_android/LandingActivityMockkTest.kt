package com.example.storybridge_android

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.example.storybridge_android.data.UserRepository
import com.example.storybridge_android.network.*
import com.example.storybridge_android.ui.landing.LandingActivity
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.*
import org.junit.runner.RunWith
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class LandingActivityMockkTest {

    private lateinit var scenario: ActivityScenario<LandingActivity>
    private lateinit var mockUserRepository: UserRepository

    @Before
    fun setup() {
        // MockK로 Mock 생성
        mockUserRepository = mockk()
        ServiceLocator.userRepository = mockUserRepository
    }

    @After
    fun teardown() {
        if (::scenario.isInitialized) scenario.close()
        ServiceLocator.reset()
        clearAllMocks()
    }

    @Test
    fun whenServerReturns200_NavigatesToMain() = runTest {
        val loginResponse = UserLoginResponse("uid", "en")
        val info = UserInfoResponse(
            user_id = "uid",
            title = "title",
            translated_title = "",
            image_base64 = "",
            started_at = "2025-10-29T00:00:00",
            session_id = "session_id"
        )
        val infoListResponse = listOf(info)

        // MockK로 stub 설정
        coEvery { mockUserRepository.login(any()) } returns Response.success(loginResponse)
        coEvery { mockUserRepository.getUserInfo(any()) } returns Response.success(infoListResponse)

        scenario = ActivityScenario.launch(LandingActivity::class.java)
        Thread.sleep(3000)

        onView(withId(R.id.main)).check(matches(isDisplayed()))

        // MockK로 검증
        coVerify(exactly = 1) { mockUserRepository.login(any()) }
    }

    @Test
    fun whenServerReturns400_ShowsLanguageSelection() = runTest {
        // 400 에러 응답
        val errorResponse = Response.error<UserLoginResponse>(
            400,
            """{"error":"USER__INVALID_REQUEST_BODY"}""".toResponseBody("application/json".toMediaType())
        )
        val registerResponse = UserRegisterResponse("test_user_mockito", "en")

        // MockK로 stub 설정
        coEvery { mockUserRepository.login(any()) } returns errorResponse
        coEvery { mockUserRepository.register(any()) } returns Response.success(registerResponse)

        // Activity 실행
        scenario = ActivityScenario.launch(LandingActivity::class.java)

        // 대기
        Thread.sleep(3000)

        // 언어 선택 화면 확인
        onView(withId(R.id.btnEnglish)).check(matches(isDisplayed()))

        // MockK로 검증
        coVerify(exactly = 1) { mockUserRepository.login(any()) }
        coVerify(exactly = 1) { mockUserRepository.register(any()) }
    }
}