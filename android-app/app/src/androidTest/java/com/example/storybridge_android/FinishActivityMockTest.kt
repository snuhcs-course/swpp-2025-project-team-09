package com.example.storybridge_android.ui.session.finish

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
import com.example.storybridge_android.R
import com.example.storybridge_android.ServiceLocator
import com.example.storybridge_android.data.SessionRepository
import com.example.storybridge_android.network.*
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.*
import org.junit.runner.RunWith
import org.hamcrest.Matchers.*

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
            override suspend fun startSession(userId: String) = Result.failure<StartSessionResponse>(Exception("unused"))
            override suspend fun selectVoice(sessionId: String, voiceStyle: String) = Result.failure<SelectVoiceResponse>(Exception("unused"))
            override suspend fun endSession(sessionId: String) = Result.success(EndSessionResponse("S1", "2025-01-01", 5))
            override suspend fun getSessionStats(sessionId: String) = Result.success(
                SessionStatsResponse(
                    "S1", "U1", false, "2025-01-01", "2025-01-01",
                    total_pages = 5, total_time_spent = 80, total_words_read = 200
                )
            )
            override suspend fun reloadAllSession(userId: String, startedAt: String) = Result.failure<ReloadAllSessionResponse>(Exception("unused"))
            override suspend fun discardSession(sessionId: String) = Result.failure<DiscardSessionResponse>(Exception("unused"))

            // 💡 WordPickerResponse 형태로 수정
            override suspend fun pickWords(
                sessionId: String,
                lang: String
            ): Result<WordPickerResponse> {
                return Result.success(WordPickerResponse(
                    session_id = sessionId,
                    status = "ok",
                    items = listOf(
                        WordItem("a", "b"),
                        WordItem("c", "d"),
                        WordItem("e", "f")
                    )
                ))
            }
        }
        ServiceLocator.sessionRepository = mockSessionRepo
        Intents.init()
    }

    @After
    fun teardown() {
        try {
            val scenario = ActivityScenario.launch<FinishActivity>(createIntent())
            scenario.onActivity {
                it.findViewById<FinishBalloonView>(R.id.balloonView)?.stopAnimation()
            }
            scenario.close()
        } catch (_: Exception) {}
        ServiceLocator.reset()
        unmockkAll()
        Intents.release()
    }

    @Test
    fun finishActivity_showsStatsAndMainButton() {
        val scenario = ActivityScenario.launch<FinishActivity>(createIntent())
        Thread.sleep(2000)
        scenario.onActivity {
            it.findViewById<FinishBalloonView>(R.id.balloonView)?.stopAnimation()
        }
        onView(withId(R.id.balloonView)).check(matches(isDisplayed()))
        onView(withId(R.id.mainButton)).check(matches(withEffectiveVisibility(Visibility.INVISIBLE)))
        scenario.close()
    }

    @Test
    fun clickMainButton_existingSession_navigatesToMain() {
        val intent = createIntent().apply { putExtra("is_new_session", false) }
        val scenario = ActivityScenario.launch<FinishActivity>(intent)
        Thread.sleep(2000)
        scenario.onActivity {
            it.findViewById<FinishBalloonView>(R.id.balloonView)?.stopAnimation()
            it.findViewById<android.widget.Button>(R.id.mainButton)?.visibility = android.view.View.VISIBLE
        }
        onView(withId(R.id.mainButton)).perform(click())
        intended(IntentMatchers.hasComponent("com.example.storybridge_android.ui.main.MainActivity"))
        scenario.close()
    }

    @Test
    fun clickMainButton_newSession_navigatesToDecideSave() {
        val intent = createIntent().apply { putExtra("is_new_session", true) }
        val scenario = ActivityScenario.launch<FinishActivity>(intent)
        Thread.sleep(2000)
        scenario.onActivity {
            it.findViewById<FinishBalloonView>(R.id.balloonView)?.stopAnimation()
            it.findViewById<android.widget.Button>(R.id.mainButton)?.visibility = android.view.View.VISIBLE
        }

        onView(withId(R.id.mainButton)).perform(click())

        intended(IntentMatchers.hasComponent("com.example.storybridge_android.ui.session.decide.DecideSaveActivity"))

        scenario.close()
    }

    @Test
    fun sessionStats_minutesAndSeconds_displaysBoth() {
        mockSessionRepo = object : SessionRepository {
            override suspend fun startSession(userId: String) = Result.failure<StartSessionResponse>(Exception("unused"))
            override suspend fun selectVoice(sessionId: String, voiceStyle: String) = Result.failure<SelectVoiceResponse>(Exception("unused"))
            override suspend fun endSession(sessionId: String) = Result.success(EndSessionResponse("S1", "2025-01-01", 3))
            override suspend fun getSessionStats(sessionId: String) = Result.success(
                SessionStatsResponse(
                    "S1", "U1", false, "2025-01-01", "2025-01-01",
                    total_pages = 3, total_time_spent = 90, total_words_read = 150
                )
            )
            override suspend fun reloadAllSession(userId: String, startedAt: String) = Result.failure<ReloadAllSessionResponse>(Exception("unused"))
            override suspend fun discardSession(sessionId: String) = Result.failure<DiscardSessionResponse>(Exception("unused"))
            override suspend fun pickWords(
                sessionId: String,
                lang: String
            ): Result<WordPickerResponse> {
                return Result.success(WordPickerResponse(session_id = sessionId, status = "ok", items = emptyList()))
            }
        }
        ServiceLocator.sessionRepository = mockSessionRepo
        val scenario = ActivityScenario.launch<FinishActivity>(createIntent())
        Thread.sleep(2000)
        scenario.onActivity {
            val v = it.findViewById<FinishBalloonView>(R.id.balloonView)
            v.stopAnimation()
            repeat(3) { idx -> v.onBalloonPopped?.invoke(idx, v.getBalloonText(idx) ?: "") }
        }
        onView(withId(R.id.balloonResultText))
            .check(matches(withText(allOf(containsString("1 minute"), containsString("30 seconds")))))
        scenario.close()
    }

    @Test
    fun sessionStats_zeroSeconds_displaysOnlyMinutes() {
        mockSessionRepo = object : SessionRepository {
            override suspend fun startSession(userId: String) = Result.failure<StartSessionResponse>(Exception("unused"))
            override suspend fun selectVoice(sessionId: String, voiceStyle: String) = Result.failure<SelectVoiceResponse>(Exception("unused"))
            override suspend fun endSession(sessionId: String) = Result.success(EndSessionResponse("S1", "2025-01-01", 5))
            override suspend fun getSessionStats(sessionId: String) = Result.success(
                SessionStatsResponse(
                    "S1", "U1", false, "2025-01-01", "2025-01-01",
                    total_pages = 5, total_time_spent = 120, total_words_read = 200
                )
            )
            override suspend fun reloadAllSession(userId: String, startedAt: String) = Result.failure<ReloadAllSessionResponse>(Exception("unused"))
            override suspend fun discardSession(sessionId: String) = Result.failure<DiscardSessionResponse>(Exception("unused"))
            override suspend fun pickWords(
                sessionId: String,
                lang: String
            ): Result<WordPickerResponse> {
                return Result.success(WordPickerResponse(session_id = sessionId, status = "ok", items = emptyList()))
            }
        }
        ServiceLocator.sessionRepository = mockSessionRepo
        val scenario = ActivityScenario.launch<FinishActivity>(createIntent())
        Thread.sleep(2000)
        scenario.onActivity {
            val v = it.findViewById<FinishBalloonView>(R.id.balloonView)
            v.stopAnimation()
            repeat(3) { idx -> v.onBalloonPopped?.invoke(idx, v.getBalloonText(idx) ?: "") }
        }
        onView(withId(R.id.balloonResultText))
            .check(matches(withText(allOf(containsString("2 minutes"), not(containsString("seconds"))))))
        scenario.close()
    }

    @Test
    fun sessionStats_lessThanOneMinute_displaysOnlySeconds() {
        mockSessionRepo = object : SessionRepository {
            override suspend fun startSession(userId: String) = Result.failure<StartSessionResponse>(Exception("unused"))
            override suspend fun selectVoice(sessionId: String, voiceStyle: String) = Result.failure<SelectVoiceResponse>(Exception("unused"))
            override suspend fun endSession(sessionId: String) = Result.success(EndSessionResponse("S1", "2025-01-01", 5))
            override suspend fun getSessionStats(sessionId: String) = Result.success(
                SessionStatsResponse(
                    "S1", "U1", false, "2025-01-01", "2025-01-01",
                    total_pages = 5, total_time_spent = 45, total_words_read = 200
                )
            )
            override suspend fun reloadAllSession(userId: String, startedAt: String) = Result.failure<ReloadAllSessionResponse>(Exception("unused"))
            override suspend fun discardSession(sessionId: String) = Result.failure<DiscardSessionResponse>(Exception("unused"))
            override suspend fun pickWords(
                sessionId: String,
                lang: String
            ): Result<WordPickerResponse> {
                return Result.success(WordPickerResponse(session_id = sessionId, status = "ok", items = emptyList()))
            }
        }
        ServiceLocator.sessionRepository = mockSessionRepo
        val scenario = ActivityScenario.launch<FinishActivity>(createIntent())
        Thread.sleep(2000)
        scenario.onActivity {
            val v = it.findViewById<FinishBalloonView>(R.id.balloonView)
            v.stopAnimation()
            repeat(3) { idx -> v.onBalloonPopped?.invoke(idx, v.getBalloonText(idx) ?: "") }
        }
        onView(withId(R.id.balloonResultText))
            .check(matches(withText(allOf(containsString("45 seconds"), not(containsString("minute"))))))
        scenario.close()
    }


    @Test
    fun balloonPopping_differentOrder_displaysTextsInCorrectOrder() {
        val scenario = ActivityScenario.launch<FinishActivity>(createIntent())
        Thread.sleep(2000)
        scenario.onActivity {
            val v = it.findViewById<FinishBalloonView>(R.id.balloonView)
            v.stopAnimation()
            val t0 = v.getBalloonText(0) ?: ""
            val t1 = v.getBalloonText(1) ?: ""
            val t2 = v.getBalloonText(2) ?: ""
            v.onBalloonPopped?.invoke(2, t2)
            v.onBalloonPopped?.invoke(0, t0)
            v.onBalloonPopped?.invoke(1, t1)
        }
        onView(withId(R.id.balloonResultText))
            .check(matches(withText(allOf(
                containsString("You read 200 words"),
                containsString("and 4 pages"),
                containsString("for 1 minute 20 seconds")
            ))))
        scenario.close()
    }

    @Test
    fun allBalloonsPopped_showsAmazingTextAndMainButton() {
        val scenario = ActivityScenario.launch<FinishActivity>(createIntent())
        Thread.sleep(2000)
        scenario.onActivity {
            val v = it.findViewById<FinishBalloonView>(R.id.balloonView)
            v.stopAnimation()
            v.onAllBalloonsPopped?.invoke()
        }

        onView(withId(R.id.tapBalloonHint)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.amazingText)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.mainButton)).check(matches(isDisplayed()))

        scenario.close()
    }

    @Test
    fun pickedWords_lessThanThree_hidesWordContainer() {
        mockSessionRepo = object : SessionRepository {
            override suspend fun getSessionStats(sessionId: String) = Result.success(
                SessionStatsResponse("S1", "U1", false, "", "", 5, 80, 200)
            )
            // 💡 WordPickerResponse 형태로 수정 및 2개 단어만 반환
            override suspend fun pickWords(
                sessionId: String,
                lang: String
            ): Result<WordPickerResponse> {
                return Result.success(WordPickerResponse(
                    session_id = sessionId,
                    status = "ok",
                    items = listOf(
                        WordItem("w1", "m1"),
                        WordItem("w2", "m2")
                    )
                ))
            }
            override suspend fun startSession(userId: String) = Result.failure<StartSessionResponse>(Exception("unused"))
            override suspend fun selectVoice(sessionId: String, voiceStyle: String) = Result.failure<SelectVoiceResponse>(Exception("unused"))
            override suspend fun endSession(sessionId: String) = Result.success(EndSessionResponse("S1", "2025-01-01", 5))
            override suspend fun reloadAllSession(userId: String, startedAt: String) = Result.failure<ReloadAllSessionResponse>(Exception("unused"))
            override suspend fun discardSession(sessionId: String) = Result.failure<DiscardSessionResponse>(Exception("unused"))
        }
        ServiceLocator.sessionRepository = mockSessionRepo

        val scenario = ActivityScenario.launch<FinishActivity>(createIntent())
        Thread.sleep(2000)

        scenario.onActivity {
            val v = it.findViewById<FinishBalloonView>(R.id.balloonView)
            v.stopAnimation()
            v.onAllBalloonsPopped?.invoke()
        }

        onView(withId(R.id.learnedWordsContainer))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.learnedWordsTitle))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.mainButton)).check(matches(isDisplayed()))

        scenario.close()
    }

    @Test
    fun balloonTexts_storedInCorrectOrder() {
        val scenario = ActivityScenario.launch<FinishActivity>(createIntent())
        Thread.sleep(1000)
        scenario.onActivity {
            val f = it.javaClass.getDeclaredField("orderedTexts")
            f.isAccessible = true
            val ordered = f.get(it) as List<*>
            Assert.assertEquals(3, ordered.size)
            Assert.assertTrue((ordered[0] as String).contains("word"))
            Assert.assertTrue((ordered[1] as String).contains("page"))
            Assert.assertTrue((ordered[2] as String).contains("minute"))
        }
        scenario.close()
    }
}