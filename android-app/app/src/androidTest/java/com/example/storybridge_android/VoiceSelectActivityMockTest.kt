package com.example.storybridge_android.ui.session

import android.content.Intent
import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import com.example.storybridge_android.R
import com.example.storybridge_android.ServiceLocator
import com.example.storybridge_android.data.MALE_VOICE
import com.example.storybridge_android.data.FEMALE_VOICE
import com.example.storybridge_android.data.ProcessRepository
import com.example.storybridge_android.data.SessionRepository
import com.example.storybridge_android.network.*
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class VoiceSelectActivityTest {
    private var scenario: ActivityScenario<VoiceSelectActivity>? = null
    private var maleSelected = false
    private var femaleSelected = false
    private var selectVoiceCallCount = 0

    @Before
    fun setup() {
        // 초기화
        maleSelected = false
        femaleSelected = false
        selectVoiceCallCount = 0

        // Fake SessionRepository
        val fakeSessionRepo = object : SessionRepository {
            override suspend fun startSession(userId: String) =
                Result.failure<StartSessionResponse>(Exception("unused"))

            override suspend fun selectVoice(sessionId: String, voiceStyle: String): Result<SelectVoiceResponse> {
                selectVoiceCallCount++
                Log.d("TEST", "selectVoice called: sessionId=$sessionId, voiceStyle=$voiceStyle")

                // 실제 상수 값과 비교
                when (voiceStyle) {
                    MALE_VOICE -> maleSelected = true
                    FEMALE_VOICE -> femaleSelected = true
                }

                return Result.success(
                    SelectVoiceResponse(
                        session_id = sessionId,
                        voice_style = voiceStyle,
                    )
                )
            }

            override suspend fun endSession(sessionId: String) =
                Result.failure<EndSessionResponse>(Exception("unused"))

            override suspend fun getSessionStats(sessionId: String) =
                Result.failure<SessionStatsResponse>(Exception("unused"))

            override suspend fun reloadAllSession(userId: String, startedAt: String) =
                Result.failure<ReloadAllSessionResponse>(Exception("unused"))

            override suspend fun discardSession(sessionId: String) =
                Result.failure<DiscardSessionResponse>(Exception("unused"))
        }

        // Fake ProcessRepository
        val fakeProcessRepo = object : ProcessRepository {
            override suspend fun uploadImage(request: UploadImageRequest) =
                Result.failure<UploadImageResponse>(Exception("unused"))

            override suspend fun uploadCoverImage(request: UploadImageRequest): Result<UploadCoverResponse> {
                return Result.success(
                    UploadCoverResponse(
                        session_id = request.session_id,
                        page_index = 0,
                        status = "ok",
                        title = "Test Title",
                        translated_title = "Test Title",
                        submitted_at = "2025-01-01",
                    )
                )
            }

            override suspend fun checkOcrStatus(sessionId: String, pageIndex: Int) =
                Result.failure<CheckOcrResponse>(Exception("unused"))

            override suspend fun checkTtsStatus(
                sessionId: String,
                pageIndex: Int
            ) = Result.failure<CheckTtsResponse>(Exception("unused"))
        }

        ServiceLocator.sessionRepository = fakeSessionRepo
        ServiceLocator.processRepository = fakeProcessRepo

        val intent = Intent(
            ApplicationProvider.getApplicationContext(),
            VoiceSelectActivity::class.java
        ).apply {
            putExtra("session_id", "abc-123")
            putExtra("image_path", "/tmp/x.jpg")
            putExtra("lang", "en")
        }

        scenario = ActivityScenario.launch(intent)
        Thread.sleep(1000)
    }

    @After
    fun teardown() {
        scenario?.close()
        ServiceLocator.reset()
        unmockkAll()
    }

    @Test
    fun testButtonsTextSetCorrectly() {
        onView(withId(R.id.manButton))
            .check(matches(withText("Man")))

        onView(withId(R.id.womanButton))
            .check(matches(withText("Woman")))

        onView(withId(R.id.nextButton))
            .check(matches(withText("Next")))
    }

    @Test
    fun testSelectMaleVoiceCallsRepository() {
        Log.d("TEST", "MALE_VOICE constant = $MALE_VOICE")

        // WHEN: Man 버튼 클릭
        onView(withId(R.id.manButton))
            .check(matches(isDisplayed()))
            .perform(click())

        Thread.sleep(2000)

        Log.d("TEST", "selectVoiceCallCount: $selectVoiceCallCount, maleSelected: $maleSelected")

        // THEN
        assert(selectVoiceCallCount > 0) { "selectVoice should be called. Call count: $selectVoiceCallCount" }
        assert(maleSelected) { "Male voice should be selected. MALE_VOICE=$MALE_VOICE" }
    }

    @Test
    fun testSelectFemaleVoiceCallsRepository() {
        Log.d("TEST", "FEMALE_VOICE constant = $FEMALE_VOICE")

        // WHEN: Woman 버튼 클릭
        onView(withId(R.id.womanButton))
            .check(matches(isDisplayed()))
            .perform(click())

        Thread.sleep(2000)

        Log.d("TEST", "selectVoiceCallCount: $selectVoiceCallCount, femaleSelected: $femaleSelected")

        // THEN
        assert(selectVoiceCallCount > 0) { "selectVoice should be called. Call count: $selectVoiceCallCount" }
        assert(femaleSelected) { "Female voice should be selected. FEMALE_VOICE=$FEMALE_VOICE" }
    }

    @Test
    fun testNextButtonEnabledAfterSelectingVoice() {
        // GIVEN: Next button is not enabled
        onView(withId(R.id.nextButton))
            .check(matches(isNotEnabled()))

        // WHEN: Woman button is clicked
        onView(withId(R.id.womanButton)).perform(click())
        Thread.sleep(2000)

        // THEN: Next button enabled
        onView(withId(R.id.nextButton))
            .check(matches(isEnabled()))
    }
}