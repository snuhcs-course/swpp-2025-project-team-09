package com.example.storybridge_android.ui.session

import android.util.Log
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.storybridge_android.data.SessionRepository
import com.example.storybridge_android.network.SelectVoiceResponse
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
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
import java.io.File
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class VoiceSelectViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var mockSessionRepository: SessionRepository

    private lateinit var viewModel: VoiceSelectViewModel
    private lateinit var testImageFile: File

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)

        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.v(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0

        Dispatchers.setMain(testDispatcher)

        viewModel = VoiceSelectViewModel(mockSessionRepository)

        // Create a test image file
        testImageFile = File.createTempFile("test_cover", ".jpg")
        testImageFile.writeBytes(ByteArray(100) { it.toByte() })
    }


    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic(Log::class)
        if (testImageFile.exists()) {
            testImageFile.delete()
        }
    }

    @Test
    fun `initial loading state is false`() {
        // Then
        assertFalse(viewModel.loading.value)
    }

    @Test
    fun `selectVoice with success emits success event`() = runTest {
        // Given
        val sessionId = "session_123"
        val voiceStyle = "MALE"
        val response = SelectVoiceResponse(
            session_id = sessionId,
            voice_style = voiceStyle
        )

        whenever(mockSessionRepository.selectVoice(sessionId, voiceStyle))
            .thenReturn(Result.success(response))

        val successEmissions = mutableListOf<Unit>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.success.collect { successEmissions.add(it) }
        }

        // When
        viewModel.selectVoice(sessionId, voiceStyle)
        advanceUntilIdle()

        // Then
        assertEquals(1, successEmissions.size)
        verify(mockSessionRepository, times(1)).selectVoice(sessionId, voiceStyle)
    }

    @Test
    fun `selectVoice with failure emits error event`() = runTest {
        // Given
        val sessionId = "session_123"
        val voiceStyle = "MALE"
        val errorMessage = "Voice selection failed"

        whenever(mockSessionRepository.selectVoice(sessionId, voiceStyle))
            .thenReturn(Result.failure(Exception(errorMessage)))

        val errorEmissions = mutableListOf<String>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.error.collect { errorEmissions.add(it) }
        }

        // When
        viewModel.selectVoice(sessionId, voiceStyle)
        advanceUntilIdle()

        // Then
        assertEquals(1, errorEmissions.size)
        assertEquals(errorMessage, errorEmissions[0])
        verify(mockSessionRepository, times(1)).selectVoice(sessionId, voiceStyle)
    }

    @Test
    fun `selectVoice sets loading to true then false`() = runTest {
        // Given
        val sessionId = "session_123"
        val voiceStyle = "FEMALE"
        val response = SelectVoiceResponse(sessionId, voiceStyle)

        whenever(mockSessionRepository.selectVoice(sessionId, voiceStyle))
            .thenReturn(Result.success(response))

        val loadingStates = mutableListOf<Boolean>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.loading.collect { loadingStates.add(it) }
        }

        // When
        viewModel.selectVoice(sessionId, voiceStyle)
        advanceUntilIdle()

        // Then
        assertTrue(loadingStates.contains(false)) // Initial
        assertTrue(loadingStates.contains(true))  // During operation
        assertEquals(false, loadingStates.last())  // Final state
    }

    @Test
    fun `selectVoice with MALE voice calls repository correctly`() = runTest {
        // Given
        val sessionId = "session_456"
        val voiceStyle = "MALE"
        val response = SelectVoiceResponse(sessionId, voiceStyle)

        whenever(mockSessionRepository.selectVoice(sessionId, voiceStyle))
            .thenReturn(Result.success(response))

        // When
        viewModel.selectVoice(sessionId, voiceStyle)
        advanceUntilIdle()

        // Then
        verify(mockSessionRepository).selectVoice(sessionId, voiceStyle)
    }

    @Test
    fun `selectVoice with FEMALE voice calls repository correctly`() = runTest {
        // Given
        val sessionId = "session_789"
        val voiceStyle = "FEMALE"
        val response = SelectVoiceResponse(sessionId, voiceStyle)

        whenever(mockSessionRepository.selectVoice(sessionId, voiceStyle))
            .thenReturn(Result.success(response))

        // When
        viewModel.selectVoice(sessionId, voiceStyle)
        advanceUntilIdle()

        // Then
        verify(mockSessionRepository).selectVoice(sessionId, voiceStyle)
    }

    @Test
    fun `selectVoice with null error message uses default`() = runTest {
        // Given
        val sessionId = "session_123"
        val voiceStyle = "MALE"
        val exceptionWithNullMessage = Exception(null as String?)

        whenever(mockSessionRepository.selectVoice(sessionId, voiceStyle))
            .thenReturn(Result.failure(exceptionWithNullMessage))

        val errorEmissions = mutableListOf<String>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.error.collect { errorEmissions.add(it) }
        }

        // When
        viewModel.selectVoice(sessionId, voiceStyle)
        advanceUntilIdle()

        // Then
        assertEquals(1, errorEmissions.size)
        assertEquals("Voice selection failed", errorEmissions[0])
    }

    @Test
    fun `multiple selectVoice calls work correctly`() = runTest {
        // Given
        val sessionId = "session_123"
        val voice1 = "MALE"
        val voice2 = "FEMALE"

        val response1 = SelectVoiceResponse(sessionId, voice1)
        val response2 = SelectVoiceResponse(sessionId, voice2)

        whenever(mockSessionRepository.selectVoice(sessionId, voice1))
            .thenReturn(Result.success(response1))
        whenever(mockSessionRepository.selectVoice(sessionId, voice2))
            .thenReturn(Result.success(response2))

        val successEmissions = mutableListOf<Unit>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.success.collect { successEmissions.add(it) }
        }

        // When
        viewModel.selectVoice(sessionId, voice1)
        advanceUntilIdle()

        viewModel.selectVoice(sessionId, voice2)
        advanceUntilIdle()

        // Then
        assertEquals(2, successEmissions.size)
        verify(mockSessionRepository).selectVoice(sessionId, voice1)
        verify(mockSessionRepository).selectVoice(sessionId, voice2)
    }

    @Test
    fun `selectVoice with empty session ID calls repository`() = runTest {
        // Given
        val sessionId = ""
        val voiceStyle = "MALE"
        val response = SelectVoiceResponse(sessionId, voiceStyle)

        whenever(mockSessionRepository.selectVoice(sessionId, voiceStyle))
            .thenReturn(Result.success(response))

        // When
        viewModel.selectVoice(sessionId, voiceStyle)
        advanceUntilIdle()

        // Then
        verify(mockSessionRepository).selectVoice(sessionId, voiceStyle)
    }

    @Test
    fun `selectVoice with empty voice style calls repository`() = runTest {
        // Given
        val sessionId = "session_123"
        val voiceStyle = ""
        val response = SelectVoiceResponse(sessionId, voiceStyle)

        whenever(mockSessionRepository.selectVoice(sessionId, voiceStyle))
            .thenReturn(Result.success(response))

        // When
        viewModel.selectVoice(sessionId, voiceStyle)
        advanceUntilIdle()

        // Then
        verify(mockSessionRepository).selectVoice(sessionId, voiceStyle)
    }

    @Test
    fun `uploadCoverInBackground with invalid path does not crash`() = runTest {
        // Given
        val invalidPath = "/non/existent/path.jpg"

        // When - should handle error gracefully
        viewModel.uploadCoverInBackground("session_123", "en", invalidPath)
        advanceUntilIdle()

        // Then - no exception thrown, operation completes
        assertFalse(viewModel.loading.value)
    }

    @Test
    fun `uploadCoverInBackground with valid path completes`() = runTest {
        // Given
        val sessionId = "session_123"
        val lang = "en"

        // When
        viewModel.uploadCoverInBackground(sessionId, lang, testImageFile.absolutePath)
        advanceUntilIdle()

        // Then - operation completes without error
        assertFalse(viewModel.loading.value)
    }

    @Test
    fun `success shared flow emits events correctly`() = runTest {
        // Given
        val sessionId = "session_123"
        val voiceStyle = "MALE"
        val response = SelectVoiceResponse(sessionId, voiceStyle)

        whenever(mockSessionRepository.selectVoice(sessionId, voiceStyle))
            .thenReturn(Result.success(response))

        val emissions = mutableListOf<Unit>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.success.collect { emissions.add(it) }
        }

        // When
        viewModel.selectVoice(sessionId, voiceStyle)
        advanceUntilIdle()

        viewModel.selectVoice(sessionId, voiceStyle)
        advanceUntilIdle()

        // Then
        assertEquals(2, emissions.size)
    }

    @Test
    fun `error shared flow emits events correctly`() = runTest {
        // Given
        val sessionId = "session_123"
        val voiceStyle = "MALE"
        val error1 = "Error 1"
        val error2 = "Error 2"

        whenever(mockSessionRepository.selectVoice(sessionId, voiceStyle))
            .thenReturn(Result.failure(Exception(error1)))
            .thenReturn(Result.failure(Exception(error2)))

        val emissions = mutableListOf<String>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.error.collect { emissions.add(it) }
        }

        // When
        viewModel.selectVoice(sessionId, voiceStyle)
        advanceUntilIdle()

        whenever(mockSessionRepository.selectVoice(sessionId, voiceStyle))
            .thenReturn(Result.failure(Exception(error2)))

        viewModel.selectVoice(sessionId, voiceStyle)
        advanceUntilIdle()

        // Then
        assertEquals(2, emissions.size)
        assertEquals(error1, emissions[0])
        assertEquals(error2, emissions[1])
    }

    @Test
    fun `loading state resets after success`() = runTest {
        // Given
        val sessionId = "session_123"
        val voiceStyle = "MALE"
        val response = SelectVoiceResponse(sessionId, voiceStyle)

        whenever(mockSessionRepository.selectVoice(sessionId, voiceStyle))
            .thenReturn(Result.success(response))

        // When
        viewModel.selectVoice(sessionId, voiceStyle)
        advanceUntilIdle()

        // Then
        assertFalse(viewModel.loading.value)
    }

    @Test
    fun `loading state resets after failure`() = runTest {
        // Given
        val sessionId = "session_123"
        val voiceStyle = "MALE"

        whenever(mockSessionRepository.selectVoice(sessionId, voiceStyle))
            .thenReturn(Result.failure(Exception("Error")))

        // When
        viewModel.selectVoice(sessionId, voiceStyle)
        advanceUntilIdle()

        // Then
        assertFalse(viewModel.loading.value)
    }

    @Test
    fun `selectVoice with special characters in session ID`() = runTest {
        // Given
        val sessionId = "session@#$%^&*()"
        val voiceStyle = "MALE"
        val response = SelectVoiceResponse(sessionId, voiceStyle)

        whenever(mockSessionRepository.selectVoice(sessionId, voiceStyle))
            .thenReturn(Result.success(response))

        // When
        viewModel.selectVoice(sessionId, voiceStyle)
        advanceUntilIdle()

        // Then
        verify(mockSessionRepository).selectVoice(sessionId, voiceStyle)
    }

    @Test
    fun `selectVoice repository is called exactly once`() = runTest {
        // Given
        val sessionId = "session_123"
        val voiceStyle = "MALE"
        val response = SelectVoiceResponse(sessionId, voiceStyle)

        whenever(mockSessionRepository.selectVoice(sessionId, voiceStyle))
            .thenReturn(Result.success(response))

        // When
        viewModel.selectVoice(sessionId, voiceStyle)
        advanceUntilIdle()

        // Then
        verify(mockSessionRepository, times(1)).selectVoice(sessionId, voiceStyle)
        verifyNoMoreInteractions(mockSessionRepository)
    }

    @Test
    fun `uploadCoverInBackground with different languages`() = runTest {
        // Given
        val sessionId = "session_123"
        val languages = listOf("en", "zh", "ko", "ja")

        // When
        languages.forEach { lang ->
            viewModel.uploadCoverInBackground(sessionId, lang, testImageFile.absolutePath)
            advanceUntilIdle()
        }

        // Then - all complete without error
        assertFalse(viewModel.loading.value)
    }

    @Test
    fun `uploadCoverInBackground does not affect loading state`() = runTest {
        // Given
        val sessionId = "session_123"
        val lang = "en"

        val initialLoading = viewModel.loading.value

        // When
        viewModel.uploadCoverInBackground(sessionId, lang, testImageFile.absolutePath)
        advanceUntilIdle()

        // Then - loading state unchanged (background operation)
        assertEquals(initialLoading, viewModel.loading.value)
    }

    @Test
    fun `consecutive success emissions work correctly`() = runTest {
        // Given
        val sessionId = "session_123"
        val voiceStyle = "MALE"
        val response = SelectVoiceResponse(sessionId, voiceStyle)

        whenever(mockSessionRepository.selectVoice(sessionId, voiceStyle))
            .thenReturn(Result.success(response))

        val emissions = mutableListOf<Unit>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.success.collect { emissions.add(it) }
        }

        // When
        repeat(3) {
            viewModel.selectVoice(sessionId, voiceStyle)
            advanceUntilIdle()
        }

        // Then
        assertEquals(3, emissions.size)
    }

    @Test
    fun `selectVoice exception handling emits error`() = runTest {
        // Given
        val sessionId = "session_123"
        val voiceStyle = "MALE"
        val errorMessage = "Network timeout"

        whenever(mockSessionRepository.selectVoice(sessionId, voiceStyle))
            .thenThrow(RuntimeException(errorMessage))

        val errorEmissions = mutableListOf<String>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.error.collect { errorEmissions.add(it) }
        }

        // When
        viewModel.selectVoice(sessionId, voiceStyle)
        advanceUntilIdle()

        // Then
        assertEquals(1, errorEmissions.size)
        assertTrue(errorEmissions[0].contains(errorMessage) || errorEmissions[0] == "Unexpected error")
    }
}