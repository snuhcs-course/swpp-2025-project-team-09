package com.example.storybridge_android.view_model

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.example.storybridge_android.ui.session.ContentInstructionViewModel
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
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.never
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for ContentInstructionViewModel.
 * This ViewModel does not use a Repository, so we test its internal state and LiveData emission.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ContentInstructionViewModelTest {

    // Rule required for testing LiveData components on a non-Android thread
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private val fakeSessionId = "test_session_123"
    private lateinit var viewModel: ContentInstructionViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = ContentInstructionViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun onStartClicked_withValidSessionId_emitsTrue() = runTest {
        // Arrange
        val observer = mock<Observer<Boolean>>()
        viewModel.navigateToCamera.observeForever(observer)
        viewModel.setSessionId(fakeSessionId)

        // Act
        viewModel.onStartClicked()

        // Assert
        // Verify that LiveData emits the navigation event
        verify(observer).onChanged(true)

        // Clean up observer
        viewModel.navigateToCamera.removeObserver(observer)
    }

    @Test
    fun onStartClicked_withValidSessionId_updatesLiveDataValue() = runTest {
        // Arrange
        viewModel.setSessionId(fakeSessionId)

        // Act
        viewModel.onStartClicked()

        // Assert
        // Check the value directly after the function call
        assertTrue(viewModel.navigateToCamera.value ?: false)
    }

    @Test
    fun onStartClicked_withNullSessionId_emitsFalseAndDoesNotNavigate() = runTest {
        // Arrange
        // sessionId is null by default at initialization
        val observer = mock<Observer<Boolean>>()
        viewModel.navigateToCamera.observeForever(observer)

        // Act
        viewModel.onStartClicked()

        // Assert
        // Verify LiveData emits false, and thus doesn't trigger navigation
        verify(observer).onChanged(false)

        // Verify that 'true' was never emitted
        verify(observer, never()).onChanged(true)

        // Check the final value
        assertFalse(viewModel.navigateToCamera.value ?: true)

        // Clean up observer
        viewModel.navigateToCamera.removeObserver(observer)
    }

    @Test
    fun setSessionId_updatesInternalState() = runTest {
        // Arrange
        // Initial state of sessionId is null

        // Act
        viewModel.setSessionId(fakeSessionId)
        // Note: We cannot directly assert the private field sessionId,
        // but we rely on the successful 'onStartClicked' test to verify state is held.

        // Assert
        // Verify the indirect effect: clicking 'start' should now succeed.
        viewModel.onStartClicked()
        assertTrue(viewModel.navigateToCamera.value ?: false)
    }
}