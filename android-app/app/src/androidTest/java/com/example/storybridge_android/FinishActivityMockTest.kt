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

        // Stop balloon animation to allow Espresso to be idle
        scenario.onActivity { activity ->
            activity.findViewById<BalloonInteractionView>(
                com.example.storybridge_android.R.id.balloonView
            ).stopAnimation()
        }

        // Check that balloon view is displayed
        onView(withId(com.example.storybridge_android.R.id.balloonView))
            .check(matches(isDisplayed()))

        // Main button exists (will be visible after balloons are popped)
        onView(withId(com.example.storybridge_android.R.id.mainButton))
            .check(matches(withEffectiveVisibility(Visibility.INVISIBLE)))

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
        try {
            val scenario = ActivityScenario.launch<FinishActivity>(intent)
            Thread.sleep(3500)

            // Stop balloon animation and make button visible
            scenario.onActivity { activity ->
                activity.findViewById<BalloonInteractionView>(
                    com.example.storybridge_android.R.id.balloonView
                ).stopAnimation()
                activity.findViewById<android.widget.Button>(
                    com.example.storybridge_android.R.id.mainButton
                ).visibility = android.view.View.VISIBLE
            }

            onView(withId(com.example.storybridge_android.R.id.mainButton))
                .perform(click())

            intended(
                IntentMatchers.hasComponent(
                    "com.example.storybridge_android.ui.main.MainActivity"
                )
            )

            scenario.close()
        } finally {
            Intents.release()
        }
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
        try {
            val scenario = ActivityScenario.launch<FinishActivity>(intent)
            Thread.sleep(3500)

            // Stop balloon animation and make button visible
            scenario.onActivity { activity ->
                activity.findViewById<BalloonInteractionView>(
                    com.example.storybridge_android.R.id.balloonView
                ).stopAnimation()
                activity.findViewById<android.widget.Button>(
                    com.example.storybridge_android.R.id.mainButton
                ).visibility = android.view.View.VISIBLE
            }

            onView(withId(com.example.storybridge_android.R.id.mainButton))
                .perform(click())

            intended(
                IntentMatchers.hasComponent(
                    "com.example.storybridge_android.ui.session.DecideSaveActivity"
                )
            )

            scenario.close()
        } finally {
            Intents.release()
        }
    }

    @Test
    fun sessionStats_minutesAndSeconds_displaysBoth() {
        // GIVEN: 1분 30초
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
                        total_pages = 3,
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
                        total_pages = 3,
                        total_time_spent = 90,  // 1분 30초
                        total_words_read = 150
                    )
                )
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

        val scenario = ActivityScenario.launch<FinishActivity>(createIntent())
        Thread.sleep(3500)

        // Stop animation and manually trigger balloon pops to reveal text
        scenario.onActivity { activity ->
            val balloonView = activity.findViewById<BalloonInteractionView>(
                com.example.storybridge_android.R.id.balloonView
            )
            balloonView.stopAnimation()

            // Get actual balloon text and trigger callbacks
            val text0 = balloonView.getBalloonText(0) ?: ""
            val text1 = balloonView.getBalloonText(1) ?: ""
            val text2 = balloonView.getBalloonText(2) ?: ""

            balloonView.onBalloonPopped?.invoke(0, text0)
            balloonView.onBalloonPopped?.invoke(1, text1)
            balloonView.onBalloonPopped?.invoke(2, text2)
        }

        // THEN: Verify the combined text contains "1 minute" and "30 seconds"
        onView(withId(com.example.storybridge_android.R.id.balloonResultText))
            .check(matches(withText(org.hamcrest.Matchers.allOf(
                org.hamcrest.Matchers.containsString("1 minute"),
                org.hamcrest.Matchers.containsString("30 seconds")
            ))))

        scenario.close()
    }

    @Test
    fun sessionStats_singlePage_displaysSingularPage() {
        // GIVEN: 2 total_pages (1 page after subtracting cover)
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
                        total_pages = 2,  // 1 page after -1
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
                        total_pages = 2,
                        total_time_spent = 60,
                        total_words_read = 50
                    )
                )
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

        val scenario = ActivityScenario.launch<FinishActivity>(createIntent())
        Thread.sleep(3500)

        // Stop animation and manually trigger balloon pops
        scenario.onActivity { activity ->
            val balloonView = activity.findViewById<BalloonInteractionView>(
                com.example.storybridge_android.R.id.balloonView
            )
            balloonView.stopAnimation()

            // Get actual balloon text and trigger callbacks
            val text0 = balloonView.getBalloonText(0) ?: ""
            val text1 = balloonView.getBalloonText(1) ?: ""
            val text2 = balloonView.getBalloonText(2) ?: ""

            balloonView.onBalloonPopped?.invoke(0, text0)
            balloonView.onBalloonPopped?.invoke(1, text1)
            balloonView.onBalloonPopped?.invoke(2, text2)
        }

        // THEN: "1 page" (singular)
        onView(withId(com.example.storybridge_android.R.id.balloonResultText))
            .check(matches(withText(org.hamcrest.Matchers.containsString("1 page"))))

        scenario.close()
    }

    @Test
    fun sessionStats_oneMinute_displaysSingularMinute() {
        // GIVEN: 1 min
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
                        total_pages = 2,
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
                        total_pages = 2,
                        total_time_spent = 60,  // 1분
                        total_words_read = 80
                    )
                )
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

        val scenario = ActivityScenario.launch<FinishActivity>(createIntent())
        Thread.sleep(3500)

        // Stop animation and manually trigger balloon pops
        scenario.onActivity { activity ->
            val balloonView = activity.findViewById<BalloonInteractionView>(
                com.example.storybridge_android.R.id.balloonView
            )
            balloonView.stopAnimation()

            // Get actual balloon text and trigger callbacks
            val text0 = balloonView.getBalloonText(0) ?: ""
            val text1 = balloonView.getBalloonText(1) ?: ""
            val text2 = balloonView.getBalloonText(2) ?: ""

            balloonView.onBalloonPopped?.invoke(0, text0)
            balloonView.onBalloonPopped?.invoke(1, text1)
            balloonView.onBalloonPopped?.invoke(2, text2)
        }

        // THEN: "1 minute" (singular)
        onView(withId(com.example.storybridge_android.R.id.balloonResultText))
            .check(matches(withText(org.hamcrest.Matchers.containsString("1 minute"))))

        scenario.close()
    }

    @Test
    fun sessionStats_oneSecond_displaysSingularSecond() {
        // GIVEN: 1 sec
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
                        total_pages = 2,
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
                        total_pages = 2,
                        total_time_spent = 1,  // 1초
                        total_words_read = 5
                    )
                )
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

        val scenario = ActivityScenario.launch<FinishActivity>(createIntent())
        Thread.sleep(3500)

        // Stop animation and manually trigger balloon pops
        scenario.onActivity { activity ->
            val balloonView = activity.findViewById<BalloonInteractionView>(
                com.example.storybridge_android.R.id.balloonView
            )
            balloonView.stopAnimation()

            // Get actual balloon text and trigger callbacks
            val text0 = balloonView.getBalloonText(0) ?: ""
            val text1 = balloonView.getBalloonText(1) ?: ""
            val text2 = balloonView.getBalloonText(2) ?: ""

            balloonView.onBalloonPopped?.invoke(0, text0)
            balloonView.onBalloonPopped?.invoke(1, text1)
            balloonView.onBalloonPopped?.invoke(2, text2)
        }

        // THEN: "1 second" (singular)
        onView(withId(com.example.storybridge_android.R.id.balloonResultText))
            .check(matches(withText(org.hamcrest.Matchers.containsString("1 second"))))

        scenario.close()
    }
}
