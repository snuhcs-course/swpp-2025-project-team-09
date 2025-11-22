package com.example.storybridge_android.view_model

import android.app.Activity
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.storybridge_android.ui.camera.CameraSessionViewModel
import com.example.storybridge_android.ui.camera.SessionUiState
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

    // Rule to execute LiveData updates synchronously
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    // Test dispatcher for coroutines
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: CameraSessionViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = CameraSessionViewModel()
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