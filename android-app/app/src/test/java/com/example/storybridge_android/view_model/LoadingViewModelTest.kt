package com.example.storybridge_android.ui.session

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.storybridge_android.data.*
import com.example.storybridge_android.network.*
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
import retrofit2.Response
import java.io.File
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class LoadingViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var mockProcessRepo: ProcessRepository

    @Mock
    private lateinit var mockPageRepo: PageRepository

    @Mock
    private lateinit var mockUserRepo: UserRepository

    @Mock
    private lateinit var mockSessionRepo: SessionRepository

    @Mock
    private lateinit var mockContext: Context

    private lateinit var viewModel: LoadingViewModel
    private lateinit var testImageFile: File

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        // Create a temporary test image file
        testImageFile = File.createTempFile("test_image", ".jpg")
        testImageFile.writeBytes(createTestImageBytes())

        viewModel = LoadingViewModel(
            mockProcessRepo,
            mockPageRepo,
            mockUserRepo,
            mockSessionRepo
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        testImageFile.delete()
    }

    private fun createTestImageBytes(): ByteArray {
        // Create a minimal valid JPEG header
        return byteArrayOf(
            0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(),
            0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01,
            0xFF.toByte(), 0xD9.toByte() // End of image
        )
    }

    @Test
    fun `initial state values are correct`() {
        // Then
        assertEquals(0, viewModel.progress.value)
        assertEquals("idle", viewModel.status.value)
        assertNull(viewModel.error.value)
        assertNull(viewModel.cover.value)
        assertNull(viewModel.navigateToVoice.value)
        assertNull(viewModel.navigateToReading.value)
        assertNull(viewModel.userInfo.value)
    }

    @Test
    fun `uploadImage with invalid path sets error`() = runTest {
        // Given
        val invalidPath = "/non/existent/path.jpg"

        // When
        viewModel.uploadImage("session_123", 1, "en", invalidPath)
        advanceUntilIdle()

        // Then
        assertEquals("Failed to process image", viewModel.error.value)
    }

    @Test
    fun `uploadCover with invalid path sets error`() = runTest {
        // Given
        val invalidPath = "/non/existent/path.jpg"

        // When
        viewModel.uploadCover("session_123", "en", invalidPath)
        advanceUntilIdle()

        // Then
        assertEquals("Failed to process image", viewModel.error.value)
    }

    @Test
    fun `loadUserInfo calls repository and updates state`() = runTest {
        // Given
        val deviceInfo = "test_device_123"
        val userInfoList = listOf(
            UserInfoResponse(
                user_id = "user_1",
                title = "Book 1",
                translated_title = "Translated Book 1",
                image_base64 = "base64_image",
                started_at = "2023-01-01T00:00:00",
                session_id = "session_1"
            )
        )
        val response = Response.success(userInfoList)
        whenever(mockUserRepo.getUserInfo(deviceInfo)).thenReturn(response)

        // When
        viewModel.loadUserInfo(deviceInfo)
        advanceUntilIdle()

        // Then
        val result = viewModel.userInfo.value
        assertNotNull(result)
        assertTrue(result.isSuccessful)
        assertEquals(1, result.body()?.size)
        verify(mockUserRepo).getUserInfo(deviceInfo)
    }

    @Test
    fun `multiple loadUserInfo calls update state`() = runTest {
        // Given
        val deviceInfo1 = "device_1"
        val deviceInfo2 = "device_2"
        val response1 = Response.success(listOf(UserInfoResponse("user_1", "Book 1", "", "", "", "2023-01-01")))
        val response2 = Response.success(listOf(UserInfoResponse("user_2", "Book 2", "", "", "", "2023-01-02")))

        whenever(mockUserRepo.getUserInfo(deviceInfo1)).thenReturn(response1)
        whenever(mockUserRepo.getUserInfo(deviceInfo2)).thenReturn(response2)

        // When
        viewModel.loadUserInfo(deviceInfo1)
        advanceUntilIdle()
        val firstResult = viewModel.userInfo.value

        viewModel.loadUserInfo(deviceInfo2)
        advanceUntilIdle()
        val secondResult = viewModel.userInfo.value

        // Then
        assertEquals("user_1", firstResult?.body()?.first()?.user_id)
        assertEquals("user_2", secondResult?.body()?.first()?.user_id)
    }

    @Test
    fun `initial progress is zero`() {
        assertEquals(0, viewModel.progress.value)
    }

    @Test
    fun `initial status is idle`() {
        assertEquals("idle", viewModel.status.value)
    }

    @Test
    fun `initial error is null`() {
        assertNull(viewModel.error.value)
    }

    @Test
    fun `initial cover is null`() {
        assertNull(viewModel.cover.value)
    }

    @Test
    fun `initial navigateToVoice is null`() {
        assertNull(viewModel.navigateToVoice.value)
    }

    @Test
    fun `initial navigateToReading is null`() {
        assertNull(viewModel.navigateToReading.value)
    }

    @Test
    fun `initial userInfo is null`() {
        assertNull(viewModel.userInfo.value)
    }

    @Test
    fun `CoverResult data class works correctly`() {
        val result = CoverResult("Test Title", "male.mp3", "female.mp3")
        assertEquals("Test Title", result.title)
        assertEquals("male.mp3", result.maleTts)
        assertEquals("female.mp3", result.femaleTts)
    }

    @Test
    fun `CoverResult with null TTS values`() {
        val result = CoverResult("Test Title", null, null)
        assertEquals("Test Title", result.title)
        assertNull(result.maleTts)
        assertNull(result.femaleTts)
    }

    @Test
    fun `SessionResumeResult data class works correctly`() {
        val result = SessionResumeResult("session_123", 5, 10)
        assertEquals("session_123", result.session_id)
        assertEquals(5, result.page_index)
        assertEquals(10, result.total_pages)
    }

    @Test
    fun `SessionResumeResult with default total_pages`() {
        val result = SessionResumeResult("session_123", 3)
        assertEquals("session_123", result.session_id)
        assertEquals(3, result.page_index)
        assertEquals(4, result.total_pages) // page_index + 1
    }

    @Test
    fun `loadUserInfo with empty response`() = runTest {
        // Given
        val deviceInfo = "test_device"
        val response = Response.success(emptyList<UserInfoResponse>())
        whenever(mockUserRepo.getUserInfo(deviceInfo)).thenReturn(response)

        // When
        viewModel.loadUserInfo(deviceInfo)
        advanceUntilIdle()

        // Then
        val result = viewModel.userInfo.value
        assertNotNull(result)
        assertTrue(result.isSuccessful)
        assertEquals(0, result.body()?.size)
    }

    @Test
    fun `error state persists across operations`() = runTest {
        // Given
        val invalidPath = "/invalid/path.jpg"

        // When
        viewModel.uploadImage("session_1", 1, "en", invalidPath)
        advanceUntilIdle()
        val firstError = viewModel.error.value

        // Then
        assertEquals("Failed to process image", firstError)

        // When - another invalid operation
        viewModel.uploadCover("session_2", "en", invalidPath)
        advanceUntilIdle()

        // Then - error persists (same error message)
        assertEquals("Failed to process image", viewModel.error.value)
    }

    @Test
    fun `progress value stays within valid range`() = runTest {
        // Initial progress should be 0-100
        assertTrue(viewModel.progress.value in 0..100)
    }

    @Test
    fun `status values are string based`() {
        // Verify status is string type and starts as "idle"
        val status: String = viewModel.status.value
        assertEquals("idle", status)
    }
}