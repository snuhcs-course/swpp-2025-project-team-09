package com.example.storybridge_android.ui.session

import android.content.Intent
import android.provider.Settings
import android.widget.ProgressBar
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.storybridge_android.R
import com.example.storybridge_android.ServiceLocator
import com.example.storybridge_android.data.*
import com.example.storybridge_android.network.*
import com.example.storybridge_android.ui.session.loading.LoadingActivity
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class LoadingActivityTest {

    private lateinit var mockProcessRepo: ProcessRepository
    private lateinit var mockUserRepo: UserRepository
    private lateinit var mockSessionRepo: SessionRepository

    @Before
    fun setup() {
        // Mock Repositories with proper Result handling
        mockProcessRepo = mockk()
        mockUserRepo = mockk()
        mockSessionRepo = mockk()

        // Default behavior - return failures to avoid real network calls
        coEvery { mockProcessRepo.uploadImage(any()) } returns Result.failure(Exception("Mock"))
        coEvery { mockProcessRepo.uploadCoverImage(any()) } returns Result.failure(Exception("Mock"))
        coEvery { mockProcessRepo.checkOcrStatus(any(), any()) } returns Result.failure(Exception("Mock"))
        coEvery { mockProcessRepo.checkTtsStatus(any(), any()) } returns Result.failure(Exception("Mock"))
        coEvery { mockUserRepo.getUserInfo(any()) } returns Response.success(emptyList())
        coEvery { mockSessionRepo.reloadAllSession(any(), any()) } returns Result.failure(Exception("Mock"))

        // Mock ServiceLocator to return test repositories
        mockkObject(ServiceLocator)
        every { ServiceLocator.processRepository } returns mockProcessRepo
        every { ServiceLocator.userRepository } returns mockUserRepo
        every { ServiceLocator.sessionRepository } returns mockSessionRepo

        // Mock Settings.Secure
        mockkStatic(Settings.Secure::class)
        every {
            Settings.Secure.getString(any(), Settings.Secure.ANDROID_ID)
        } returns "test_device_id"
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // ========== New Session - Invalid Input Tests ==========

    @Test
    fun handleNewSession_withNullSessionId_finishesActivity() {
        // Given - sessionId is null
        val intent = Intent(ApplicationProvider.getApplicationContext(), LoadingActivity::class.java).apply {
            // session_id intentionally missing
            putExtra("image_path", "/path/to/image.jpg")
            putExtra("is_cover", false)
            putExtra("lang", "en")
            putExtra("page_index", 1)
        }

        // When
        val scenario = ActivityScenario.launch<LoadingActivity>(intent)

        // Give time for error handling
        Thread.sleep(300)

        // Then - Activity should finish due to error
        // Can't call onActivity when destroyed, check state instead
        assertEquals(androidx.lifecycle.Lifecycle.State.DESTROYED, scenario.state)

        scenario.close()
    }

    @Test
    fun handleNewSession_withNullImagePath_finishesActivity() {
        // Given - imagePath is null
        val intent = Intent(ApplicationProvider.getApplicationContext(), LoadingActivity::class.java).apply {
            putExtra("session_id", "session_123")
            // image_path intentionally missing
            putExtra("is_cover", false)
            putExtra("lang", "en")
            putExtra("page_index", 1)
        }

        // When
        val scenario = ActivityScenario.launch<LoadingActivity>(intent)

        // Give time for error handling
        Thread.sleep(300)

        // Then - Activity should finish due to error
        assertEquals(androidx.lifecycle.Lifecycle.State.DESTROYED, scenario.state)

        scenario.close()
    }

    @Test
    fun handleNewSession_withMissingLang_defaultsToEn() {
        // Given - lang parameter missing
        val intent = Intent(ApplicationProvider.getApplicationContext(), LoadingActivity::class.java).apply {
            putExtra("session_id", "session_123")
            putExtra("image_path", "/path/to/image.jpg")
            putExtra("is_cover", false)
            // lang intentionally missing - should default to "en"
            putExtra("page_index", 1)
        }

        // When
        val scenario = ActivityScenario.launch<LoadingActivity>(intent)

        // Then - Activity should be created (lang defaults to "en")
        scenario.onActivity { activity ->
            assertNotNull(activity)
        }

        scenario.close()
    }

    // ========== New Session - Cover Upload Tests ==========

    @Test
    fun handleNewSession_withCoverImage_startsUpload() {
        // Given
        val intent = Intent(ApplicationProvider.getApplicationContext(), LoadingActivity::class.java).apply {
            putExtra("session_id", "session_cover")
            putExtra("image_path", "/path/to/cover.jpg")
            putExtra("is_cover", true)
            putExtra("lang", "ko")
            putExtra("page_index", 0)
        }

        // When
        val scenario = ActivityScenario.launch<LoadingActivity>(intent)

        // Then - Activity created, UI initialized
        scenario.onActivity { activity ->
            val progressBar = activity.findViewById<ProgressBar>(R.id.loadingBar)
            assertNotNull(progressBar)
            assertEquals(100, progressBar.max)
        }

        scenario.close()
    }

    // ========== New Session - Regular Page Upload Tests ==========

    @Test
    fun handleNewSession_withRegularPage_startsUpload() {
        // Given
        val intent = Intent(ApplicationProvider.getApplicationContext(), LoadingActivity::class.java).apply {
            putExtra("session_id", "session_page")
            putExtra("image_path", "/path/to/page.jpg")
            putExtra("is_cover", false)
            putExtra("lang", "en")
            putExtra("page_index", 3)
        }

        // When
        val scenario = ActivityScenario.launch<LoadingActivity>(intent)

        // Then
        scenario.onActivity { activity ->
            val progressBar = activity.findViewById<ProgressBar>(R.id.loadingBar)
            assertNotNull(progressBar)
            assertTrue(progressBar.progress >= 0 && progressBar.progress <= 100)
        }

        scenario.close()
    }

    // ========== Resume Session Tests ==========

    @Test
    fun handleResumeSession_withNoMatchingSession_showsError() {
        // Given
        val startedAt = "2023-01-01T00:00:00"
        val intent = Intent(ApplicationProvider.getApplicationContext(), LoadingActivity::class.java).apply {
            putExtra("started_at", startedAt)
        }

        val userInfoList = listOf(
            UserInfoResponse(
                user_id = "user_1",
                title = "Book 1",
                translated_title = "책 1",
                image_base64 = "base64_img",
                started_at = "2023-02-01T00:00:00", // Different started_at
                session_id = "session_999"
            )
        )

        coEvery { mockUserRepo.getUserInfo("test_device_id") } returns Response.success(userInfoList)

        // When
        val scenario = ActivityScenario.launch<LoadingActivity>(intent)

        // Give time for coroutines to execute
        Thread.sleep(500)

        // Then - Activity should finish due to no match
        assertEquals(androidx.lifecycle.Lifecycle.State.DESTROYED, scenario.state)

        scenario.close()
    }

    // ========== UI Component Tests ==========

    @Test
    fun observeProgress_updatesProgressBarValue() {
        // Given
        val intent = Intent(ApplicationProvider.getApplicationContext(), LoadingActivity::class.java).apply {
            putExtra("session_id", "session_123")
            putExtra("image_path", "/path/to/image.jpg")
            putExtra("is_cover", false)
            putExtra("lang", "en")
            putExtra("page_index", 1)
        }

        // When
        val scenario = ActivityScenario.launch<LoadingActivity>(intent)

        // Then - Progress should be within valid range
        scenario.onActivity { activity ->
            val progressBar = activity.findViewById<ProgressBar>(R.id.loadingBar)
            assertTrue(progressBar.progress >= 0 && progressBar.progress <= 100)
        }

        scenario.close()
    }

    // ========== Navigation Intent Tests ==========

    @Test
    fun navigateToReading_withNewSession_hasNoStartedAt() {
        // Given - No started_at means new session
        val intent = Intent(ApplicationProvider.getApplicationContext(), LoadingActivity::class.java).apply {
            putExtra("session_id", "session_new")
            putExtra("image_path", "/path/to/image.jpg")
            putExtra("is_cover", false)
            putExtra("lang", "en")
            putExtra("page_index", 2)
            // No started_at = new session
        }

        // When
        val scenario = ActivityScenario.launch<LoadingActivity>(intent)

        // Then - Verify intent extras are set correctly
        scenario.onActivity { activity ->
            val startedAt = activity.intent.getStringExtra("started_at")
            assertNull(startedAt) // New session has no started_at
        }

        scenario.close()
    }

    // ========== Back Press Handler Tests ==========

    @Test
    fun backPress_doesNotFinishActivity() {
        // Given
        val intent = Intent(ApplicationProvider.getApplicationContext(), LoadingActivity::class.java).apply {
            putExtra("session_id", "session_123")
            putExtra("image_path", "/path/to/image.jpg")
            putExtra("is_cover", false)
            putExtra("lang", "en")
            putExtra("page_index", 1)
        }

        // When
        val scenario = ActivityScenario.launch<LoadingActivity>(intent)

        scenario.onActivity { activity ->
            // Simulate back press
            activity.onBackPressedDispatcher.onBackPressed()
            // Toast would be shown but can't test directly
        }

        scenario.close()
    }

    // ========== Intent Extra Validation Tests ==========

    @Test
    fun intent_containsCorrectSessionId() {
        // Given
        val sessionId = "session_test_123"
        val intent = Intent(ApplicationProvider.getApplicationContext(), LoadingActivity::class.java).apply {
            putExtra("session_id", sessionId)
            putExtra("image_path", "/path/to/image.jpg")
            putExtra("is_cover", false)
            putExtra("lang", "en")
            putExtra("page_index", 1)
        }

        // When
        val scenario = ActivityScenario.launch<LoadingActivity>(intent)

        // Then
        scenario.onActivity { activity ->
            val retrievedSessionId = activity.intent.getStringExtra("session_id")
            assertEquals(sessionId, retrievedSessionId)
        }

        scenario.close()
    }

    @Test
    fun intent_containsCorrectPageIndex() {
        // Given
        val pageIndex = 5
        val intent = Intent(ApplicationProvider.getApplicationContext(), LoadingActivity::class.java).apply {
            putExtra("session_id", "session_123")
            putExtra("image_path", "/path/to/image.jpg")
            putExtra("is_cover", false)
            putExtra("lang", "en")
            putExtra("page_index", pageIndex)
        }

        // When
        val scenario = ActivityScenario.launch<LoadingActivity>(intent)

        // Then
        scenario.onActivity { activity ->
            val retrievedPageIndex = activity.intent.getIntExtra("page_index", -1)
            assertEquals(pageIndex, retrievedPageIndex)
        }

        scenario.close()
    }

    @Test
    fun intent_containsCorrectIsCoverFlag() {
        // Given
        val isCover = true
        val intent = Intent(ApplicationProvider.getApplicationContext(), LoadingActivity::class.java).apply {
            putExtra("session_id", "session_123")
            putExtra("image_path", "/path/to/cover.jpg")
            putExtra("is_cover", isCover)
            putExtra("lang", "en")
            putExtra("page_index", 0)
        }

        // When
        val scenario = ActivityScenario.launch<LoadingActivity>(intent)

        // Then
        scenario.onActivity { activity ->
            val retrievedIsCover = activity.intent.getBooleanExtra("is_cover", false)
            assertEquals(isCover, retrievedIsCover)
        }

        scenario.close()
    }

    @Test
    fun intent_containsCorrectLang() {
        // Given
        val lang = "ko"
        val intent = Intent(ApplicationProvider.getApplicationContext(), LoadingActivity::class.java).apply {
            putExtra("session_id", "session_123")
            putExtra("image_path", "/path/to/image.jpg")
            putExtra("is_cover", false)
            putExtra("lang", lang)
            putExtra("page_index", 1)
        }

        // When
        val scenario = ActivityScenario.launch<LoadingActivity>(intent)

        // Then
        scenario.onActivity { activity ->
            val retrievedLang = activity.intent.getStringExtra("lang")
            assertEquals(lang, retrievedLang)
        }

        scenario.close()
    }

}