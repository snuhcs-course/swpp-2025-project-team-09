package com.example.storybridge_android.ui.camera

import android.Manifest
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CameraActivityMockTest {
    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.CAMERA
    )

    private lateinit var scenario: ActivityScenario<CameraActivity>
    private val mockViewModel = mockk<CameraViewModel>(relaxed = true)

    @Before
    fun setup() {
        CameraViewModelFactoryHelper.fake = null
    }

    @After
    fun tearDown() {
        unmockkAll()
        if (this::scenario.isInitialized) scenario.close()
    }

    @Test
    fun onCreate_googlePlayServicesMissing_finishesActivity() {
        // GIVEN: Google Play Services가 없음
        every { mockViewModel.checkGooglePlayServices() } returns false
        CameraViewModelFactoryHelper.fake = mockViewModel

        // WHEN: Activity 실행
        scenario = ActivityScenario.launch(CameraActivity::class.java)
        Thread.sleep(500)

        // THEN: Activity가 종료됨
        assert(scenario.state.isAtLeast(Lifecycle.State.DESTROYED)) {
            "Activity should be destroyed when Google Play Services is missing"
        }
    }

    @Test
    fun onCreate_permissionAlreadyGranted_callsInitScanner() {
        // GIVEN: Google Play Services 있음 (권한은 Rule로 자동 부여됨)
        every { mockViewModel.checkGooglePlayServices() } returns true
        every { mockViewModel.checkModuleAndInitScanner() } just Runs

        CameraViewModelFactoryHelper.fake = mockViewModel

        // WHEN: Activity 실행
        scenario = ActivityScenario.launch(CameraActivity::class.java)
        Thread.sleep(1000)

        // THEN: initScanner 호출 확인
        verify(timeout = 2000) { mockViewModel.checkModuleAndInitScanner() }
    }

    @Test
    fun uiState_ready_callsStartScan() {
        // GIVEN: Ready 상태의 UI
        every { mockViewModel.checkGooglePlayServices() } returns true
        every { mockViewModel.checkModuleAndInitScanner() } just Runs

        val flow = MutableStateFlow(
            CameraUiState(isInstalling = false, isReady = true)
        )
        every { mockViewModel.uiState } returns flow.asStateFlow()
        every { mockViewModel.consumeReadyFlag() } just Runs

        CameraViewModelFactoryHelper.fake = mockViewModel

        // WHEN: Activity 실행
        scenario = ActivityScenario.launch(CameraActivity::class.java)
        Thread.sleep(1000)

        // THEN: consumeReadyFlag 호출 확인
        verify(timeout = 2000) { mockViewModel.consumeReadyFlag() }
    }

    @Test
    fun uiState_notReady_doesNotCallStartScan() {
        // GIVEN: Not ready 상태
        every { mockViewModel.checkGooglePlayServices() } returns true
        every { mockViewModel.checkModuleAndInitScanner() } just Runs

        val flow = MutableStateFlow(
            CameraUiState(isInstalling = false, isReady = false)
        )
        every { mockViewModel.uiState } returns flow.asStateFlow()

        CameraViewModelFactoryHelper.fake = mockViewModel

        // WHEN: Activity 실행
        scenario = ActivityScenario.launch(CameraActivity::class.java)
        Thread.sleep(1000)

        // THEN: consumeReadyFlag 호출 x
        verify(exactly = 0) { mockViewModel.consumeReadyFlag() }
    }
}