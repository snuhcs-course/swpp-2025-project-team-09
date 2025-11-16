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
import com.example.storybridge_android.ui.session.LoadingActivity
import com.example.storybridge_android.ui.session.VoiceSelectActivity
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CameraSessionActivityMockTest {
    // 권한 자동 부여
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

        // UI 상태 변경
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

        // UI 상태 변경
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

        // UI 상태 변경
        flow.value = SessionUiState.Cancelled
        Thread.sleep(500)

        assert(scenario.state == Lifecycle.State.DESTROYED)
    }

    @Test
    fun uiState_error_finishes() {
        val flow = MutableStateFlow<SessionUiState>(SessionUiState.Idle)
        every { mockViewModel.uiState } returns flow.asStateFlow()

        scenario = launchWithExtras()
        Thread.sleep(300)

        flow.value = SessionUiState.Error("Test error")
        Thread.sleep(500)

        assert(scenario.state == Lifecycle.State.DESTROYED)
    }
}