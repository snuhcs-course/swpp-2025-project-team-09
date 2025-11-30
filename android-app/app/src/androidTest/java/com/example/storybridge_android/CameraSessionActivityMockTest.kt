package com.example.storybridge_android.ui.camera

import android.Manifest
import android.content.Intent
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.example.storybridge_android.ui.session.loading.LoadingActivity
import com.example.storybridge_android.ui.session.voice.VoiceSelectActivity
import com.example.storybridge_android.ui.setting.AppSettings
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CameraSessionActivityMockTest {
    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.CAMERA
    )

    private lateinit var scenario: ActivityScenario<CameraSessionActivity>
    private val mockViewModel = mockk<CameraSessionViewModel>(relaxed = true)

    @Before
    fun setup() {
        CameraSessionActivity.testMode = true
        CameraSessionActivity.testViewModelFactory =
            object : androidx.lifecycle.ViewModelProvider.Factory {
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return mockViewModel as T
                }
            }
        Intents.init()
    }

    @After
    fun teardown() {
        if (::scenario.isInitialized) scenario.close()
        CameraSessionActivity.testMode = false
        CameraSessionActivity.testViewModelFactory = null
        Intents.release()
    }

    private fun launchWithExtras(
        sessionId: String? = "S1",
        pageIndex: Int = 1,
        isCover: Boolean = false
    ): ActivityScenario<CameraSessionActivity> {
        val intent = Intent(
            ApplicationProvider.getApplicationContext(),
            CameraSessionActivity::class.java
        ).apply {
            if (sessionId != null) putExtra("session_id", sessionId)
            putExtra("page_index", pageIndex)
            putExtra("is_cover", isCover)
        }
        return ActivityScenario.launch(intent)
    }

    @Test
    fun missingSessionId_finishesActivity() {
        scenario = launchWithExtras(sessionId = null)
        Thread.sleep(500)

        assert(scenario.state == Lifecycle.State.DESTROYED)
    }

    @Test
    fun uiState_success_coverNavigatesToVoiceSelect() {
        val flow = MutableStateFlow<SessionUiState>(SessionUiState.Idle)
        every { mockViewModel.uiState } returns flow.asStateFlow()

        scenario = launchWithExtras(isCover = true)
        Thread.sleep(500)

        // UI state changed
        flow.value = SessionUiState.Success("/tmp/a.jpg")
        Thread.sleep(1000)

        Intents.intended(IntentMatchers.hasComponent(VoiceSelectActivity::class.java.name))
    }

    @Test
    fun uiState_success_normalNavigatesToLoading() {
        val flow = MutableStateFlow<SessionUiState>(SessionUiState.Idle)
        every { mockViewModel.uiState } returns flow.asStateFlow()

        scenario = launchWithExtras(isCover = false)
        Thread.sleep(500)

        // UI state changed
        flow.value = SessionUiState.Success("/tmp/img.png")
        Thread.sleep(1000)

        Intents.intended(IntentMatchers.hasComponent(LoadingActivity::class.java.name))
    }

    @Test
    fun uiState_cancelled_finishes() {
        val flow = MutableStateFlow<SessionUiState>(SessionUiState.Idle)
        every { mockViewModel.uiState } returns flow.asStateFlow()

        scenario = launchWithExtras()
        Thread.sleep(500)

        // UI state changed
        flow.value = SessionUiState.Cancelled
        Thread.sleep(500)

        assert(scenario.state == Lifecycle.State.DESTROYED)
    }

    @Test
    fun navigateToVoiceSelect_includesLanguageSetting() {
        // GIVEN: AppSettings mock
        mockkObject(AppSettings)
        every { AppSettings.getLanguage(any()) } returns "zh"

        val flow = MutableStateFlow<SessionUiState>(SessionUiState.Idle)
        every { mockViewModel.uiState } returns flow.asStateFlow()

        // WHEN: Cover mode Success
        scenario = launchWithExtras(isCover = true, sessionId = "S1")
        Thread.sleep(500)

        flow.value = SessionUiState.Success("/tmp/cover.jpg")
        Thread.sleep(1000)

        // THEN: language setting is passed to VoiceSelectActivity
        Intents.intended(
            IntentMatchers.hasExtra("lang", "zh")
        )

        unmockkObject(AppSettings)
    }

    @Test
    fun navigateToLoading_includesAllRequiredExtras() {
        // GIVEN: AppSettings mock
        mockkObject(AppSettings)
        every { AppSettings.getLanguage(any()) } returns "en"

        val flow = MutableStateFlow<SessionUiState>(SessionUiState.Idle)
        every { mockViewModel.uiState } returns flow.asStateFlow()

        scenario = launchWithExtras(
            isCover = false,
            sessionId = "S123",
            pageIndex = 5
        )
        Thread.sleep(500)

        flow.value = SessionUiState.Success("/tmp/page5.jpg")
        Thread.sleep(1000)

        // THEN: move to LoadingActivity
        Intents.intended(IntentMatchers.hasComponent(LoadingActivity::class.java.name))

        val loadingIntents = Intents.getIntents().filter {
            it.component?.className == LoadingActivity::class.java.name
        }

        assert(loadingIntents.size == 1) { "Should have exactly 1 LoadingActivity intent" }

        val intent = loadingIntents.first()
        assert(intent.getStringExtra("session_id") == "S123")
        assert(intent.getIntExtra("page_index", -1) == 5)
        assert(intent.getStringExtra("image_path") == "/tmp/page5.jpg")
        assert(intent.getBooleanExtra("is_cover", true) == false)
        assert(intent.getStringExtra("lang") == "en")

        unmockkObject(AppSettings)
    }

    @Test
    fun multipleStateChanges_finalSuccessNavigates() {
        val flow = MutableStateFlow<SessionUiState>(SessionUiState.Idle)
        every { mockViewModel.uiState } returns flow.asStateFlow()

        scenario = launchWithExtras(isCover = false)
        Thread.sleep(500)

        // WHEN: Change state to Success
        flow.value = SessionUiState.Success("/tmp/final.jpg")
        Thread.sleep(1000)

        // THEN: Move to LoadingActivity
        val loadingIntents = Intents.getIntents().filter {
            it.component?.className == LoadingActivity::class.java.name
        }

        assert(loadingIntents.size == 1)
        assert(loadingIntents.first().getStringExtra("image_path") == "/tmp/final.jpg")
    }
}