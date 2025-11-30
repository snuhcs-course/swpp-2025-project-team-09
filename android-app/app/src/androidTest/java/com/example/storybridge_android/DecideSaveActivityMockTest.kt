package com.example.storybridge_android.ui.session

import android.content.Intent
import android.provider.Settings
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.storybridge_android.R
import com.example.storybridge_android.ServiceLocator
import com.example.storybridge_android.data.SessionRepository
import com.example.storybridge_android.network.*
import com.example.storybridge_android.ui.session.decide.DecideSaveActivity
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class DecideSaveActivityMockTest {

    private fun launch(): ActivityScenario<DecideSaveActivity> {
        val intent = Intent(
            ApplicationProvider.getApplicationContext(),
            DecideSaveActivity::class.java
        ).apply {
            putExtra("session_id", "S123")
        }
        return ActivityScenario.launch(intent)
    }

    @Before
    fun setup() {
        mockkStatic(Settings.Secure::class)
        every { Settings.Secure.getString(any(), Settings.Secure.ANDROID_ID) } returns "DEVICE123"
    }

    @After
    fun teardown() {
        ServiceLocator.reset()
        unmockkAll()
    }

    @Test
    fun clickSave_showMainButton_thenNavigateOnConfirm() {
        // GIVEN: Mock successful endSession (not needed for save, but required for interface)
        val fakeRepo = object : SessionRepository {

            override suspend fun startSession(userId: String): Result<StartSessionResponse> {
                return Result.failure(Exception("unused"))
            }

            override suspend fun selectVoice(
                sessionId: String,
                voiceStyle: String
            ): Result<SelectVoiceResponse> {
                return Result.failure(Exception("unused"))
            }

            override suspend fun endSession(sessionId: String): Result<EndSessionResponse> {
                return Result.success(
                    EndSessionResponse(
                        session_id = sessionId,
                        ended_at = "2025-01-01T00:00:00",
                        total_pages = 5
                    )
                )
            }

            override suspend fun getSessionStats(
                sessionId: String
            ): Result<SessionStatsResponse> {
                return Result.failure(Exception("unused"))
            }

            override suspend fun reloadAllSession(
                userId: String,
                startedAt: String
            ): Result<ReloadAllSessionResponse> {
                return Result.failure(Exception("unused"))
            }

            override suspend fun discardSession(sessionId: String): Result<DiscardSessionResponse> {
                return Result.failure(Exception("unused"))
            }
        }

        ServiceLocator.sessionRepository = fakeRepo

        // WHEN: Launch activity and click Save button
        val scenario = launch()
        Thread.sleep(500)

        // Main button should be disabled initially
        onView(withId(R.id.mainButton)).check(matches(isNotEnabled()))

        // Click Save button
        onView(withId(R.id.btnSave)).perform(click())
        Thread.sleep(500)

        // THEN: Verify main button is displayed and enabled
        onView(withId(R.id.mainButton))
            .check(matches(isDisplayed()))
            .check(matches(isEnabled()))

        scenario.close()
    }

    @Test
    fun clickDiscard_showMainButton_thenNavigateOnConfirm() {
        // GIVEN: Mock successful discardSession
        val fakeRepo = object : SessionRepository {

            override suspend fun startSession(userId: String): Result<StartSessionResponse> {
                return Result.failure(Exception("unused"))
            }

            override suspend fun selectVoice(sessionId: String, voiceStyle: String): Result<SelectVoiceResponse> {
                return Result.failure(Exception("unused"))
            }

            override suspend fun endSession(sessionId: String): Result<EndSessionResponse> {
                return Result.failure(Exception("unused"))
            }

            override suspend fun getSessionStats(sessionId: String): Result<SessionStatsResponse> {
                return Result.failure(Exception("unused"))
            }

            override suspend fun reloadAllSession(userId: String, startedAt: String): Result<ReloadAllSessionResponse> {
                return Result.failure(Exception("unused"))
            }

            override suspend fun discardSession(sessionId: String): Result<DiscardSessionResponse> {
                return Result.success(DiscardSessionResponse("discarded"))
            }
        }

        ServiceLocator.sessionRepository = fakeRepo

        // WHEN: Launch activity and click Discard button
        val scenario = launch()
        Thread.sleep(500)

        // Main button should be disabled initially
        onView(withId(R.id.mainButton)).check(matches(isNotEnabled()))

        // Click Discard button
        onView(withId(R.id.btnDiscard)).perform(click())
        Thread.sleep(500)

        // THEN: Verify main button is displayed and enabled
        onView(withId(R.id.mainButton))
            .check(matches(isDisplayed()))
            .check(matches(isEnabled()))

        scenario.close()
    }

    @Test
    fun discardError_buttonsReset_andClickableAgain() {
        // GIVEN: Mock failed discardSession
        val fakeRepo = object : SessionRepository {

            override suspend fun startSession(userId: String): Result<StartSessionResponse> {
                return Result.failure(Exception("unused"))
            }

            override suspend fun selectVoice(sessionId: String, voiceStyle: String): Result<SelectVoiceResponse> {
                return Result.failure(Exception("unused"))
            }

            override suspend fun endSession(sessionId: String): Result<EndSessionResponse> {
                return Result.failure(Exception("unused"))
            }

            override suspend fun getSessionStats(sessionId: String): Result<SessionStatsResponse> {
                return Result.failure(Exception("unused"))
            }

            override suspend fun reloadAllSession(userId: String, startedAt: String): Result<ReloadAllSessionResponse> {
                return Result.failure(Exception("unused"))
            }

            override suspend fun discardSession(sessionId: String): Result<DiscardSessionResponse> {
                return Result.failure(Exception("DISCARD FAIL"))
            }
        }

        ServiceLocator.sessionRepository = fakeRepo

        // WHEN: Launch activity and attempt discard (fails)
        val scenario = launch()
        Thread.sleep(500)

        // Click Discard button → mainButton shows
        onView(withId(R.id.btnDiscard)).perform(click())
        Thread.sleep(500)
        onView(withId(R.id.mainButton)).check(matches(isDisplayed()))

        // Click mainButton → discard fails → mainButton hidden
        onView(withId(R.id.mainButton)).perform(click())
        Thread.sleep(1000)
        onView(withId(R.id.mainButton)).check(matches(isNotEnabled()))

        // THEN: Verify button is clickable again after failure
        onView(withId(R.id.btnDiscard)).perform(click())
        Thread.sleep(500)
        onView(withId(R.id.mainButton)).check(matches(isDisplayed()))

        scenario.close()
    }
}