package com.example.storybridge_android.ui.setting

import android.provider.Settings
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.storybridge_android.R
import com.example.storybridge_android.ServiceLocator
import com.example.storybridge_android.StoryBridgeApplication
import com.example.storybridge_android.data.UserRepository
import com.example.storybridge_android.network.UserLangResponse
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.hamcrest.Matchers.not as hamcrestNot
import org.junit.*
import org.junit.runner.RunWith
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class SettingActivityMockTest {

    private lateinit var scenario: ActivityScenario<SettingActivity>
    private lateinit var mockUserRepository: UserRepository

    @Before
    fun setup() {
        // MockK로 Mock 생성
        mockUserRepository = mockk(relaxed = true)
        ServiceLocator.userRepository = mockUserRepository

        // Settings.Secure Mock
        mockkStatic(Settings.Secure::class)
        every {
            Settings.Secure.getString(any(), Settings.Secure.ANDROID_ID)
        } returns "mockDeviceId"

        // AppSettings Mock
        mockkObject(AppSettings)
        every { AppSettings.getLanguage(any()) } returns "en"
        every { AppSettings.setLanguage(any(), any()) } just Runs

        // StoryBridgeApplication Mock
        mockkObject(StoryBridgeApplication)
        every { StoryBridgeApplication.applyLanguage(any()) } just Runs
    }

    @After
    fun teardown() {
        if (::scenario.isInitialized) {
            scenario.close()
        }
        ServiceLocator.reset()
        unmockkAll()
    }

    @Test
    fun englishSelection_callsUserLangAndFinishesOnSuccess() = runTest {
        // GIVEN: 성공 응답 설정
        val successResponse = Response.success(
            UserLangResponse("uid", "en", "2025-11-15T12:00:00")
        )
        coEvery { mockUserRepository.userLang(any()) } returns successResponse

        var wasFinishing = false

        // WHEN: Activity 실행
        scenario = ActivityScenario.launch(SettingActivity::class.java)
        Thread.sleep(500)

        onView(withId(R.id.radioEnglish)).perform(click())
        onView(withId(R.id.btnBack)).perform(click())

        // 짧은 대기 후 isFinishing 체크 (Activity가 종료되기 직전)
        Thread.sleep(1000)

        try {
            scenario.onActivity {
                wasFinishing = it.isFinishing
            }
        } catch (e: Exception) {
            // Activity가 이미 종료되었다면 성공으로 간주
            wasFinishing = true
        }

        // 추가 대기
        Thread.sleep(2000)

        // THEN: userLang 호출 확인
        coVerify(exactly = 1) {
            mockUserRepository.userLang(any())
        }

        // Activity가 종료되었는지 확인
        Assert.assertTrue("Activity should be finishing or destroyed", wasFinishing)
    }

    @Test
    fun chineseSelection_callsUserLangAndFinishesOnSuccess() = runTest {
        // GIVEN: 성공 응답 설정
        val successResponse = Response.success(
            UserLangResponse("uid", "zh", "2025-11-15T12:00:00")
        )
        coEvery { mockUserRepository.userLang(any()) } returns successResponse

        var wasFinishing = false

        // WHEN: Activity 실행
        scenario = ActivityScenario.launch(SettingActivity::class.java)
        Thread.sleep(500)

        onView(withId(R.id.radioChinese)).perform(click())
        onView(withId(R.id.btnBack)).perform(click())

        // 짧은 대기 후 isFinishing 체크 (Activity가 종료되기 직전)
        Thread.sleep(1000)

        try {
            scenario.onActivity {
                wasFinishing = it.isFinishing
            }
        } catch (e: Exception) {
            // Activity가 이미 종료되었다면 성공으로 간주
            wasFinishing = true
        }

        // 추가 대기
        Thread.sleep(2000)

        // THEN: userLang 호출 확인
        coVerify(exactly = 1) {
            mockUserRepository.userLang(any())
        }

        // Activity가 종료되었는지 확인
        Assert.assertTrue("Activity should be finishing or destroyed", wasFinishing)
    }

    @Test
    fun errorResponse_doesNotFinishActivity() = runTest {
        // GIVEN: 에러 응답 설정
        val errorResponse = Response.error<UserLangResponse>(
            400,
            """{"error":"invalid"}""".toResponseBody("application/json".toMediaType())
        )
        coEvery { mockUserRepository.userLang(any()) } returns errorResponse

        // WHEN: Activity 실행
        scenario = ActivityScenario.launch(SettingActivity::class.java)
        Thread.sleep(500)

        onView(withId(R.id.radioEnglish)).perform(click())
        onView(withId(R.id.btnBack)).perform(click())

        Thread.sleep(2000)

        // THEN: userLang 호출 확인
        coVerify(exactly = 1) {
            mockUserRepository.userLang(any())
        }

        // Activity가 여전히 살아있는지 확인
        scenario.onActivity {
            Assert.assertFalse("Activity should not be finishing", it.isFinishing)
        }
    }

    @Test
    fun onCreate_checksCorrectRadioButton() {
        // GIVEN: AppSettings가 "en"을 반환

        // WHEN: Activity 실행
        scenario = ActivityScenario.launch(SettingActivity::class.java)
        Thread.sleep(500)

        // THEN: English 라디오 버튼이 체크되어 있는지 확인
        onView(withId(R.id.radioEnglish)).check(matches(isChecked()))
        onView(withId(R.id.radioChinese)).check(matches(hamcrestNot(isChecked())))
    }

    @Test
    fun onCreate_checksChineseWhenLanguageIsZh() {
        // GIVEN: AppSettings가 "zh"를 반환하도록 설정
        every { AppSettings.getLanguage(any()) } returns "zh"

        // WHEN: Activity 실행
        scenario = ActivityScenario.launch(SettingActivity::class.java)
        Thread.sleep(500)

        // THEN: Chinese 라디오 버튼이 체크되어 있는지 확인
        onView(withId(R.id.radioChinese)).check(matches(isChecked()))
        onView(withId(R.id.radioEnglish)).check(matches(hamcrestNot(isChecked())))
    }
}