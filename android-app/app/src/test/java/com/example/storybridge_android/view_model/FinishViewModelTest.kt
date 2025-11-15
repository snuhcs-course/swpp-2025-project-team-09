package com.example.storybridge_android.view_model

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.storybridge_android.data.SessionRepository
import com.example.storybridge_android.network.EndSessionResponse
import com.example.storybridge_android.network.SessionStatsResponse
import com.example.storybridge_android.ui.session.FinishViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.Result
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse // Import for better clarity

/**
 * Unit tests for FinishViewModel, verifying network call sequencing, timing, and LiveData updates.
 *
 * This version mocks the repository methods to return Result<EndSessionResponse> and Result<SessionStatsResponse>
 * as defined by the SessionRepository contract.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FinishViewModelTest {

    // Executes tasks synchronously on the same thread, preventing issues with LiveData.
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: FinishViewModel

    // Mock the dependency
    @Mock
    private lateinit var mockSessionRepository: SessionRepository

    private val fakeSessionId = "test_session_42"
    private val mockEndSessionResponse = EndSessionResponse(
        session_id = fakeSessionId,
        ended_at = "2023-11-15T01:00:00",
        total_pages = 7
    )
    private val mockStats = SessionStatsResponse(
        session_id = fakeSessionId, // Added fields for completeness, although only stats are used
        user_id = "test_user",
        isOngoing = false,
        started_at = "2023-11-15T00:00:00",
        ended_at = "2023-11-15T01:00:00",
        total_pages = 7,
        total_words_read = 500,
        total_time_spent = 185
    )

    @Before
    fun setup() {
        // Initialize mocks
        MockitoAnnotations.openMocks(this)
        // Set the Main dispatcher to the test dispatcher
        Dispatchers.setMain(testDispatcher)
        // Inject the mocked repository into the ViewModel
        viewModel = FinishViewModel(mockSessionRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun endSession_withEmptyId_doesNothing() = runTest {
        // Arrange is implicit

        // Act
        viewModel.endSession("")
        advanceTimeBy(4000) // Advance time past the expected 3.5s execution

        // Assert
        // Verify no network calls were made
        verify(mockSessionRepository, times(0)).endSession(any())
        verify(mockSessionRepository, times(0)).getSessionStats(any())

        // Verify no LiveData updates occurred and state remains initial (false)
        assertNull(viewModel.sessionStats.value)
        assertFalse(viewModel.showMainButton.value!!) // Use assertFalse for clarity
    }

    @Test
    fun endSession_successFlow_updatesStatsAndShowsButtonAfterDelay() = runTest {
        // Arrange: Mock success for both API calls using the new return types
        whenever(mockSessionRepository.endSession(eq(fakeSessionId)))
            .thenReturn(Result.success(mockEndSessionResponse))
        whenever(mockSessionRepository.getSessionStats(eq(fakeSessionId)))
            .thenReturn(Result.success(mockStats))

        // Act
        viewModel.endSession(fakeSessionId)

        // Assert 1: After endSession and 500ms delay, stats should be fetched
        advanceTimeBy(501)
        verify(mockSessionRepository, times(1)).endSession(fakeSessionId)
        verify(mockSessionRepository, times(1)).getSessionStats(fakeSessionId)

        // Check stats immediately posted (InstantTaskExecutorRule allows this)
        assertEquals(500, viewModel.sessionStats.value?.total_words_read)
        assertEquals(185, viewModel.sessionStats.value?.total_time_spent)
        assertFalse(viewModel.showMainButton.value!!) // Button not shown yet

        // Assert 2: Advance time past the final 3000ms delay
        advanceTimeBy(3000)

        // Check final state
        assertTrue(viewModel.showMainButton.value!!)
    }

    @Test
    fun endSession_whenEndSessionFails_skipsStatsButStillShowsButton() = runTest {
        // Arrange: Mock endSession failure
        whenever(mockSessionRepository.endSession(eq(fakeSessionId)))
            .thenReturn(Result.failure(Exception("End Session Failed")))
        // Mock stats to ensure they are NOT called
        whenever(mockSessionRepository.getSessionStats(any()))
            .thenReturn(Result.success(mockStats))

        // Act
        viewModel.endSession(fakeSessionId)

        // Assert 1: Only endSession is called
        advanceTimeBy(1)
        verify(mockSessionRepository, times(1)).endSession(fakeSessionId)
        verify(mockSessionRepository, times(0)).getSessionStats(any())

        // Stats should remain null
        assertNull(viewModel.sessionStats.value)

        // Assert 2: Advance time past the final 3000ms delay
        advanceTimeBy(3500) // 3000ms final delay + buffer

        // Check final state: button should still show
        assertTrue(viewModel.showMainButton.value!!)
    }

    @Test
    fun endSession_whenGetStatsFails_statsAreNullButButtonShows() = runTest {
        // Arrange: Mock endSession success, but getSessionStats failure
        whenever(mockSessionRepository.endSession(eq(fakeSessionId)))
            .thenReturn(Result.success(mockEndSessionResponse)) // Success for endSession
        whenever(mockSessionRepository.getSessionStats(eq(fakeSessionId)))
            .thenReturn(Result.failure(Exception("Get Stats Failed")))

        // Act
        viewModel.endSession(fakeSessionId)

        // Assert 1: Both calls happen
        advanceTimeBy(501)
        verify(mockSessionRepository, times(1)).endSession(fakeSessionId)
        verify(mockSessionRepository, times(1)).getSessionStats(fakeSessionId)

        // Stats should be null because the onFailure block does not update LiveData
        assertNull(viewModel.sessionStats.value)
        assertFalse(viewModel.showMainButton.value!!)

        // Assert 2: Advance time past the final 3000ms delay
        advanceTimeBy(3000)

        // Check final state
        assertTrue(viewModel.showMainButton.value!!)
    }
}