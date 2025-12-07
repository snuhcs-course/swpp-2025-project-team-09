package com.example.storybridge_android

import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
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
import com.example.storybridge_android.ui.landing.TutorialActivity
import com.example.storybridge_android.ui.main.MainActivity
import com.example.storybridge_android.ui.setting.AppSettings
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.*
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import retrofit2.Response

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class LandingActivityMockTest {

    private lateinit var scenario: ActivityScenario<LandingActivity>
    private lateinit var mockUserRepository: UserRepository

    @Before
    fun setup() {
        mockUserRepository = mockk()
        ServiceLocator.userRepository = mockUserRepository
        Intents.init()
    }

    @After
    fun teardown() {
        if (::scenario.isInitialized) scenario.close()
        ServiceLocator.reset()
        clearAllMocks()
        Intents.release()
    }

    //------------------------------------
    // 1. Check user info
    //------------------------------------

    @Test
    fun whenServerReturns200_ShowsLanguageSelection() = runTest {
        val loginResponse = UserLoginResponse("uid", "en")
        val infoListResponse = emptyList<UserInfoResponse>()

        coEvery { mockUserRepository.login(any()) } returns Response.success(loginResponse)
        coEvery { mockUserRepository.getUserInfo(any()) } returns Response.success(infoListResponse)

        scenario = ActivityScenario.launch(LandingActivity::class.java)
        Thread.sleep(3000)

        onView(withId(R.id.btnEnglish)).check(matches(isDisplayed()))
        onView(withId(R.id.btnChinese)).check(matches(isDisplayed()))

        coVerify(exactly = 1) { mockUserRepository.login(any()) }
        coVerify(exactly = 0) { mockUserRepository.getUserInfo(any()) }
    }

    @Test
    fun successfulLogin_withEmptyUserInfo_ShowsLanguageSelection() = runTest {
        val loginResponse = UserLoginResponse("uid", "en")
        val infoListResponse = emptyList<UserInfoResponse>()

        coEvery { mockUserRepository.login(any()) } returns Response.success(loginResponse)
        coEvery { mockUserRepository.getUserInfo(any()) } returns Response.success(infoListResponse)

        scenario = ActivityScenario.launch(LandingActivity::class.java)
        Thread.sleep(3000)

        onView(withId(R.id.btnEnglish)).check(matches(isDisplayed()))
        intended(IntentMatchers.hasComponent(MainActivity::class.java.name), times(0))
    }

    @Test
    fun getUserInfoError_ShowsLanguageSelection() = runTest {
        val loginResponse = UserLoginResponse("uid", "en")
        val infoError = Response.error<List<UserInfoResponse>>(
            500,
            """{"error":"SERVER_ERROR"}""".toResponseBody("application/json".toMediaType())
        )

        coEvery { mockUserRepository.login(any()) } returns Response.success(loginResponse)
        coEvery { mockUserRepository.getUserInfo(any()) } returns infoError

        scenario = ActivityScenario.launch(LandingActivity::class.java)
        Thread.sleep(3000)

        onView(withId(R.id.btnEnglish)).check(matches(isDisplayed()))

        coVerify(exactly = 1) { mockUserRepository.login(any()) }
    }

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
    fun registerSuccess_navigatesToTutorial() = runTest {
        val loginError = Response.error<UserLoginResponse>(
            400,
            """{"error":"USER__INVALID_REQUEST_BODY"}""".toResponseBody("application/json".toMediaType())
        )
        val registerResponse = UserRegisterResponse("new_uid", "en")

        coEvery { mockUserRepository.login(any()) } returns loginError
        coEvery { mockUserRepository.register(any()) } returns Response.success(registerResponse)

        scenario = ActivityScenario.launch(LandingActivity::class.java)
        Thread.sleep(1500)

        onView(withId(R.id.btnEnglish)).perform(click())
        Thread.sleep(300)
        onView(withId(R.id.startButton)).perform(click())
        Thread.sleep(1000)

        intended(IntentMatchers.hasComponent(TutorialActivity::class.java.name), times(1))
    }

    //------------------------------------
    // 2. Language Selection
    //------------------------------------

    @Test
    fun clickingStartButton_withoutSelectingLanguage_showsToastAndStays() = runTest {
        val loginError = Response.error<UserLoginResponse>(
            400,
            """{"error":"USER__INVALID_REQUEST_BODY"}""".toResponseBody("application/json".toMediaType())
        )
        val registerResponse = UserRegisterResponse("mock_uid", "en")

        coEvery { mockUserRepository.login(any()) } returns loginError
        coEvery { mockUserRepository.register(any()) } returns Response.success(registerResponse)

        scenario = ActivityScenario.launch(LandingActivity::class.java)
        Thread.sleep(1500)

        onView(withId(R.id.startButton)).check(matches(isDisplayed())).perform(click())
        Thread.sleep(1000)

        intended(IntentMatchers.hasComponent(TutorialActivity::class.java.name), times(0))

        onView(withId(R.id.btnEnglish)).check(matches(isDisplayed()))
    }

    @Test
    fun clickingVietnameseButton_setsLanguageToVi() = runTest {
        mockkObject(AppSettings)
        every { AppSettings.setLanguage(any(), any()) } just Runs

        val loginError = Response.error<UserLoginResponse>(
            400,
            """{"error":"USER__INVALID_REQUEST_BODY"}""".toResponseBody("application/json".toMediaType())
        )
        val registerResponse = UserRegisterResponse("mock_uid", "vi")

        coEvery { mockUserRepository.login(any()) } returns loginError
        coEvery { mockUserRepository.register(any()) } returns Response.success(registerResponse)

        scenario = ActivityScenario.launch(LandingActivity::class.java)
        Thread.sleep(1500)

        onView(withId(R.id.btnVietnamese)).check(matches(isDisplayed())).perform(click())
        Thread.sleep(500)

        verify { AppSettings.setLanguage(any(), "vi") }

        unmockkObject(AppSettings)
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        AppSettings.setLanguage(context, "en")
    }

    @Test
    fun clickingChineseButton_setsLanguageToZh() = runTest {
        mockkObject(AppSettings)
        every { AppSettings.setLanguage(any(), any()) } just Runs

        val loginError = Response.error<UserLoginResponse>(
            400,
            """{"error":"USER__INVALID_REQUEST_BODY"}""".toResponseBody("application/json".toMediaType())
        )
        val registerResponse = UserRegisterResponse("mock_uid", "zh")

        coEvery { mockUserRepository.login(any()) } returns loginError
        coEvery { mockUserRepository.register(any()) } returns Response.success(registerResponse)

        scenario = ActivityScenario.launch(LandingActivity::class.java)
        Thread.sleep(1500)

        onView(withId(R.id.btnChinese)).check(matches(isDisplayed())).perform(click())
        Thread.sleep(500)

        verify { AppSettings.setLanguage(any(), "zh") }

        unmockkObject(AppSettings)
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        AppSettings.setLanguage(context, "en")
    }

    @Test
    fun z_clickingEnglishButton_setsLanguageToEn() = runTest {
        mockkObject(AppSettings)
        every { AppSettings.setLanguage(any(), any()) } just Runs

        val loginError = Response.error<UserLoginResponse>(
            400,
            """{"error":"USER__INVALID_REQUEST_BODY"}""".toResponseBody("application/json".toMediaType())
        )
        val registerResponse = UserRegisterResponse("mock_uid", "en")

        coEvery { mockUserRepository.login(any()) } returns loginError
        coEvery { mockUserRepository.register(any()) } returns Response.success(registerResponse)

        scenario = ActivityScenario.launch(LandingActivity::class.java)
        Thread.sleep(1500)

        onView(withId(R.id.btnEnglish)).check(matches(isDisplayed())).perform(click())
        Thread.sleep(500)

        verify { AppSettings.setLanguage(any(), "en") }

        unmockkObject(AppSettings)
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        AppSettings.setLanguage(context, "en")
    }
}