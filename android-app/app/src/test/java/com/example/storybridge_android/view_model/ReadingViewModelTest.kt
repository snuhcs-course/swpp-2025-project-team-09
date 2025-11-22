package com.example.storybridge_android.ui.reading

import android.util.Log
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.storybridge_android.data.PageRepository
import com.example.storybridge_android.network.GetImageResponse
import com.example.storybridge_android.network.GetOcrTranslationResponse
import com.example.storybridge_android.network.GetTtsResponse
import com.example.storybridge_android.network.AudioResult
import com.example.storybridge_android.network.BBox
import com.example.storybridge_android.network.OcrBox
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

@ExperimentalCoroutinesApi
class ReadingViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @MockK
    private lateinit var repository: PageRepository

    private lateinit var viewModel: ReadingViewModel
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var testScope: TestScope

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)

        // Mock Android Log class
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.v(any(), any()) } returns 0

        testDispatcher = UnconfinedTestDispatcher()
        testScope = TestScope(testDispatcher)
        Dispatchers.setMain(testDispatcher)
        viewModel = ReadingViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic(Log::class)
        clearAllMocks()
    }

    @Test
    fun `initial state should have default values`() = testScope.runTest {
        val state = viewModel.uiState.value

        assertNull(state.image)
        assertNull(state.ocr)
        assertNull(state.tts)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `fetchPage should set loading state and call all fetch methods`() = testScope.runTest {
        // Given
        val sessionId = "test-session"
        val pageIndex = 1
        val mockImage = GetImageResponse(
            session_id = sessionId,
            page_index = pageIndex,
            image_base64 = "image-base64-data",
            stored_at = "2024-01-01T12:00:00Z"
        )
        val mockOcr = GetOcrTranslationResponse(
            session_id = sessionId,
            page_index = pageIndex,
            ocr_results = listOf(
                OcrBox(
                    bbox = BBox(100, 100, 200, 100, 200, 150, 100, 150),
                    original_txt = "Original text",
                    translation_txt = "Translated text"
                )
            ),
            processed_at = "2024-01-01T12:00:00Z"
        )
        val mockTts = GetTtsResponse(
            session_id = sessionId,
            page_index = pageIndex,
            audio_results = listOf(
                AudioResult(
                    bbox_index = 0,
                    audio_base64_list = listOf("audio-base64-data-1", "audio-base64-data-2")
                )
            ),
            generated_at = "2024-01-01T12:00:00Z"
        )

        coEvery { repository.getImage(sessionId, pageIndex) } returns Result.success(mockImage)
        coEvery { repository.getOcrResults(sessionId, pageIndex) } returns Result.success(mockOcr)
        coEvery { repository.getTtsResults(sessionId, pageIndex) } returns Result.success(mockTts)

        // When
        viewModel.fetchPage(sessionId, pageIndex)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(mockImage, state.image)
        assertEquals(mockOcr, state.ocr)
        assertEquals(mockTts, state.tts)
        assertFalse(state.isLoading)
        assertNull(state.error)

        coVerify { repository.getImage(sessionId, pageIndex) }
        coVerify { repository.getOcrResults(sessionId, pageIndex) }
        coVerify(atLeast = 1) { repository.getTtsResults(sessionId, pageIndex) }
    }

    @Test
    fun `fetchPage should handle image fetch failure`() = testScope.runTest {
        // Given
        val sessionId = "test-session"
        val pageIndex = 1
        val errorMessage = "Failed to fetch image"

        coEvery { repository.getImage(sessionId, pageIndex) } returns Result.failure(Exception(errorMessage))
        coEvery { repository.getOcrResults(sessionId, pageIndex) } returns Result.success(
            GetOcrTranslationResponse(
                session_id = sessionId,
                page_index = pageIndex,
                ocr_results = emptyList(),
                processed_at = "2024-01-01T12:00:00Z"
            )
        )
        coEvery { repository.getTtsResults(sessionId, pageIndex) } returns Result.success(
            GetTtsResponse(
                session_id = sessionId,
                page_index = pageIndex,
                audio_results = emptyList(),
                generated_at = "2024-01-01T12:00:00Z"
            )
        )

        // When
        viewModel.fetchPage(sessionId, pageIndex)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertNull(state.image)
        assertEquals(errorMessage, state.error)
        assertFalse(state.isLoading)
    }

    @Test
    fun `fetchPage should handle OCR fetch failure`() = testScope.runTest {
        // Given
        val sessionId = "test-session"
        val pageIndex = 1
        val errorMessage = "Failed to fetch OCR"

        coEvery { repository.getImage(sessionId, pageIndex) } returns Result.success(
            GetImageResponse(
                session_id = sessionId,
                page_index = pageIndex,
                image_base64 = "image-data",
                stored_at = "2024-01-01T12:00:00Z"
            )
        )
        coEvery { repository.getOcrResults(sessionId, pageIndex) } returns Result.failure(
            Exception(errorMessage)
        )
        coEvery { repository.getTtsResults(sessionId, pageIndex) } returns Result.success(
            GetTtsResponse(
                session_id = sessionId,
                page_index = pageIndex,
                audio_results = emptyList(),
                generated_at = "2024-01-01T12:00:00Z"
            )
        )

        // When
        viewModel.fetchPage(sessionId, pageIndex)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertNull(state.ocr)
        assertEquals(errorMessage, state.error)
    }

    @Test
    fun `fetchPage should handle TTS fetch failure`() = testScope.runTest {
        // Given
        val sessionId = "test-session"
        val pageIndex = 1
        val errorMessage = "Failed to fetch TTS"

        coEvery { repository.getImage(sessionId, pageIndex) } returns Result.success(
            GetImageResponse(
                session_id = sessionId,
                page_index = pageIndex,
                image_base64 = "image-data",
                stored_at = "2024-01-01T12:00:00Z"
            )
        )
        coEvery { repository.getOcrResults(sessionId, pageIndex) } returns Result.success(
            GetOcrTranslationResponse(
                session_id = sessionId,
                page_index = pageIndex,
                ocr_results = emptyList(),
                processed_at = "2024-01-01T12:00:00Z"
            )
        )
        coEvery { repository.getTtsResults(sessionId, pageIndex) } returns Result.failure(
            Exception(errorMessage)
        )

        // When
        viewModel.fetchPage(sessionId, pageIndex)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertNull(state.tts)
        assertEquals(errorMessage, state.error)
    }

    @Test
    fun `TTS polling should update state periodically`() = testScope.runTest {
        // Given
        val sessionId = "test-session"
        val pageIndex = 1
        val initialTts = GetTtsResponse(
            session_id = sessionId,
            page_index = pageIndex,
            audio_results = listOf(
                AudioResult(
                    bbox_index = 0,
                    audio_base64_list = listOf("audio1")
                )
            ),
            generated_at = "2024-01-01T12:00:00Z"
        )
        val updatedTts = GetTtsResponse(
            session_id = sessionId,
            page_index = pageIndex,
            audio_results = listOf(
                AudioResult(
                    bbox_index = 0,
                    audio_base64_list = listOf("audio1")
                ),
                AudioResult(
                    bbox_index = 1,
                    audio_base64_list = listOf("audio2", "audio3")
                )
            ),
            generated_at = "2024-01-01T12:01:00Z"
        )

        coEvery { repository.getImage(sessionId, pageIndex) } returns Result.success(
            GetImageResponse(
                session_id = sessionId,
                page_index = pageIndex,
                image_base64 = "image-data",
                stored_at = "2024-01-01T12:00:00Z"
            )
        )
        coEvery { repository.getOcrResults(sessionId, pageIndex) } returns Result.success(
            GetOcrTranslationResponse(
                session_id = sessionId,
                page_index = pageIndex,
                ocr_results = emptyList(),
                processed_at = "2024-01-01T12:00:00Z"
            )
        )
        coEvery { repository.getTtsResults(sessionId, pageIndex) } returnsMany listOf(
            Result.success(initialTts),
            Result.success(updatedTts)
        )

        // When
        viewModel.fetchPage(sessionId, pageIndex)
        advanceTimeBy(2100) // Just past first polling interval

        // Then
        val state = viewModel.uiState.value
        assertEquals(2, state.tts?.audio_results?.size)
        coVerify(atLeast = 2) { repository.getTtsResults(sessionId, pageIndex) }
    }

    @Test
    fun `polling should stop after maximum attempts`() = testScope.runTest {
        // Given
        val sessionId = "test-session"
        val pageIndex = 1
        val mockTts = GetTtsResponse(
            session_id = sessionId,
            page_index = pageIndex,
            audio_results = emptyList(),
            generated_at = "2024-01-01T12:00:00Z"
        )

        coEvery { repository.getImage(sessionId, pageIndex) } returns Result.success(
            GetImageResponse(
                session_id = sessionId,
                page_index = pageIndex,
                image_base64 = "image-data",
                stored_at = "2024-01-01T12:00:00Z"
            )
        )
        coEvery { repository.getOcrResults(sessionId, pageIndex) } returns Result.success(
            GetOcrTranslationResponse(
                session_id = sessionId,
                page_index = pageIndex,
                ocr_results = emptyList(),
                processed_at = "2024-01-01T12:00:00Z"
            )
        )
        coEvery { repository.getTtsResults(sessionId, pageIndex) } returns Result.success(mockTts)

        // When
        viewModel.fetchPage(sessionId, pageIndex)
        advanceTimeBy(125000) // More than 60 * 2000ms = 120 seconds

        // Then
        // Verify that polling stops after 60 attempts (initial + 60 polls = 61 total)
        coVerify(atMost = 61) { repository.getTtsResults(sessionId, pageIndex) }
    }

    @Test
    fun `fetchThumbnail should add thumbnail to list on success`() = testScope.runTest {
        // Given
        val sessionId = "test-session"
        val pageIndex = 1
        val mockImage = GetImageResponse(
            session_id = sessionId,
            page_index = pageIndex,
            image_base64 = "thumbnail-base64",
            stored_at = "2024-01-01T12:00:00Z"
        )

        coEvery { repository.getImage(sessionId, pageIndex) } returns Result.success(mockImage)

        // When
        viewModel.fetchThumbnail(sessionId, pageIndex)
        advanceUntilIdle()

        // Then
        val thumbnails = viewModel.thumbnailList.value
        assertEquals(1, thumbnails.size)
        assertEquals(pageIndex, thumbnails[0].pageIndex)
        assertEquals("thumbnail-base64", thumbnails[0].imageBase64)
    }

    @Test
    fun `fetchThumbnail should add null thumbnail on failure`() = testScope.runTest {
        // Given
        val sessionId = "test-session"
        val pageIndex = 1

        coEvery { repository.getImage(sessionId, pageIndex) } returns Result.failure(
            Exception("Failed to fetch thumbnail")
        )

        // When
        viewModel.fetchThumbnail(sessionId, pageIndex)
        advanceUntilIdle()

        // Then
        val thumbnails = viewModel.thumbnailList.value
        assertEquals(1, thumbnails.size)
        assertEquals(pageIndex, thumbnails[0].pageIndex)
        assertNull(thumbnails[0].imageBase64)
    }

    @Test
    fun `fetchThumbnail multiple times should accumulate thumbnails`() = testScope.runTest {
        // Given
        val sessionId = "test-session"

        coEvery { repository.getImage(sessionId, 1) } returns Result.success(
            GetImageResponse(
                session_id = sessionId,
                page_index = 1,
                image_base64 = "thumbnail1",
                stored_at = "2024-01-01T12:00:00Z"
            )
        )
        coEvery { repository.getImage(sessionId, 2) } returns Result.success(
            GetImageResponse(
                session_id = sessionId,
                page_index = 2,
                image_base64 = "thumbnail2",
                stored_at = "2024-01-01T12:00:00Z"
            )
        )
        coEvery { repository.getImage(sessionId, 3) } returns Result.failure(
            Exception("Failed")
        )

        // When
        viewModel.fetchThumbnail(sessionId, 1)
        viewModel.fetchThumbnail(sessionId, 2)
        viewModel.fetchThumbnail(sessionId, 3)
        advanceUntilIdle()

        // Then
        val thumbnails = viewModel.thumbnailList.value
        assertEquals(3, thumbnails.size)
        assertEquals("thumbnail1", thumbnails[0].imageBase64)
        assertEquals("thumbnail2", thumbnails[1].imageBase64)
        assertNull(thumbnails[2].imageBase64)
    }

    @Test
    fun `onCleared should cancel polling job`() = testScope.runTest {
        // Given
        val sessionId = "test-session"
        val pageIndex = 1

        coEvery { repository.getImage(sessionId, pageIndex) } returns Result.success(
            GetImageResponse(
                session_id = sessionId,
                page_index = pageIndex,
                image_base64 = "image-data",
                stored_at = "2024-01-01T12:00:00Z"
            )
        )
        coEvery { repository.getOcrResults(sessionId, pageIndex) } returns Result.success(
            GetOcrTranslationResponse(
                session_id = sessionId,
                page_index = pageIndex,
                ocr_results = emptyList(),
                processed_at = "2024-01-01T12:00:00Z"
            )
        )
        coEvery { repository.getTtsResults(sessionId, pageIndex) } returns Result.success(
            GetTtsResponse(
                session_id = sessionId,
                page_index = pageIndex,
                audio_results = emptyList(),
                generated_at = "2024-01-01T12:00:00Z"
            )
        )

        // When
        viewModel.fetchPage(sessionId, pageIndex)
        advanceTimeBy(1000)

        // Clear the viewModel (simulating lifecycle end)
        viewModel.onCleared()
        advanceTimeBy(5000) // Advance time to see if polling continues

        // Then
        // Verify that no additional polls happen after onCleared
        coVerify(atMost = 2) { repository.getTtsResults(sessionId, pageIndex) }
    }

    @Test
    fun `new fetchPage call should cancel previous polling`() = testScope.runTest {
        // Given
        val sessionId = "test-session"

        coEvery { repository.getImage(any(), any()) } returns Result.success(
            GetImageResponse(
                session_id = sessionId,
                page_index = 0,
                image_base64 = "image-data",
                stored_at = "2024-01-01T12:00:00Z"
            )
        )
        coEvery { repository.getOcrResults(any(), any()) } returns Result.success(
            GetOcrTranslationResponse(
                session_id = sessionId,
                page_index = 0,
                ocr_results = emptyList(),
                processed_at = "2024-01-01T12:00:00Z"
            )
        )
        coEvery { repository.getTtsResults(any(), any()) } returns Result.success(
            GetTtsResponse(
                session_id = sessionId,
                page_index = 0,
                audio_results = emptyList(),
                generated_at = "2024-01-01T12:00:00Z"
            )
        )

        // When
        viewModel.fetchPage(sessionId, 1)
        advanceTimeBy(1000)
        viewModel.fetchPage(sessionId, 2) // New page fetch should cancel previous polling
        advanceTimeBy(5000)

        // Then
        // Verify that polling for page 1 stopped and page 2 is being polled
        coVerify(atMost = 2) { repository.getTtsResults(sessionId, 1) }
        coVerify(atLeast = 2) { repository.getTtsResults(sessionId, 2) }
    }

    @Test
    fun `verify BBox computed properties work correctly`() {
        // Given a BBox with coordinates forming a rectangle
        val bbox = BBox(
            x1 = 100, y1 = 100,  // top-left
            x2 = 200, y2 = 100,  // top-right
            x3 = 200, y3 = 150,  // bottom-right
            x4 = 100, y4 = 150   // bottom-left
        )

        // Then
        assertEquals(100, bbox.x)      // minimum x coordinate
        assertEquals(100, bbox.y)      // minimum y coordinate
        assertEquals(100, bbox.width)  // max x - min x
        assertEquals(50, bbox.height)  // max y - min y
    }

    @Test
    fun `verify AudioResult can have multiple audio files`() {
        // Given
        val audioResult = AudioResult(
            bbox_index = 0,
            audio_base64_list = listOf("audio1", "audio2", "audio3")
        )

        // Then
        assertEquals(0, audioResult.bbox_index)
        assertEquals(3, audioResult.audio_base64_list.size)
        assertEquals("audio1", audioResult.audio_base64_list[0])
        assertEquals("audio2", audioResult.audio_base64_list[1])
        assertEquals("audio3", audioResult.audio_base64_list[2])
    }
}