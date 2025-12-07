package com.example.storybridge_android.view_model

import android.app.Activity
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.storybridge_android.data.SessionRepository
import com.example.storybridge_android.network.DiscardSessionResponse
import com.example.storybridge_android.network.EndSessionResponse
import com.example.storybridge_android.network.ReloadAllSessionResponse
import com.example.storybridge_android.network.SelectVoiceResponse
import com.example.storybridge_android.network.SessionStatsResponse
import com.example.storybridge_android.network.StartSessionResponse
import com.example.storybridge_android.data.ProcessRepository
import com.example.storybridge_android.network.CheckOcrResponse
import com.example.storybridge_android.network.CheckTtsResponse
import com.example.storybridge_android.network.UploadCoverResponse
import com.example.storybridge_android.network.UploadImageRequest
import com.example.storybridge_android.network.UploadImageResponse
import com.example.storybridge_android.network.WordPickerResponse
import com.example.storybridge_android.ui.camera.CameraSessionViewModel
import com.example.storybridge_android.ui.camera.SessionUiState
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


/**
 * Unit tests for CameraSessionViewModel.
 * This ViewModel is simple, so we are just testing the state transitions
 * based on the results from handleCameraResult.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CameraSessionViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Before
    fun mockLog() {
        mockkStatic(android.util.Log::class)
        every { android.util.Log.d(any(), any()) } returns 0
    }

    @After
    fun unmockLog() {
        unmockkAll()
    }

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: CameraSessionViewModel

    private class FakeSessionRepository : SessionRepository {

        override suspend fun startSession(userId: String) =
            Result.failure<StartSessionResponse>(NotImplementedError())

        override suspend fun selectVoice(
            sessionId: String,
            voiceStyle: String
        ) = Result.failure<SelectVoiceResponse>(NotImplementedError())

        override suspend fun endSession(sessionId: String) =
            Result.failure<EndSessionResponse>(NotImplementedError())

        override suspend fun getSessionStats(sessionId: String) =
            Result.failure<SessionStatsResponse>(NotImplementedError())

        override suspend fun reloadAllSession(
            userId: String,
            startedAt: String
        ) = Result.failure<ReloadAllSessionResponse>(NotImplementedError())

        override suspend fun discardSession(sessionId: String): Result<DiscardSessionResponse> {
            return Result.success(
                DiscardSessionResponse(
                    message = "discard"
                )
            )
        }
        override suspend fun pickWords(
            sessionId: String,
            lang: String
        ) = Result.failure<WordPickerResponse>(NotImplementedError())
    }

    class FakeProcessRepository : ProcessRepository {

        override suspend fun uploadImage(req: UploadImageRequest): Result<UploadImageResponse> {
            return Result.failure(NotImplementedError())
        }

        override suspend fun uploadCoverImage(req: UploadImageRequest): Result<UploadCoverResponse> {
            return Result.failure(NotImplementedError())
        }

        override suspend fun checkOcrStatus(
            sessionId: String,
            pageIndex: Int
        ): Result<CheckOcrResponse> {
            return Result.failure(NotImplementedError())
        }

        override suspend fun checkTtsStatus(
            sessionId: String,
            pageIndex: Int
        ): Result<CheckTtsResponse> {
            return Result.failure(NotImplementedError())
        }
    }


    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = CameraSessionViewModel(FakeSessionRepository(), FakeProcessRepository())
    }


    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_isIdle() = runTest {
        assertEquals(SessionUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun handleCameraResult_withResultOkAndImagePath_setsSuccessState() = runTest {
        val fakeImagePath = "path/to/image.jpg"

        // Act
        viewModel.handleCameraResult(Activity.RESULT_OK, fakeImagePath)

        // Assert
        val state = viewModel.uiState.value
        assertTrue(state is SessionUiState.Success)
        assertEquals(fakeImagePath, state.imagePath)
    }

    @Test
    fun handleCameraResult_withResultCancelled_setsCancelledState() = runTest {
        // Act
        viewModel.handleCameraResult(Activity.RESULT_CANCELED, null)

        // Assert
        assertEquals(SessionUiState.Cancelled, viewModel.uiState.value)
    }

    @Test
    fun handleCameraResult_withResultOkAndNullPath_setsErrorState() = runTest {
        // Act: This is an error case (OK result but no image)
        viewModel.handleCameraResult(Activity.RESULT_OK, null)

        // Assert
        val state = viewModel.uiState.value
        assertTrue(state is SessionUiState.Error)
        assertEquals("Failed to capture image", state.message)
    }

    @Test
    fun handleCameraResult_withOtherResultCode_setsErrorState() = runTest {
        // Act
        viewModel.handleCameraResult(Activity.RESULT_FIRST_USER, "path/to/image.jpg")

        // Assert
        val state = viewModel.uiState.value
        assertTrue(state is SessionUiState.Error)
        assertEquals("Failed to capture image", state.message)
    }

    @Test
    fun resetState_fromSuccess_goesToIdle() = runTest {
        // Arrange: Set state to Success
        viewModel.handleCameraResult(Activity.RESULT_OK, "path/to/image.jpg")
        assertTrue(viewModel.uiState.value is SessionUiState.Success)

        // Act
        viewModel.resetState()

        // Assert
        assertEquals(SessionUiState.Idle, viewModel.uiState.value)
    }
}