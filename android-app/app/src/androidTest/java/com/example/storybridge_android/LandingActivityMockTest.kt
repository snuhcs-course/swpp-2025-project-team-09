package com.example.storybridge_android

import android.widget.Button
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.times
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.example.storybridge_android.data.UserRepository
import com.example.storybridge_android.network.*
import com.example.storybridge_android.ui.landing.LandingActivity
import com.example.storybridge_android.ui.main.MainActivity
import com.example.storybridge_android.ui.setting.AppSettings
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
class LandingActivityMockTest {

    private lateinit var scenario: ActivityScenario<LandingActivity>
    private lateinit var mockUserRepository: UserRepository

    @Before
    fun setup() {
        mockUserRepository = mockk()
        ServiceLocator.userRepository = mockUserRepository
    }

    @After
    fun teardown() {
        if (::scenario.isInitialized) scenario.close()
        ServiceLocator.reset()
        clearAllMocks()
    }

    /***
     * 1. Successful Login
     ***/
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

        coEvery { mockUserRepository.login(any()) } returns Response.success(loginResponse)
        coEvery { mockUserRepository.getUserInfo(any()) } returns Response.success(infoListResponse)

        scenario = ActivityScenario.launch(LandingActivity::class.java)
        Thread.sleep(3000)

        onView(withId(R.id.main)).check(matches(isDisplayed()))

        coVerify(exactly = 1) { mockUserRepository.login(any()) }
    }

    @Test
    fun successfulLogin_withEmptyUserInfo_navigatesToMain() = runTest {
        val loginResponse = UserLoginResponse("uid", "en")

        coEvery { mockUserRepository.login(any()) } returns Response.success(loginResponse)
        coEvery { mockUserRepository.getUserInfo(any()) } returns Response.success(emptyList())

        scenario = ActivityScenario.launch(LandingActivity::class.java)
        Thread.sleep(3000)

        var isFinishing = false
        try {
            scenario.onActivity { activity ->
                isFinishing = activity.isFinishing
            }
        } catch (e: Exception) {
            isFinishing = true
        }
        assert(isFinishing) { "Activity should be finishing after navigation to MainActivity" }
    }

    @Test
    fun getUserInfoError_stillNavigatesToMain() = runTest {
        val loginResponse = UserLoginResponse("uid", "en")
        val infoError = Response.error<List<UserInfoResponse>>(
            500,
            """{"error":"SERVER_ERROR"}""".toResponseBody("application/json".toMediaType())
        )

        coEvery { mockUserRepository.login(any()) } returns Response.success(loginResponse)
        coEvery { mockUserRepository.getUserInfo(any()) } returns infoError

        scenario = ActivityScenario.launch(LandingActivity::class.java)
        Thread.sleep(3000)

        coVerify(exactly = 1) { mockUserRepository.getUserInfo(any()) }
    }

    /***
     * 2. Login failed -> Register (language selection)
     ***/

    @Test
    fun whenServerReturns400_ShowsLanguageSelection() = runTest {
        val errorResponse = Response.error<UserLoginResponse>(
            400,
            """{"error":"USER__INVALID_REQUEST_BODY"}""".toResponseBody("application/json".toMediaType())
        )
        val registerResponse = UserRegisterResponse("test_user_mockito", "en")

        coEvery { mockUserRepository.login(any()) } returns errorResponse
        coEvery { mockUserRepository.register(any()) } returns Response.success(registerResponse)

        scenario = ActivityScenario.launch(LandingActivity::class.java)

        Thread.sleep(3000)

        onView(withId(R.id.btnEnglish)).check(matches(isDisplayed()))

        coVerify(exactly = 1) { mockUserRepository.login(any()) }
        coVerify(exactly = 1) { mockUserRepository.register(any()) }
    }

    @Test
    fun clickingLanguageButton_callsRegister() = runTest {
        val errorResponse = Response.error<UserLoginResponse>(
            400,
            """{"error":"USER__INVALID_REQUEST_BODY"}""".toResponseBody("application/json".toMediaType())
        )

        val registerResponse = UserRegisterResponse("mock_uid", "en")

        coEvery { mockUserRepository.login(any()) } returns errorResponse
        coEvery { mockUserRepository.register(any()) } returns Response.success(registerResponse)

        scenario = ActivityScenario.launch(LandingActivity::class.java)
        Thread.sleep(1500)

        onView(withId(R.id.btnEnglish)).perform(click())
        Thread.sleep(1000)

        coVerify(exactly = 1) { mockUserRepository.register(any()) }
    }

    @Test
    fun registerSuccess_navigatesToMain() = runTest {
        // GIVEN: Login 실패 → Register 성공
        val loginError = Response.error<UserLoginResponse>(
            400,
            """{"error":"USER__INVALID_REQUEST_BODY"}""".toResponseBody("application/json".toMediaType())
        )
        val registerResponse = UserRegisterResponse("new_uid", "en")

        coEvery { mockUserRepository.login(any()) } returns loginError
        coEvery { mockUserRepository.register(any()) } returns Response.success(registerResponse)

        // WHEN: Activity 실행 후 English 선택 및 Start
        scenario = ActivityScenario.launch(LandingActivity::class.java)
        Thread.sleep(1500)

        onView(withId(R.id.btnEnglish)).perform(click())
        Thread.sleep(300)
        onView(withId(R.id.startButton)).perform(click())
        Thread.sleep(1000)

        // THEN: MainActivity로 이동
        var isFinishing = false
        try {
            scenario.onActivity { activity ->
                isFinishing = activity.isFinishing
            }
        } catch (e: Exception) {
            isFinishing = true
        }
        assert(isFinishing) { "Activity should finish after successful registration" }
    }

    /***
     * 3. Language selection UI
     ***/

    @Test
    fun clickingChineseButton_setsLanguageToZh() = runTest {
        // GIVEN: AppSettings mock
        mockkObject(AppSettings)
        every { AppSettings.setLanguage(any(), any()) } just Runs

        val errorResponse = Response.error<UserLoginResponse>(
            400,
            """{"error":"USER__INVALID_REQUEST_BODY"}""".toResponseBody("application/json".toMediaType())
        )
        val registerResponse = UserRegisterResponse("mock_uid", "zh")

        coEvery { mockUserRepository.login(any()) } returns errorResponse
        coEvery { mockUserRepository.register(any()) } returns Response.success(registerResponse)

        scenario = ActivityScenario.launch(LandingActivity::class.java)
        Thread.sleep(1500)

        // WHEN: Chinese 버튼 클릭
        onView(withId(R.id.btnChinese)).perform(click())
        Thread.sleep(500)

        // THEN: AppSettings에 "zh"가 설정됨
        verify { AppSettings.setLanguage(any(), "zh") }

        unmockkObject(AppSettings)

        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        AppSettings.setLanguage(context, "en")
    }

    @Test
    fun clickingEnglishButton_setsLanguageToEn() = runTest {
        // GIVEN: AppSettings mock
        mockkObject(AppSettings)
        every { AppSettings.setLanguage(any(), any()) } just Runs

        val errorResponse = Response.error<UserLoginResponse>(
            400,
            """{"error":"USER__INVALID_REQUEST_BODY"}""".toResponseBody("application/json".toMediaType())
        )
        val registerResponse = UserRegisterResponse("mock_uid", "en")

        coEvery { mockUserRepository.login(any()) } returns errorResponse
        coEvery { mockUserRepository.register(any()) } returns Response.success(registerResponse)

        scenario = ActivityScenario.launch(LandingActivity::class.java)
        Thread.sleep(1500)

        // WHEN: English 버튼 클릭
        onView(withId(R.id.btnEnglish)).perform(click())
        Thread.sleep(500)

        // THEN: AppSettings에 "en"이 설정됨
        verify { AppSettings.setLanguage(any(), "en") }

        unmockkObject(AppSettings)
    }


    @Test
    fun languageButtonSelection_updatesButtonState() = runTest {
        val errorResponse = Response.error<UserLoginResponse>(
            400,
            """{"error":"USER__INVALID_REQUEST_BODY"}""".toResponseBody("application/json".toMediaType())
        )
        val registerResponse = UserRegisterResponse("mock_uid", "en")

        coEvery { mockUserRepository.login(any()) } returns errorResponse
        coEvery { mockUserRepository.register(any()) } returns Response.success(registerResponse)

        scenario = ActivityScenario.launch(LandingActivity::class.java)
        Thread.sleep(1500)

        // WHEN: English 버튼 선택
        onView(withId(R.id.btnEnglish)).perform(click())
        Thread.sleep(300)

        // THEN: English 버튼이 선택됨
        scenario.onActivity { activity ->
            val btnEnglish = activity.findViewById<Button>(R.id.btnEnglish)
            val btnChinese = activity.findViewById<Button>(R.id.btnChinese)
            assert(btnEnglish.isSelected) { "English button should be selected" }
            assert(!btnChinese.isSelected) { "Chinese button should not be selected" }
        }

        // WHEN: Chinese 버튼 선택 (토글)
        onView(withId(R.id.btnChinese)).perform(click())
        Thread.sleep(300)

        // THEN: Chinese 버튼만 선택됨
        scenario.onActivity { activity ->
            val btnEnglish = activity.findViewById<Button>(R.id.btnEnglish)
            val btnChinese = activity.findViewById<Button>(R.id.btnChinese)
            assert(!btnEnglish.isSelected) { "English button should not be selected" }
            assert(btnChinese.isSelected) { "Chinese button should be selected" }
        }

        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        AppSettings.setLanguage(context, "en")
    }

    /***
     * 4. Start button in Language selection UI
     ***/
    @Test
    fun clickingStartButton_withoutSelectingLanguage_doesNotNavigate() = runTest {
        val errorResponse = Response.error<UserLoginResponse>(
            400,
            """{"error":"USER__INVALID_REQUEST_BODY"}""".toResponseBody("application/json".toMediaType())
        )
        val registerResponse = UserRegisterResponse("mock_uid", "en")

        coEvery { mockUserRepository.login(any()) } returns errorResponse
        coEvery { mockUserRepository.register(any()) } returns Response.success(registerResponse)

        Intents.init()

        scenario = ActivityScenario.launch(LandingActivity::class.java)
        Thread.sleep(1500)

        onView(withId(R.id.startButton)).perform(click())
        Thread.sleep(500)

        intended(IntentMatchers.hasComponent(MainActivity::class.java.name), times(0))

        Intents.release()
    }

    @Test
    fun clickingStartButton_withoutSelectingLanguage_staysOnLanguageScreen() = runTest {
        val loginError = Response.error<UserLoginResponse>(
            400,
            """{"error":"USER__INVALID_REQUEST_BODY"}""".toResponseBody("application/json".toMediaType())
        )
        val registerSuccess = UserRegisterResponse("mock_uid", "en")

        coEvery { mockUserRepository.login(any()) } returns loginError
        coEvery { mockUserRepository.register(any()) } returns Response.success(registerSuccess)

        Intents.init()

        scenario = ActivityScenario.launch(LandingActivity::class.java)
        Thread.sleep(1500) // ShowLanguageSelect 도달

        // WHEN: 언어 선택 없이 startButton 클릭
        onView(withId(R.id.startButton)).perform(click())
        Thread.sleep(500)

        // THEN: MainActivity로 이동하지 않아야 함
        intended(IntentMatchers.hasComponent(MainActivity::class.java.name), times(0))

        Intents.release()
    }

    @Test
    fun clickingStartButton_afterSelectingChinese_navigatesToMain() = runTest {
        val errorResponse = Response.error<UserLoginResponse>(
            400,
            """{"error":"USER__INVALID_REQUEST_BODY"}""".toResponseBody("application/json".toMediaType())
        )
        val registerResponse = UserRegisterResponse("mock_uid", "zh")

        coEvery { mockUserRepository.login(any()) } returns errorResponse
        coEvery { mockUserRepository.register(any()) } returns Response.success(registerResponse)

        scenario = ActivityScenario.launch(LandingActivity::class.java)
        Thread.sleep(1500)

        // WHEN: Start after Chinese selected
        onView(withId(R.id.btnChinese)).perform(click())
        Thread.sleep(300)
        onView(withId(R.id.startButton)).perform(click())
        Thread.sleep(1000)

        // THEN: Move to MainActivity
        var isFinishing = false
        try {
            scenario.onActivity { activity ->
                isFinishing = activity.isFinishing
            }
        } catch (e: Exception) {
            isFinishing = true
        }
        assert(isFinishing) { "Activity should finish after clicking start" }

        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        AppSettings.setLanguage(context, "en")
    }


    /***
     * 5. Error Handling
     ***/
    @Test
    fun loginError_andRegisterError_showsErrorAndStaysOnLanding() = runTest {
        val loginError = Response.error<UserLoginResponse>(
            400, """{"error":"USER__INVALID_REQUEST_BODY"}""".toResponseBody("application/json".toMediaType())
        )

        val registerError = Response.error<UserRegisterResponse>(
            500, """{"error":"SERVER_ERROR"}""".toResponseBody("application/json".toMediaType())
        )

        coEvery { mockUserRepository.login(any()) } returns loginError
        coEvery { mockUserRepository.register(any()) } returns registerError

        scenario = ActivityScenario.launch(LandingActivity::class.java)
        Thread.sleep(1500)

        onView(withId(R.id.btnEnglish)).check(doesNotExist())
        onView(withId(R.id.btnChinese)).check(doesNotExist())

        onView(withId(R.id.logo)).check(matches(isDisplayed()))
    }

    @Test
    fun uiStateError_showsToast() = runTest {
        // GIVEN: Login exception
        coEvery { mockUserRepository.login(any()) } throws Exception("Network error")

        // WHEN: Activity 실행
        scenario = ActivityScenario.launch(LandingActivity::class.java)
        Thread.sleep(2000)

        scenario.onActivity { activity ->
            assert(!activity.isFinishing) { "Activity should not finish on error" }
        }
    }
}