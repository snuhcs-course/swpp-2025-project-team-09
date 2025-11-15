package com.example.storybridge_android.ui.session

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.storybridge_android.ServiceLocator
import com.example.storybridge_android.data.SessionRepository
import com.example.storybridge_android.network.*
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class FinishActivityMockTest {

    private lateinit var mockSessionRepo: SessionRepository

    private fun createIntent(): Intent {
        return Intent(
            ApplicationProvider.getApplicationContext(),
            FinishActivity::class.java
        ).apply {
            putExtra("session_id", "S1")
            putExtra("is_new_session", true)
        }
    }

    @Before
    fun setup() {

        mockSessionRepo = object : SessionRepository {

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
                        session_id = "S1",
                        total_pages = 5,
                        ended_at = "2025-01-01"
                    )
                )
            }

            override suspend fun getSessionStats(sessionId: String): Result<SessionStatsResponse> {
                return Result.success(
                    SessionStatsResponse(
                        session_id = "S1",
                        user_id = "U1",
                        isOngoing = false,
                        started_at = "2025-01-01",
                        ended_at = "2025-01-01",
                        total_pages = 5,
                        total_time_spent = 80,
                        total_words_read = 200
                    )
                )
            }

            override suspend fun reloadSession(
                userId: String,
                startedAt: String,
                pageIndex: Int
            ): Result<ReloadSessionResponse> {
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

        ServiceLocator.sessionRepository = mockSessionRepo
    }

    @After
    fun teardown() {
        ServiceLocator.reset()
        unmockkAll()
    }

    @Test
    fun finishActivity_showsStatsAndMainButton() {

        val scenario = ActivityScenario.launch<FinishActivity>(createIntent())

        Thread.sleep(3500)

        onView(withId(com.example.storybridge_android.R.id.sessionSummary))
            .check(matches(isDisplayed()))

        onView(withId(com.example.storybridge_android.R.id.mainButton))
            .check(matches(isDisplayed()))

        scenario.close()
    }

    @Test
    fun clickMainButton_existingSession_navigatesToMain() {
        val intent = Intent(
            ApplicationProvider.getApplicationContext(),
            FinishActivity::class.java
        ).apply {
            putExtra("session_id", "S1")
            putExtra("is_new_session", false)
        }

        Intents.init()
        ActivityScenario.launch<FinishActivity>(intent)

        Thread.sleep(3500)

        onView(withId(com.example.storybridge_android.R.id.mainButton))
            .perform(click())

        intended(
            IntentMatchers.hasComponent(
                "com.example.storybridge_android.ui.main.MainActivity"
            )
        )

        Intents.release()
    }

    @Test
    fun clickMainButton_newSession_navigatesToDecideSave() {
        val intent = Intent(
            ApplicationProvider.getApplicationContext(),
            FinishActivity::class.java
        ).apply {
            putExtra("session_id", "S1")
            putExtra("is_new_session", true)
        }

        Intents.init()
        ActivityScenario.launch<FinishActivity>(intent)

        Thread.sleep(3500)

        onView(withId(com.example.storybridge_android.R.id.mainButton))
            .perform(click())

        intended(
            IntentMatchers.hasComponent(
                "com.example.storybridge_android.ui.session.DecideSaveActivity"
            )
        )

        Intents.release()
    }
}
