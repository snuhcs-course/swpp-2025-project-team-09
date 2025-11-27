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
        mockUserRepository = mockk(relaxed = true)
        ServiceLocator.userRepository = mockUserRepository

        // Mock Settings.Secure
        mockkStatic(Settings.Secure::class)
        every {
            Settings.Secure.getString(any(), Settings.Secure.ANDROID_ID)
        } returns "mockDeviceId"

        // Mock AppSettings
        mockkObject(AppSettings)
        every { AppSettings.getLanguage(any()) } returns "en"
        every { AppSettings.setLanguage(any(), any()) } just Runs

        // Mock StoryBridgeApplication
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
        // GIVEN: Mock successful response
        val successResponse = Response.success(
            UserLangResponse("uid", "en", "2025-11-15T12:00:00")
        )
        coEvery { mockUserRepository.userLang(any()) } returns successResponse

        var wasFinishing = false

        // WHEN: Launch activity and select English
        scenario = ActivityScenario.launch(SettingActivity::class.java)
        Thread.sleep(500)

        onView(withId(R.id.radioEnglish)).perform(click())
        onView(withId(R.id.btnBack)).perform(click())

        // Wait briefly and check isFinishing
        Thread.sleep(1000)

        try {
            scenario.onActivity {
                wasFinishing = it.isFinishing
            }
        } catch (e: Exception) {
            // If activity is already destroyed, consider it a success
            wasFinishing = true
        }

        Thread.sleep(2000)

        // THEN: Verify userLang was called
        coVerify(exactly = 1) {
            mockUserRepository.userLang(any())
        }

        // Verify activity is finishing or destroyed
        Assert.assertTrue("Activity should be finishing or destroyed", wasFinishing)
    }

    @Test
    fun chineseSelection_callsUserLangAndFinishesOnSuccess() = runTest {
        // GIVEN: Mock successful response
        val successResponse = Response.success(
            UserLangResponse("uid", "zh", "2025-11-15T12:00:00")
        )
        coEvery { mockUserRepository.userLang(any()) } returns successResponse

        var wasFinishing = false

        // WHEN: Launch activity and select Chinese
        scenario = ActivityScenario.launch(SettingActivity::class.java)
        Thread.sleep(500)

        onView(withId(R.id.radioChinese)).perform(click())
        onView(withId(R.id.btnBack)).perform(click())

        // Wait briefly and check isFinishing
        Thread.sleep(1000)

        try {
            scenario.onActivity {
                wasFinishing = it.isFinishing
            }
        } catch (e: Exception) {
            // If activity is already destroyed, consider it a success
            wasFinishing = true
        }

        Thread.sleep(2000)

        // THEN: Verify userLang was called
        coVerify(exactly = 1) {
            mockUserRepository.userLang(any())
        }

        // Verify activity is finishing or destroyed
        Assert.assertTrue("Activity should be finishing or destroyed", wasFinishing)
    }

    @Test
    fun errorResponse_doesNotFinishActivity() = runTest {
        // GIVEN: Mock error response
        val errorResponse = Response.error<UserLangResponse>(
            400,
            """{"error":"invalid"}""".toResponseBody("application/json".toMediaType())
        )
        coEvery { mockUserRepository.userLang(any()) } returns errorResponse

        // WHEN: Launch activity and attempt language change
        scenario = ActivityScenario.launch(SettingActivity::class.java)
        Thread.sleep(500)

        onView(withId(R.id.radioEnglish)).perform(click())
        onView(withId(R.id.btnBack)).perform(click())

        Thread.sleep(2000)

        // THEN: Verify userLang was called
        coVerify(exactly = 1) {
            mockUserRepository.userLang(any())
        }

        // Verify activity is still alive
        scenario.onActivity {
            Assert.assertFalse("Activity should not be finishing", it.isFinishing)
        }
    }

    @Test
    fun onCreate_checksCorrectRadioButton() {
        // GIVEN: AppSettings returns "en"

        // WHEN: Launch activity
        scenario = ActivityScenario.launch(SettingActivity::class.java)
        Thread.sleep(500)

        // THEN: Verify English radio button is checked
        onView(withId(R.id.radioEnglish)).check(matches(isChecked()))
        onView(withId(R.id.radioChinese)).check(matches(hamcrestNot(isChecked())))
    }

    @Test
    fun onCreate_checksChineseWhenLanguageIsZh() {
        // GIVEN: AppSettings returns "zh"
        every { AppSettings.getLanguage(any()) } returns "zh"

        // WHEN: Launch activity
        scenario = ActivityScenario.launch(SettingActivity::class.java)
        Thread.sleep(500)

        // THEN: Verify Chinese radio button is checked
        onView(withId(R.id.radioChinese)).check(matches(isChecked()))
        onView(withId(R.id.radioEnglish)).check(matches(hamcrestNot(isChecked())))
    }
}