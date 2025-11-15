package com.example.storybridge_android.ui.camera

import android.app.Application
import android.content.ContentResolver
import android.content.IntentSender
import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import java.io.ByteArrayInputStream
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CameraViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var mockApplication: Application

    @Mock
    private lateinit var mockContentResolver: ContentResolver

    @Mock
    private lateinit var mockScanningResult: GmsDocumentScanningResult

    @Mock
    private lateinit var mockUri: Uri

    @Mock
    private lateinit var mockActivity: android.app.Activity

    private lateinit var viewModel: CameraViewModel
    private lateinit var testFile: File

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        // Create a real temporary directory for tests
        testFile = File.createTempFile("test", "dir")
        testFile.delete()
        testFile.mkdirs()

        // Mock Application context
        whenever(mockApplication.applicationContext).thenReturn(mockApplication)
        whenever(mockApplication.filesDir).thenReturn(testFile)

        viewModel = CameraViewModel(mockApplication)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        // Clean up test files
        testFile.deleteRecursively()
    }

    @Test
    fun `initial state should be default`() {
        // Given & When
        val initialState = viewModel.uiState.value

        // Then
        assertFalse(initialState.isInstalling)
        assertFalse(initialState.isReady)
        assertFalse(initialState.isScanning)
        assertEquals(null, initialState.imagePath)
        assertEquals(null, initialState.error)
    }

    @Test
    fun `consumeReadyFlag sets isReady to false`() = runTest {
        // When
        viewModel.consumeReadyFlag()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isReady)
    }

    @Test
    fun `handleScanningResult with null result sets error`() = runTest {
        // When
        viewModel.handleScanningResult(null, mockContentResolver)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertNotNull(state.error)
        assertTrue(state.error!!.contains("null"))
    }

    @Test
    fun `handleScanningResult with no pages sets error`() = runTest {
        // Given
        whenever(mockScanningResult.pages).thenReturn(emptyList())

        // When
        viewModel.handleScanningResult(mockScanningResult, mockContentResolver)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertNotNull(state.error)
        assertTrue(state.error!!.contains("No page"))
    }

    @Test
    fun `handleScanningResult with valid page saves image successfully`() = runTest {
        // Given
        val mockPage = mock<GmsDocumentScanningResult.Page>()
        whenever(mockPage.imageUri).thenReturn(mockUri)
        whenever(mockScanningResult.pages).thenReturn(listOf(mockPage))

        // Create a real input stream with test data
        val testData = "test image data".toByteArray()
        val inputStream = ByteArrayInputStream(testData)
        whenever(mockContentResolver.openInputStream(mockUri)).thenReturn(inputStream)

        // When
        viewModel.handleScanningResult(mockScanningResult, mockContentResolver)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertNotNull(state.imagePath)
        assertTrue(state.imagePath!!.contains("scan_"))
        assertTrue(state.imagePath!!.endsWith(".jpg"))

        // Verify file was created
        val savedFile = File(state.imagePath!!)
        assertTrue(savedFile.exists())
    }

    @Test
    fun `handleScanningResult with invalid input stream sets error`() = runTest {
        // Given
        val mockPage = mock<GmsDocumentScanningResult.Page>()
        whenever(mockPage.imageUri).thenReturn(mockUri)
        whenever(mockScanningResult.pages).thenReturn(listOf(mockPage))
        whenever(mockContentResolver.openInputStream(mockUri)).thenReturn(null)

        // When
        viewModel.handleScanningResult(mockScanningResult, mockContentResolver)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertNotNull(state.error)
        assertTrue(state.error!!.contains("Cannot open input stream") || state.error!!.contains("Save failed"))
    }

    @Test
    fun `prepareScannerIntent calls onError when scanner is null`() = runTest {
        // Given
        var errorCalled = false
        var errorMessage = ""

        // When
        viewModel.prepareScannerIntent(
            activity = mockActivity,
            onReady = { },
            onError = { msg ->
                errorCalled = true
                errorMessage = msg
            }
        )
        advanceUntilIdle()

        // Then
        assertTrue(errorCalled)
        assertTrue(errorMessage.contains("not initialized"))
    }

    @Test
    fun `multiple consumeReadyFlag calls keep isReady false`() = runTest {
        // When
        repeat(5) {
            viewModel.consumeReadyFlag()
            advanceUntilIdle()
        }

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isReady)
    }

    @Test
    fun `error state is preserved until new operation`() = runTest {
        // Given - cause an error
        viewModel.handleScanningResult(null, mockContentResolver)
        advanceUntilIdle()

        // When - verify error is set
        val errorState = viewModel.uiState.value
        assertNotNull(errorState.error)
        val errorMessage = errorState.error

        // Then - error persists
        advanceUntilIdle()
        assertEquals(errorMessage, viewModel.uiState.value.error)
    }

    @Test
    fun `handleScanningResult with IOException during save sets error`() = runTest {
        // Given
        val mockPage = mock<GmsDocumentScanningResult.Page>()
        whenever(mockPage.imageUri).thenReturn(mockUri)
        whenever(mockScanningResult.pages).thenReturn(listOf(mockPage))
        whenever(mockContentResolver.openInputStream(mockUri)).thenThrow(RuntimeException("IO Error"))

        // When
        viewModel.handleScanningResult(mockScanningResult, mockContentResolver)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertNotNull(state.error)
        assertTrue(state.error!!.contains("Save failed") || state.error!!.contains("IO Error"))
    }

    @Test
    fun `saved image path contains timestamp`() = runTest {
        // Given
        val mockPage = mock<GmsDocumentScanningResult.Page>()
        whenever(mockPage.imageUri).thenReturn(mockUri)
        whenever(mockScanningResult.pages).thenReturn(listOf(mockPage))

        val testData = "test image data".toByteArray()
        val inputStream = ByteArrayInputStream(testData)
        whenever(mockContentResolver.openInputStream(mockUri)).thenReturn(inputStream)

        val beforeTime = System.currentTimeMillis()

        // When
        viewModel.handleScanningResult(mockScanningResult, mockContentResolver)
        advanceUntilIdle()

        val afterTime = System.currentTimeMillis()

        // Then
        val state = viewModel.uiState.value
        assertNotNull(state.imagePath)
        assertTrue(state.imagePath!!.contains("scan_"))

        // Extract timestamp from filename
        val filename = File(state.imagePath!!).name
        assertTrue(filename.matches(Regex("scan_\\d+\\.jpg")))
    }

    @Test
    fun `handleScanningResult with empty pages list returns error`() = runTest {
        // Given
        whenever(mockScanningResult.pages).thenReturn(null)

        // When
        viewModel.handleScanningResult(mockScanningResult, mockContentResolver)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertNotNull(state.error)
    }

    @Test
    fun `state updates are idempotent for same operation`() = runTest {
        // When
        viewModel.consumeReadyFlag()
        advanceUntilIdle()
        val state1 = viewModel.uiState.value

        viewModel.consumeReadyFlag()
        advanceUntilIdle()
        val state2 = viewModel.uiState.value

        // Then
        assertEquals(state1.isReady, state2.isReady)
        assertFalse(state1.isReady)
        assertFalse(state2.isReady)
    }

    @Test
    fun `prepareScannerIntent with uninitialized scanner calls onError`() = runTest {
        // Given
        var readyCalled = false
        var errorCalled = false

        // When - with uninitialized scanner (default state)
        viewModel.prepareScannerIntent(
            activity = mockActivity,
            onReady = { readyCalled = true },
            onError = { errorCalled = true }
        )
        advanceUntilIdle()

        // Then - should call onError since scanner is not initialized
        assertTrue(errorCalled)
        assertFalse(readyCalled)
    }

    @Test
    fun `multiple errors from same source maintain error state`() = runTest {
        // When - cause multiple errors
        viewModel.handleScanningResult(null, mockContentResolver)
        advanceUntilIdle()

        val firstError = viewModel.uiState.value.error
        assertNotNull(firstError)

        // Cause another error (error should still be present)
        whenever(mockScanningResult.pages).thenReturn(emptyList())
        viewModel.handleScanningResult(mockScanningResult, mockContentResolver)
        advanceUntilIdle()

        // Then - error state is updated
        val secondError = viewModel.uiState.value.error
        assertNotNull(secondError)
    }

    @Test
    fun `file path uses application filesDir`() = runTest {
        // Given
        val mockPage = mock<GmsDocumentScanningResult.Page>()
        whenever(mockPage.imageUri).thenReturn(mockUri)
        whenever(mockScanningResult.pages).thenReturn(listOf(mockPage))

        val testData = "test".toByteArray()
        val inputStream = ByteArrayInputStream(testData)
        whenever(mockContentResolver.openInputStream(mockUri)).thenReturn(inputStream)

        // When
        viewModel.handleScanningResult(mockScanningResult, mockContentResolver)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertNotNull(state.imagePath)
        assertTrue(state.imagePath!!.startsWith(testFile.absolutePath))
    }
}