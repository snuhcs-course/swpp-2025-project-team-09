package com.example.storybridge_android.ui.setting

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.storybridge_android.data.UserRepository
import com.example.storybridge_android.network.UserLangRequest
import com.example.storybridge_android.network.UserLangResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import retrofit2.Response
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SettingViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var mockUserRepository: UserRepository

    private lateinit var viewModel: SettingViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        viewModel = SettingViewModel(mockUserRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial langResponse should be null`() {
        // Given & When
        val initialResponse = viewModel.langResponse.value

        // Then
        assertNull(initialResponse)
    }

    @Test
    fun `updateLanguage with successful response sets langResponse`() = runTest {
        // Given
        val request = UserLangRequest(
            device_info = "test_device_123",
            language_preference = "en"
        )
        val successResponse = UserLangResponse(
            user_id = "user_123",
            language_preference = "en",
            updated_at = "2023-09-01T12:00:00"
        )
        whenever(mockUserRepository.userLang(request))
            .thenReturn(Response.success(successResponse))

        // When
        viewModel.updateLanguage(request)
        advanceUntilIdle()

        // Then
        val response = viewModel.langResponse.value
        assertNotNull(response)
        assertTrue(response.isSuccessful)
        assertEquals("user_123", response.body()?.user_id)
        assertEquals("en", response.body()?.language_preference)
        verify(mockUserRepository, times(1)).userLang(request)
    }

    @Test
    fun `updateLanguage with Chinese language succeeds`() = runTest {
        // Given
        val request = UserLangRequest(
            device_info = "test_device_456",
            language_preference = "zh"
        )
        val successResponse = UserLangResponse(
            user_id = "user_456",
            language_preference = "zh",
            updated_at = "2023-09-02T18:30:00"
        )
        whenever(mockUserRepository.userLang(request))
            .thenReturn(Response.success(successResponse))

        // When
        viewModel.updateLanguage(request)
        advanceUntilIdle()

        // Then
        val response = viewModel.langResponse.value
        assertNotNull(response)
        assertTrue(response.isSuccessful)
        assertEquals("zh", response.body()?.language_preference)
        verify(mockUserRepository, times(1)).userLang(request)
    }

    @Test
    fun `updateLanguage with error response sets error state`() = runTest {
        // Given
        val request = UserLangRequest(
            device_info = "test_device_789",
            language_preference = "en"
        )
        val errorResponse = Response.error<UserLangResponse>(
            400,
            """{"error":"INVALID_REQUEST"}""".toResponseBody("application/json".toMediaType())
        )
        whenever(mockUserRepository.userLang(request))
            .thenReturn(errorResponse)

        // When
        viewModel.updateLanguage(request)
        advanceUntilIdle()

        // Then
        val response = viewModel.langResponse.value
        assertNotNull(response)
        assertTrue(!response.isSuccessful)
        assertEquals(400, response.code())
        verify(mockUserRepository, times(1)).userLang(request)
    }

    @Test
    fun `updateLanguage with 404 error response`() = runTest {
        // Given
        val request = UserLangRequest(
            device_info = "unknown_device",
            language_preference = "en"
        )
        val errorResponse = Response.error<UserLangResponse>(
            404,
            """{"error":"USER_NOT_FOUND"}""".toResponseBody("application/json".toMediaType())
        )
        whenever(mockUserRepository.userLang(request))
            .thenReturn(errorResponse)

        // When
        viewModel.updateLanguage(request)
        advanceUntilIdle()

        // Then
        val response = viewModel.langResponse.value
        assertNotNull(response)
        assertTrue(!response.isSuccessful)
        assertEquals(404, response.code())
    }

    @Test
    fun `updateLanguage with 500 server error response`() = runTest {
        // Given
        val request = UserLangRequest(
            device_info = "test_device",
            language_preference = "en"
        )
        val errorResponse = Response.error<UserLangResponse>(
            500,
            """{"error":"INTERNAL_SERVER_ERROR"}""".toResponseBody("application/json".toMediaType())
        )
        whenever(mockUserRepository.userLang(request))
            .thenReturn(errorResponse)

        // When
        viewModel.updateLanguage(request)
        advanceUntilIdle()

        // Then
        val response = viewModel.langResponse.value
        assertNotNull(response)
        assertTrue(!response.isSuccessful)
        assertEquals(500, response.code())
    }

    @Test
    fun `multiple updateLanguage calls update state correctly`() = runTest {
        // Given
        val request1 = UserLangRequest(
            device_info = "device_1",
            language_preference = "en"
        )
        val request2 = UserLangRequest(
            device_info = "device_1",
            language_preference = "zh"
        )
        val response1 = UserLangResponse(user_id = "user_1", language_preference = "en", updated_at = "2023-09-01T12:00:00")
        val response2 = UserLangResponse(user_id = "user_1", language_preference = "zh", updated_at = "2023-09-01T12:00:00")

        whenever(mockUserRepository.userLang(request1))
            .thenReturn(Response.success(response1))
        whenever(mockUserRepository.userLang(request2))
            .thenReturn(Response.success(response2))

        // When
        viewModel.updateLanguage(request1)
        advanceUntilIdle()
        val firstResponse = viewModel.langResponse.value

        viewModel.updateLanguage(request2)
        advanceUntilIdle()
        val secondResponse = viewModel.langResponse.value

        // Then
        assertNotNull(firstResponse)
        assertEquals("en", firstResponse.body()?.language_preference)

        assertNotNull(secondResponse)
        assertEquals("zh", secondResponse.body()?.language_preference)

        verify(mockUserRepository, times(1)).userLang(request1)
        verify(mockUserRepository, times(1)).userLang(request2)
    }

    @Test
    fun `updateLanguage preserves device info in request`() = runTest {
        // Given
        val deviceInfo = "unique_device_identifier_12345"
        val request = UserLangRequest(
            device_info = deviceInfo,
            language_preference = "en"
        )
        val successResponse = UserLangResponse(
            user_id = "user_test",
            language_preference = "en",
            updated_at = "2023-09-01T12:00:00"
        )
        whenever(mockUserRepository.userLang(request))
            .thenReturn(Response.success(successResponse))

        // When
        viewModel.updateLanguage(request)
        advanceUntilIdle()

        // Then
        verify(mockUserRepository).userLang(argThat {
            device_info == deviceInfo
        })
    }

    @Test
    fun `updateLanguage with empty device info still calls repository`() = runTest {
        // Given
        val request = UserLangRequest(
            device_info = "",
            language_preference = "en"
        )
        val successResponse = UserLangResponse(
            user_id = "user_empty",
            language_preference = "en",
            updated_at = "2023-09-01T12:00:00"
        )
        whenever(mockUserRepository.userLang(request))
            .thenReturn(Response.success(successResponse))

        // When
        viewModel.updateLanguage(request)
        advanceUntilIdle()

        // Then
        val response = viewModel.langResponse.value
        assertNotNull(response)
        assertTrue(response.isSuccessful)
        verify(mockUserRepository, times(1)).userLang(request)
    }

    @Test
    fun `langResponse state flow emits values correctly`() = runTest {
        // Given
        val request = UserLangRequest(
            device_info = "test_device",
            language_preference = "en"
        )
        val successResponse = UserLangResponse(
            user_id = "user_flow",
            language_preference = "en",
            updated_at = "2023-09-01T12:00:00"
        )
        whenever(mockUserRepository.userLang(request))
            .thenReturn(Response.success(successResponse))

        val emissions = mutableListOf<Response<UserLangResponse>?>()

        // Collect emissions
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.langResponse.collect { response ->
                emissions.add(response)
            }
        }

        // When
        viewModel.updateLanguage(request)
        advanceUntilIdle()

        // Then
        assertEquals(2, emissions.size) // null initially, then success response
        assertNull(emissions[0])
        assertNotNull(emissions[1])
        assertTrue(emissions[1]!!.isSuccessful)
    }

    @Test
    fun `updateLanguage with 201 created response is successful`() = runTest {
        // Given
        val request = UserLangRequest(
            device_info = "new_device",
            language_preference = "en"
        )
        val createdResponse = UserLangResponse(
            user_id = "new_user",
            language_preference = "en",
            updated_at = "2023-09-01T12:00:00"
        )
        val response = Response.success(201, createdResponse)
        whenever(mockUserRepository.userLang(request))
            .thenReturn(response)

        // When
        viewModel.updateLanguage(request)
        advanceUntilIdle()

        // Then
        val result = viewModel.langResponse.value
        assertNotNull(result)
        assertTrue(result.isSuccessful)
        assertEquals(201, result.code())
    }

    @Test
    fun `updateLanguage request verification with exact parameters`() = runTest {
        // Given
        val expectedDeviceInfo = "exact_device_123"
        val expectedLanguage = "zh"
        val request = UserLangRequest(
            device_info = expectedDeviceInfo,
            language_preference = expectedLanguage
        )
        val successResponse = UserLangResponse(
            user_id = "user_exact",
            language_preference = expectedLanguage,
            updated_at = "2023-09-01T12:00:00"
        )
        whenever(mockUserRepository.userLang(any()))
            .thenReturn(Response.success(successResponse))

        // When
        viewModel.updateLanguage(request)
        advanceUntilIdle()

        // Then
        verify(mockUserRepository).userLang(argThat {
            device_info == expectedDeviceInfo && language_preference == expectedLanguage
        })
    }

    @Test
    fun `consecutive updateLanguage calls with same request`() = runTest {
        // Given
        val request = UserLangRequest(
            device_info = "same_device",
            language_preference = "en"
        )
        val successResponse = UserLangResponse(
            user_id = "user_same",
            language_preference = "en",
            updated_at = "2023-09-01T12:00:00"
        )
        whenever(mockUserRepository.userLang(request))
            .thenReturn(Response.success(successResponse))

        // When
        viewModel.updateLanguage(request)
        advanceUntilIdle()

        viewModel.updateLanguage(request)
        advanceUntilIdle()

        viewModel.updateLanguage(request)
        advanceUntilIdle()

        // Then
        verify(mockUserRepository, times(3)).userLang(request)
        val response = viewModel.langResponse.value
        assertNotNull(response)
        assertTrue(response.isSuccessful)
    }

    @Test
    fun `updateLanguage with 401 unauthorized error`() = runTest {
        // Given
        val request = UserLangRequest(
            device_info = "unauthorized_device",
            language_preference = "en"
        )
        val errorResponse = Response.error<UserLangResponse>(
            401,
            """{"error":"UNAUTHORIZED"}""".toResponseBody("application/json".toMediaType())
        )
        whenever(mockUserRepository.userLang(request))
            .thenReturn(errorResponse)

        // When
        viewModel.updateLanguage(request)
        advanceUntilIdle()

        // Then
        val response = viewModel.langResponse.value
        assertNotNull(response)
        assertTrue(!response.isSuccessful)
        assertEquals(401, response.code())
    }

    @Test
    fun `repository is called with correct request object`() = runTest {
        // Given
        val capturedRequest = argumentCaptor<UserLangRequest>()
        val request = UserLangRequest(
            device_info = "capture_test_device",
            language_preference = "zh"
        )
        val successResponse = UserLangResponse(
            user_id = "user_capture",
            language_preference = "zh",
            updated_at = "2023-09-01T12:00:00"
        )
        whenever(mockUserRepository.userLang(any()))
            .thenReturn(Response.success(successResponse))

        // When
        viewModel.updateLanguage(request)
        advanceUntilIdle()

        // Then
        verify(mockUserRepository).userLang(capturedRequest.capture())
        assertEquals("capture_test_device", capturedRequest.firstValue.device_info)
        assertEquals("zh", capturedRequest.firstValue.language_preference)
    }
}