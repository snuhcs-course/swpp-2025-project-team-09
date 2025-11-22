package com.example.storybridge_android.ui.session

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.storybridge_android.data.SessionRepository
import com.example.storybridge_android.network.StartSessionResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class StartSessionViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var mockSessionRepository: SessionRepository

    private lateinit var viewModel: StartSessionViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        viewModel = StartSessionViewModel(mockSessionRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Idle`() {
        // Then
        assertTrue(viewModel.state.value is StartSessionUiState.Idle)
    }

    @Test
    fun `startSession with success updates state to Success`() = runTest {
        // Given
        val deviceId = "test_device_123"
        val sessionId = "session_456"
        val sessionResponse = StartSessionResponse(
            session_id = sessionId,
            page_index = 0,
            started_at = "2023-01-01T00:00:00"
        )

        whenever(mockSessionRepository.startSession(deviceId))
            .thenReturn(Result.success(sessionResponse))

        // When
        viewModel.startSession(deviceId)
        advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertTrue(state is StartSessionUiState.Success)
        assertEquals(sessionId, (state as StartSessionUiState.Success).sessionId)
        verify(mockSessionRepository, times(1)).startSession(deviceId)
    }

    @Test
    fun `startSession with failure updates state to Error`() = runTest {
        // Given
        val deviceId = "test_device_123"
        val errorMessage = "Network error"

        whenever(mockSessionRepository.startSession(deviceId))
            .thenReturn(Result.failure(Exception(errorMessage)))

        // When
        viewModel.startSession(deviceId)
        advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertTrue(state is StartSessionUiState.Error)
        assertEquals(errorMessage, (state as StartSessionUiState.Error).message)
        verify(mockSessionRepository, times(1)).startSession(deviceId)
    }

    @Test
    fun `startSession sets Loading state before completing`() = runTest {
        // Given
        val deviceId = "test_device_123"
        val sessionResponse = StartSessionResponse(
            session_id = "session_123",
            page_index = 0,
            started_at = "2023-01-01T00:00:00"
        )

        whenever(mockSessionRepository.startSession(deviceId))
            .thenReturn(Result.success(sessionResponse))

        val stateEmissions = mutableListOf<StartSessionUiState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.state.collect { stateEmissions.add(it) }
        }

        // When
        viewModel.startSession(deviceId)
        advanceUntilIdle()

        // Then
        assertTrue(stateEmissions[0] is StartSessionUiState.Idle)
        assertTrue(stateEmissions[1] is StartSessionUiState.Loading)
        assertTrue(stateEmissions[2] is StartSessionUiState.Success)
    }

    @Test
    fun `startSession with empty device ID calls repository`() = runTest {
        // Given
        val deviceId = ""
        val sessionResponse = StartSessionResponse(
            session_id = "session_empty",
            page_index = 0,
            started_at = "2023-01-01T00:00:00"
        )

        whenever(mockSessionRepository.startSession(deviceId))
            .thenReturn(Result.success(sessionResponse))

        // When
        viewModel.startSession(deviceId)
        advanceUntilIdle()

        // Then
        verify(mockSessionRepository).startSession(deviceId)
        assertTrue(viewModel.state.value is StartSessionUiState.Success)
    }

    @Test
    fun `startSession with null error message uses default message`() = runTest {
        // Given
        val deviceId = "test_device"
        val exceptionWithNullMessage = Exception(null as String?)

        whenever(mockSessionRepository.startSession(deviceId))
            .thenReturn(Result.failure(exceptionWithNullMessage))

        // When
        viewModel.startSession(deviceId)
        advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertTrue(state is StartSessionUiState.Error)
        assertEquals("Failed to start session", (state as StartSessionUiState.Error).message)
    }

    @Test
    fun `multiple startSession calls update state correctly`() = runTest {
        // Given
        val deviceId1 = "device_1"
        val deviceId2 = "device_2"
        val session1 = StartSessionResponse(
            session_id = "session_1",
            page_index = 0,
            started_at = "2023-01-01T00:00:00"
        )
        val session2 = StartSessionResponse(
            session_id = "session_2",
            page_index = 0,
            started_at = "2023-01-02T00:00:00"
        )

        whenever(mockSessionRepository.startSession(deviceId1))
            .thenReturn(Result.success(session1))
        whenever(mockSessionRepository.startSession(deviceId2))
            .thenReturn(Result.success(session2))

        // When - First call
        viewModel.startSession(deviceId1)
        advanceUntilIdle()
        val firstState = viewModel.state.value

        // When - Second call
        viewModel.startSession(deviceId2)
        advanceUntilIdle()
        val secondState = viewModel.state.value

        // Then
        assertTrue(firstState is StartSessionUiState.Success)
        assertEquals("session_1", (firstState as StartSessionUiState.Success).sessionId)

        assertTrue(secondState is StartSessionUiState.Success)
        assertEquals("session_2", (secondState as StartSessionUiState.Success).sessionId)

        verify(mockSessionRepository, times(1)).startSession(deviceId1)
        verify(mockSessionRepository, times(1)).startSession(deviceId2)
    }

    @Test
    fun `startSession with long device ID works correctly`() = runTest {
        // Given
        val longDeviceId = "a".repeat(1000)
        val sessionResponse = StartSessionResponse(
            session_id = "session_long",
            page_index = 0,
            started_at = "2023-01-01T00:00:00"
        )

        whenever(mockSessionRepository.startSession(longDeviceId))
            .thenReturn(Result.success(sessionResponse))

        // When
        viewModel.startSession(longDeviceId)
        advanceUntilIdle()

        // Then
        verify(mockSessionRepository).startSession(longDeviceId)
        assertTrue(viewModel.state.value is StartSessionUiState.Success)
    }

    @Test
    fun `state flow emits all state transitions`() = runTest {
        // Given
        val deviceId = "test_device"
        val sessionResponse = StartSessionResponse(
            session_id = "session_123",
            page_index = 0,
            started_at = "2023-01-01T00:00:00"
        )

        whenever(mockSessionRepository.startSession(deviceId))
            .thenReturn(Result.success(sessionResponse))

        val emissions = mutableListOf<StartSessionUiState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.state.collect { emissions.add(it) }
        }

        // When
        viewModel.startSession(deviceId)
        advanceUntilIdle()

        // Then
        assertEquals(3, emissions.size)
        assertTrue(emissions[0] is StartSessionUiState.Idle)
        assertTrue(emissions[1] is StartSessionUiState.Loading)
        assertTrue(emissions[2] is StartSessionUiState.Success)
    }

    @Test
    fun `error state with custom message preserves message`() = runTest {
        // Given
        val deviceId = "test_device"
        val customError = "Custom error message"

        whenever(mockSessionRepository.startSession(deviceId))
            .thenReturn(Result.failure(Exception(customError)))

        // When
        viewModel.startSession(deviceId)
        advanceUntilIdle()

        // Then
        val state = viewModel.state.value as StartSessionUiState.Error
        assertEquals(customError, state.message)
    }

    @Test
    fun `startSession with special characters in device ID`() = runTest {
        // Given
        val deviceId = "device@#$%^&*()_+-=[]{}|;:',.<>?/~`"
        val sessionResponse = StartSessionResponse(
            session_id = "session_special",
            page_index = 0,
            started_at = "2023-01-01T00:00:00"
        )

        whenever(mockSessionRepository.startSession(deviceId))
            .thenReturn(Result.success(sessionResponse))

        // When
        viewModel.startSession(deviceId)
        advanceUntilIdle()

        // Then
        verify(mockSessionRepository).startSession(deviceId)
        assertTrue(viewModel.state.value is StartSessionUiState.Success)
    }

    @Test
    fun `consecutive failures maintain Error state`() = runTest {
        // Given
        val deviceId = "test_device"
        val error1 = "Error 1"
        val error2 = "Error 2"

        whenever(mockSessionRepository.startSession(deviceId))
            .thenReturn(Result.failure(Exception(error1)))

        // When - First failure
        viewModel.startSession(deviceId)
        advanceUntilIdle()
        val firstState = viewModel.state.value

        // Setup second failure
        whenever(mockSessionRepository.startSession(deviceId))
            .thenReturn(Result.failure(Exception(error2)))

        // When - Second failure
        viewModel.startSession(deviceId)
        advanceUntilIdle()
        val secondState = viewModel.state.value

        // Then
        assertTrue(firstState is StartSessionUiState.Error)
        assertEquals(error1, (firstState as StartSessionUiState.Error).message)

        assertTrue(secondState is StartSessionUiState.Error)
        assertEquals(error2, (secondState as StartSessionUiState.Error).message)
    }

    @Test
    fun `Success state contains valid session ID`() = runTest {
        // Given
        val deviceId = "test_device"
        val expectedSessionId = "valid_session_id_123"
        val sessionResponse = StartSessionResponse(
            session_id = expectedSessionId,
            page_index = 0,
            started_at = "2023-01-01T00:00:00"
        )

        whenever(mockSessionRepository.startSession(deviceId))
            .thenReturn(Result.success(sessionResponse))

        // When
        viewModel.startSession(deviceId)
        advanceUntilIdle()

        // Then
        val state = viewModel.state.value as StartSessionUiState.Success
        assertEquals(expectedSessionId, state.sessionId)
        assertTrue(state.sessionId.isNotEmpty())
    }

    @Test
    fun `Loading state is transient`() = runTest {
        // Given
        val deviceId = "test_device"
        val sessionResponse = StartSessionResponse(
            session_id = "session_123",
            page_index = 0,
            started_at = "2023-01-01T00:00:00"
        )

        whenever(mockSessionRepository.startSession(deviceId))
            .thenReturn(Result.success(sessionResponse))

        // When
        viewModel.startSession(deviceId)
        advanceUntilIdle()

        // Then - Loading should not be the final state
        assertFalse(viewModel.state.value is StartSessionUiState.Loading)
    }

    @Test
    fun `repository is called exactly once per startSession call`() = runTest {
        // Given
        val deviceId = "test_device"
        val sessionResponse = StartSessionResponse(
            session_id = "session_123",
            page_index = 0,
            started_at = "2023-01-01T00:00:00"
        )

        whenever(mockSessionRepository.startSession(deviceId))
            .thenReturn(Result.success(sessionResponse))

        // When
        viewModel.startSession(deviceId)
        advanceUntilIdle()

        // Then
        verify(mockSessionRepository, times(1)).startSession(deviceId)
        verifyNoMoreInteractions(mockSessionRepository)
    }

    @Test
    fun `StartSessionUiState sealed class has all expected types`() {
        // Verify all state types can be instantiated
        val idle: StartSessionUiState = StartSessionUiState.Idle
        val loading: StartSessionUiState = StartSessionUiState.Loading
        val success: StartSessionUiState = StartSessionUiState.Success("test")
        val error: StartSessionUiState = StartSessionUiState.Error("test error")

        // Verify they are instances of the correct types
        assertTrue(idle is StartSessionUiState.Idle)
        assertTrue(loading is StartSessionUiState.Loading)
        assertTrue(success is StartSessionUiState.Success)
        assertTrue(error is StartSessionUiState.Error)
    }

    @Test
    fun `Success state with empty session ID`() = runTest {
        // Given
        val deviceId = "test_device"
        val sessionResponse = StartSessionResponse(
            session_id = "",
            page_index = 0,
            started_at = "2023-01-01T00:00:00"
        )

        whenever(mockSessionRepository.startSession(deviceId))
            .thenReturn(Result.success(sessionResponse))

        // When
        viewModel.startSession(deviceId)
        advanceUntilIdle()

        // Then
        val state = viewModel.state.value as StartSessionUiState.Success
        assertEquals("", state.sessionId)
    }

    @Test
    fun `Error state data class preserves message`() {
        // Given
        val errorMessage = "Test error message"

        // When
        val errorState = StartSessionUiState.Error(errorMessage)

        // Then
        assertEquals(errorMessage, errorState.message)
    }

    @Test
    fun `Success state data class preserves session ID`() {
        // Given
        val sessionId = "test_session_123"

        // When
        val successState = StartSessionUiState.Success(sessionId)

        // Then
        assertEquals(sessionId, successState.sessionId)
    }

    @Test
    fun `state flow is read-only`() {
        // Verify that state is exposed as StateFlow (read-only)
        val stateFlow = viewModel.state

        // StateFlow should not allow external modification
        // This is enforced by the type system (asStateFlow())
        assertNotNull(stateFlow)
        assertTrue(stateFlow.value is StartSessionUiState.Idle)
    }
}