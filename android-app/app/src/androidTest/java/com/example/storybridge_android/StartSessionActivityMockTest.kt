package com.example.storybridge_android

import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.storybridge_android.data.SessionRepository
import com.example.storybridge_android.network.DiscardSessionResponse
import com.example.storybridge_android.network.EndSessionResponse
import com.example.storybridge_android.network.ReloadAllSessionResponse
import com.example.storybridge_android.network.SelectVoiceResponse
import com.example.storybridge_android.network.SessionStatsResponse
import com.example.storybridge_android.network.StartSessionResponse
import com.example.storybridge_android.network.WordPickerResponse
import com.example.storybridge_android.ui.session.start.StartSessionActivity
import io.mockk.coEvery
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.*
import org.junit.runner.RunWith
import org.hamcrest.CoreMatchers.allOf

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class StartSessionActivityMockTest {

    private lateinit var fakeRepo: SessionRepository

    private fun launch(): ActivityScenario<StartSessionActivity> {
        return ActivityScenario.launch(
            Intent(
                ApplicationProvider.getApplicationContext(),
                StartSessionActivity::class.java
            ).apply {
                putExtra("session_id", "S1_FROM_VOICE_SELECT")
            }
        )
    }

    @Before
    fun setup() {
        mockkStatic(Settings.Secure::class)
        coEvery {
            Settings.Secure.getString(any(), Settings.Secure.ANDROID_ID)
        } returns "DEVICE123"

        fakeRepo = object : SessionRepository {
            override suspend fun startSession(userId: String) =
                Result.success(StartSessionResponse("S1", "2025-01-01", 1))

            override suspend fun selectVoice(
                sessionId: String,
                voiceStyle: String
            ): Result<SelectVoiceResponse> {
                return Result.failure(Exception("unused"))
            }
            override suspend fun endSession(
                sessionId: String
            ): Result<EndSessionResponse> {
                return Result.failure(Exception("unused"))
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

            override suspend fun pickWords(
                sessionId: String,
                lang: String
            ): Result<WordPickerResponse> {
                return Result.failure(Exception("unused"))
            }
        }

        ServiceLocator.sessionRepository = fakeRepo
        Intents.init()
    }

    @After
    fun teardown() {
        unmockkAll()
        Intents.release()
    }

    @Test
    fun clickButton_andNavigateToCamera() {
        val expectedSessionId = "S1_FROM_VOICE_SELECT"

        val scenario = launch()
        Thread.sleep(700)

        onView(withId(R.id.startSessionButton)).perform(click())
        Thread.sleep(1500)

        Intents.intended(
            allOf(
                IntentMatchers.hasComponent("com.example.storybridge_android.ui.camera.CameraSessionActivity"),
                IntentMatchers.hasExtra("session_id", expectedSessionId),
                IntentMatchers.hasExtra("page_index", 0),
                IntentMatchers.hasExtra("is_cover", true)
            )
        )

        scenario.close()
    }

    @Test
    fun missingSessionId_finishesActivity() {
        val intentWithoutSessionId = Intent(
            ApplicationProvider.getApplicationContext(),
            StartSessionActivity::class.java
        )

        val scenario = ActivityScenario.launch<StartSessionActivity>(intentWithoutSessionId)
        Thread.sleep(500)

        assert(scenario.state.isAtLeast(Lifecycle.State.DESTROYED))

        scenario.close()
    }

    @Test
    fun clickBackButton_navigatesToVoiceSelection() {
        val expectedSessionId = "S1_FROM_VOICE_SELECT"

        val scenario = launch()
        Thread.sleep(500)

        scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        Thread.sleep(1000)

        Intents.intended(
            allOf(
                IntentMatchers.hasComponent("com.example.storybridge_android.ui.session.voice.VoiceSelectActivity"),
                IntentMatchers.hasExtra("session_id", expectedSessionId)
            )
        )

        scenario.close()
    }
}
