package com.example.storybridge_android.ui.reading

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.storybridge_android.R
import com.example.storybridge_android.ServiceLocator
import com.example.storybridge_android.data.PageRepository
import com.example.storybridge_android.network.*
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.runner.RunWith
import android.util.Base64
import android.view.View

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class ReadingActivityMockkTest {

    private lateinit var scenario: ActivityScenario<ReadingActivity>
    private lateinit var mockPageRepository: PageRepository

    private val testImageBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8DwHwAFBQIAX8jx0gAAAABJRU5ErkJggg=="

    @Before
    fun setup() {
        androidx.test.platform.app.InstrumentationRegistry
            .getInstrumentation()
            .uiAutomation
            .executeShellCommand("settings put global window_animation_scale 0")
        androidx.test.platform.app.InstrumentationRegistry
            .getInstrumentation()
            .uiAutomation
            .executeShellCommand("settings put global transition_animation_scale 0")
        androidx.test.platform.app.InstrumentationRegistry
            .getInstrumentation()
            .uiAutomation
            .executeShellCommand("settings put global animator_duration_scale 0")


        // Fake Repository 생성
        mockPageRepository = object : PageRepository {
            override suspend fun getImage(sessionId: String, pageIndex: Int): Result<GetImageResponse> {
                return Result.success(
                    GetImageResponse(
                        session_id = sessionId,
                        page_index = pageIndex,
                        image_base64 = testImageBase64,
                        stored_at = "2025-11-15T12:00:00"
                    )
                )
            }

            override suspend fun getOcrResults(sessionId: String, pageIndex: Int): Result<GetOcrTranslationResponse> {
                return Result.success(
                    GetOcrTranslationResponse(
                        session_id = sessionId,
                        page_index = pageIndex,
                        ocr_results = emptyList(),
                        processed_at = "2025-11-15T12:00:00"
                    )
                )
            }

            override suspend fun getTtsResults(sessionId: String, pageIndex: Int): Result<GetTtsResponse> {
                return Result.success(
                    GetTtsResponse(
                        session_id = sessionId,
                        page_index = pageIndex,
                        audio_results = emptyList(),
                        generated_at = "2025-11-15T12:00:00"
                    )
                )
            }
        }

        ServiceLocator.pageRepository = mockPageRepository

        // Base64 static method mock
        mockkStatic(Base64::class)
        every { Base64.decode(any<String>(), any()) } returns byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
        )
    }

    @After
    fun teardown() {
        if (::scenario.isInitialized) {
            scenario.close()
        }
        ServiceLocator.reset()
        unmockkAll()
    }

    private fun createIntent(
        sessionId: String = "test_session",
        pageIndex: Int = 1,
        totalPages: Int = 5,
        isNewSession: Boolean = true
    ): Intent {
        return Intent(ApplicationProvider.getApplicationContext(), ReadingActivity::class.java).apply {
            putExtra("session_id", sessionId)
            putExtra("page_index", pageIndex)
            putExtra("total_pages", totalPages)
            putExtra("is_new_session", isNewSession)
        }
    }

    @Test
    fun onCreate_loadsPageSuccessfully() = runTest {
        scenario = ActivityScenario.launch(createIntent(totalPages = 2))
        Thread.sleep(1500)

        scenario.onActivity { activity ->
            val topUi = activity.findViewById<View>(R.id.topUi)
            Assert.assertTrue(topUi.translationY < 0f)
        }

        onView(withId(R.id.main)).perform(click())
        Thread.sleep(300)

        scenario.onActivity { activity ->
            val topUi = activity.findViewById<View>(R.id.topUi)
            Assert.assertEquals(0f, topUi.translationY, 5f)
        }

        onView(withId(R.id.pageImage)).check(matches(isDisplayed()))
    }

    @Test
    fun imageLoadError_handlesFailure() = runTest {
        // Fake Repository를 에러 반환하도록 재생성
        mockPageRepository = object : PageRepository {
            override suspend fun getImage(sessionId: String, pageIndex: Int): Result<GetImageResponse> {
                return Result.failure(Exception("Image fetch failed: 500"))
            }

            override suspend fun getOcrResults(sessionId: String, pageIndex: Int): Result<GetOcrTranslationResponse> {
                return Result.success(
                    GetOcrTranslationResponse(sessionId, pageIndex, emptyList(), "2025-11-15T12:00:00")
                )
            }

            override suspend fun getTtsResults(sessionId: String, pageIndex: Int): Result<GetTtsResponse> {
                return Result.success(
                    GetTtsResponse(sessionId, pageIndex, emptyList(), "2025-11-15T12:00:00")
                )
            }
        }
        ServiceLocator.pageRepository = mockPageRepository

        scenario = ActivityScenario.launch(createIntent(totalPages = 2))
        Thread.sleep(2000)

        onView(withId(R.id.pageImage)).check(matches(isDisplayed()))
    }

    @Test
    fun prevButton_loadsCorrectPage() = runTest {
        scenario = ActivityScenario.launch(createIntent(pageIndex = 2, totalPages = 2))
        Thread.sleep(1500)

        onView(withId(R.id.main)).perform(click())
        Thread.sleep(300)

        onView(withId(R.id.prevButton)).perform(click())
        Thread.sleep(2000)

        onView(withId(R.id.pageImage)).check(matches(isDisplayed()))
    }

    @Test
    fun nextButton_loadsCorrectPage() = runTest {
        scenario = ActivityScenario.launch(createIntent(pageIndex = 1, totalPages = 3))
        Thread.sleep(1500)

        onView(withId(R.id.main)).perform(click())
        Thread.sleep(300)

        onView(withId(R.id.nextButton)).perform(click())
        Thread.sleep(2000)

        onView(withId(R.id.pageImage)).check(matches(isDisplayed()))
    }

    @Test
    fun toggleUI_showsAndHidesTopBottomNav() = runTest {
        scenario = ActivityScenario.launch(createIntent(totalPages = 2))
        Thread.sleep(1500)

        scenario.onActivity { activity ->
            val topUi = activity.findViewById<android.view.View>(R.id.topUi)
            val bottomUi = activity.findViewById<android.view.View>(R.id.bottomUi)

            Assert.assertTrue("TopUi should be hidden initially", topUi.translationY < 0f)
            Assert.assertTrue("BottomUi should be hidden initially", bottomUi.translationY > 0f)
        }

        onView(withId(R.id.main)).perform(click())
        Thread.sleep(500)

        scenario.onActivity { activity ->
            val topUi = activity.findViewById<android.view.View>(R.id.topUi)
            val bottomUi = activity.findViewById<android.view.View>(R.id.bottomUi)

            Assert.assertEquals("TopUi should be visible", 0f, topUi.translationY, 10f)
            Assert.assertEquals("BottomUi should be visible", 0f, bottomUi.translationY, 10f)
        }
    }

    @Test
    fun menuButton_togglesOverlay() = runTest {
        scenario = ActivityScenario.launch(createIntent(totalPages = 2))
        Thread.sleep(1500)

        onView(withId(R.id.main)).perform(click())
        Thread.sleep(300)

        onView(withId(R.id.menuButton)).perform(click())
        Thread.sleep(500)

        onView(withId(R.id.leftPanel)).check(matches(isDisplayed()))
        onView(withId(R.id.dimBackground)).check(matches(isDisplayed()))
    }

    @Test
    fun displayOcrResults_showsBoundingBoxes() = runTest {
        val bbox = BBox(100, 100, 200, 100, 200, 200, 100, 200)
        val ocrBox = OcrBox(bbox, "Original", "Translated Text")

        mockPageRepository = object : PageRepository {
            override suspend fun getImage(sessionId: String, pageIndex: Int): Result<GetImageResponse> {
                return Result.success(
                    GetImageResponse(sessionId, pageIndex, testImageBase64, "2025-11-15T12:00:00")
                )
            }
            override suspend fun getOcrResults(sessionId: String, pageIndex: Int): Result<GetOcrTranslationResponse> {
                return Result.success(
                    GetOcrTranslationResponse(sessionId, pageIndex, listOf(ocrBox), "2025-11-15T12:00:00")
                )
            }
            override suspend fun getTtsResults(sessionId: String, pageIndex: Int): Result<GetTtsResponse> {
                return Result.success(
                    GetTtsResponse(sessionId, pageIndex, emptyList(), "2025-11-15T12:00:00")
                )
            }
        }
        ServiceLocator.pageRepository = mockPageRepository

        scenario = ActivityScenario.launch(createIntent(totalPages = 2))
        Thread.sleep(3000)

        var bboxFound = false
        try {
            scenario.onActivity { activity ->
                val mainLayout = activity.findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.main)
                var bboxCount = 0
                for (i in 0 until mainLayout.childCount) {
                    if (mainLayout.getChildAt(i).tag == "bbox") {
                        bboxCount++
                    }
                }
                bboxFound = bboxCount > 0
            }
        } catch (e: Exception) {
            // Activity가 종료된 경우 무시
        }

        Assert.assertTrue("Should have at least one bounding box", bboxFound)
    }

    @Test
    fun statusText_displaysCorrectPageInfo() = runTest {
        scenario = ActivityScenario.launch(createIntent(pageIndex = 2, totalPages = 3))
        Thread.sleep(1500)
        onView(withId(R.id.main)).perform(click())
        Thread.sleep(500)
        onView(withId(R.id.statusText)).check(matches(withText("page 2/2")))
    }

}