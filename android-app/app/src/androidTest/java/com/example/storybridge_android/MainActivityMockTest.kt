package com.example.storybridge_android.ui.main

import android.app.Activity.RESULT_OK
import android.app.Instrumentation
import android.content.Intent
import android.provider.Settings
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.intent.Intents.intended
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.storybridge_android.ServiceLocator
import com.example.storybridge_android.data.*
import com.example.storybridge_android.network.UserInfoResponse
import com.example.storybridge_android.R
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class MainActivityMockTest {

    private lateinit var mockProcessRepo: ProcessRepository
    private lateinit var mockUserRepo: UserRepository
    private lateinit var mockSessionRepo: SessionRepository

    @Before
    fun setup() {
        mockProcessRepo = mockk(relaxed = true)
        mockUserRepo = mockk(relaxed = true)
        mockSessionRepo = mockk(relaxed = true)

        ServiceLocator.processRepository = mockProcessRepo
        ServiceLocator.userRepository = mockUserRepo
        ServiceLocator.sessionRepository = mockSessionRepo

        mockkStatic(Settings.Secure::class)
        every { Settings.Secure.getString(any(), Settings.Secure.ANDROID_ID) } returns "DEVICE123"

        Intents.init()
    }

    @After
    fun tearDown() {
        unmockkAll()
        Intents.release()
    }

    private fun launchMain() = ActivityScenario.launch<MainActivity>(
        Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
    )

    @Test
    fun startButton_opensStartSessionActivity() = runTest {
        coEvery { mockUserRepo.getUserInfo("DEVICE123") } returns retrofit2.Response.success(emptyList())

        launchMain()
        Thread.sleep(800)

        onView(withId(R.id.startNewReadingButton)).perform(click())

        intended(IntentMatchers.hasComponent("com.example.storybridge_android.ui.session.StartSessionActivity"))
    }

    @Test
    fun settingsButton_opensSettingActivity() = runTest {
        coEvery { mockUserRepo.getUserInfo("DEVICE123") } returns retrofit2.Response.success(emptyList())

        launchMain()
        Thread.sleep(800)

        onView(withId(R.id.navbarSettingsButton)).perform(click())

        intended(IntentMatchers.hasComponent("com.example.storybridge_android.ui.setting.SettingActivity"))
    }

    @Test
    fun userInfo_showsSessionCards() = runTest {
        val fakeList = listOf(
            UserInfoResponse(
                session_id = "S1",
                user_id = "DEVICE123",
                image_base64 = "",
                started_at = "2025-01-01",
                title = "A",
                translated_title = "Dog"
            ),
            UserInfoResponse(
                session_id = "S2",
                user_id = "DEVICE123",
                image_base64 = "",
                started_at = "2025-01-02",
                title = "B",
                translated_title = "Cat"
            )
        )

        coEvery { mockUserRepo.getUserInfo("DEVICE123") } returns retrofit2.Response.success(fakeList)

        launchMain()
        Thread.sleep(1000)

        onView(withText("Dog")).check(matches(isDisplayed()))
        onView(withText("Cat")).check(matches(isDisplayed()))
    }

    @Test
    fun userInfo_empty_showsEmptyContainer() = runTest {
        coEvery { mockUserRepo.getUserInfo("DEVICE123") } returns retrofit2.Response.success(emptyList())

        launchMain()
        Thread.sleep(800)

        onView(withId(R.id.emptyContainer)).check(matches(isDisplayed()))
    }

    @Test
    fun settingsLauncher_receivesResultOk() = runTest {
        coEvery { mockUserRepo.getUserInfo("DEVICE123") } returns retrofit2.Response.success(emptyList())

        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
        val scenario = ActivityScenario.launch<MainActivity>(intent)

        Thread.sleep(500)

        Intents.intending(
            IntentMatchers.hasComponent("com.example.storybridge_android.ui.setting.SettingActivity")
        ).respondWith(
            Instrumentation.ActivityResult(RESULT_OK, Intent())
        )

        // WHEN: Settings button clicked
        onView(withId(R.id.navbarSettingsButton)).perform(click())

        Thread.sleep(1000)

        // THEN: SettingActivity activated
        Intents.intended(
            IntentMatchers.hasComponent("com.example.storybridge_android.ui.setting.SettingActivity")
        )

        scenario.onActivity { activity ->
            assert(!activity.isFinishing) { "MainActivity should still be running" }
        }

        scenario.close()
    }

    @Test
    fun backPress_showsExitPanel() = runTest {
        coEvery { mockUserRepo.getUserInfo("DEVICE123") } returns retrofit2.Response.success(emptyList())

        launchMain()
        Thread.sleep(500)

        pressBack()
        Thread.sleep(500)

        onView(withId(R.id.exitPanelInclude)).check(matches(isDisplayed()))
    }

    @Test
    fun exitConfirmBtn_finishesActivity() = runTest {
        coEvery { mockUserRepo.getUserInfo("DEVICE123") } returns retrofit2.Response.success(emptyList())

        val scenario = launchMain()
        Thread.sleep(500)

        pressBack()
        Thread.sleep(500)

        onView(withId(R.id.exitConfirmBtn)).perform(click())
        Thread.sleep(500)

        scenario.onActivity { activity ->
            assert(activity.isFinishing) { "MainActivity should be finishing after exit confirm" }
        }

        scenario.close()
    }

    @Test
    fun exitCancelBtn_hidesExitPanel() = runTest {
        coEvery { mockUserRepo.getUserInfo("DEVICE123") } returns retrofit2.Response.success(emptyList())

        launchMain()
        Thread.sleep(500)

        pressBack()
        Thread.sleep(500)

        onView(withId(R.id.exitPanelInclude)).check(matches(isDisplayed()))

        onView(withId(R.id.exitCancelBtn)).perform(click())
        Thread.sleep(500)

        onView(withId(R.id.exitPanelInclude)).check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    @Test
    fun trashButton_showsDiscardPanel() = runTest {
        val fakeList = listOf(
            UserInfoResponse(
                session_id = "S1",
                user_id = "DEVICE123",
                image_base64 = "",
                started_at = "2025-01-01",
                title = "A",
                translated_title = "TestBook"
            )
        )
        coEvery { mockUserRepo.getUserInfo("DEVICE123") } returns retrofit2.Response.success(fakeList)

        launchMain()
        Thread.sleep(800)

        onView(withId(R.id.cardTrashButton)).perform(click())
        Thread.sleep(500)

        onView(withId(R.id.discardPanelInclude)).check(matches(isDisplayed()))
    }

    @Test
    fun discardCancelBtn_hidesDiscardPanel() = runTest {
        val fakeList = listOf(
            UserInfoResponse(
                session_id = "S1",
                user_id = "DEVICE123",
                image_base64 = "",
                started_at = "2025-01-01",
                title = "A",
                translated_title = "TestBook"
            )
        )
        coEvery { mockUserRepo.getUserInfo("DEVICE123") } returns retrofit2.Response.success(fakeList)

        launchMain()
        Thread.sleep(800)

        onView(withId(R.id.cardTrashButton)).perform(click())
        Thread.sleep(500)

        onView(withId(R.id.discardCancelBtn)).perform(click())
        Thread.sleep(500)

        onView(withId(R.id.discardPanelInclude)).check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    @Test
    fun discardConfirmBtn_deletesSession() = runTest {
        val fakeList = listOf(
            UserInfoResponse(
                session_id = "S1",
                user_id = "DEVICE123",
                image_base64 = "",
                started_at = "2025-01-01",
                title = "A",
                translated_title = "TestBook"
            )
        )
        coEvery { mockUserRepo.getUserInfo("DEVICE123") } returns retrofit2.Response.success(fakeList)
        coEvery { mockSessionRepo.discardSession("S1", "DEVICE123") } returns retrofit2.Response.success(Unit)

        launchMain()
        Thread.sleep(800)

        onView(withId(R.id.cardTrashButton)).perform(click())
        Thread.sleep(500)

        onView(withId(R.id.discardConfirmBtn)).perform(click())
        Thread.sleep(500)

        coVerify { mockSessionRepo.discardSession("S1", "DEVICE123") }
        onView(withId(R.id.discardPanelInclude)).check(matches(withEffectiveVisibility(Visibility.GONE)))
    }
}
