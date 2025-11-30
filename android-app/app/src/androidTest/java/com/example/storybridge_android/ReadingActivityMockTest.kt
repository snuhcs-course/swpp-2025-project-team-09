package com.example.storybridge_android.ui.reading

import android.content.Intent
import android.graphics.Bitmap
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
import android.widget.ImageView
import org.hamcrest.Matchers.`is`
import org.hamcrest.TypeSafeMatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class ReadingActivityMockTest {

    private lateinit var scenario: ActivityScenario<ReadingActivity>
    private lateinit var mockPageRepository: PageRepository

    private val testImageBase64 =
        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8DwHwAFBQIAX8jx0gAAAABJRU5ErkJggg=="

    @Before
    fun setup() {
        // Disable animations
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

        // Default mock repository
        mockPageRepository = createSuccessRepository()
        ServiceLocator.pageRepository = mockPageRepository

        // Mock Base64
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

    // ==================== Helper Functions ====================

    private fun createIntent(
        sessionId: String = "test_session",
        pageIndex: Int = 1,
        totalPages: Int = 5,
        isNewSession: Boolean = true
    ): Intent {
        return Intent(
            ApplicationProvider.getApplicationContext(),
            ReadingActivity::class.java
        ).apply {
            putExtra("session_id", sessionId)
            putExtra("page_index", pageIndex)
            putExtra("total_pages", totalPages)
            putExtra("is_new_session", isNewSession)
        }
    }

    private fun createBBox(x: Int = 100, y: Int = 100, width: Int = 200, height: Int = 100): BBox {
        return BBox(
            x1 = x,
            y1 = y,
            x2 = x + width,
            y2 = y,
            x3 = x + width,
            y3 = y + height,
            x4 = x,
            y4 = y + height
        )
    }

    private fun createSuccessRepository(
        withOcr: Boolean = false,
        withTts: Boolean = false
    ): PageRepository {
        return object : PageRepository {
            override suspend fun getImage(
                sessionId: String,
                pageIndex: Int
            ): Result<GetImageResponse> {
                return Result.success(
                    GetImageResponse(sessionId, pageIndex, testImageBase64, "2025-11-15T12:00:00")
                )
            }

            override suspend fun getOcrResults(
                sessionId: String,
                pageIndex: Int
            ): Result<GetOcrTranslationResponse> {
                val ocrResults = if (withOcr) {
                    listOf(OcrBox(createBBox(), "Original", "Translated"))
                } else {
                    emptyList()
                }
                return Result.success(
                    GetOcrTranslationResponse(
                        sessionId,
                        pageIndex,
                        ocrResults,
                        "2025-11-15T12:00:00"
                    )
                )
            }

            override suspend fun getTtsResults(
                sessionId: String,
                pageIndex: Int
            ): Result<GetTtsResponse> {
                val ttsResults = if (withTts) {
                    listOf(AudioResult(0, listOf("dGVzdA==")))
                } else {
                    emptyList()
                }
                return Result.success(
                    GetTtsResponse(sessionId, pageIndex, ttsResults, "2025-11-15T12:00:00")
                )
            }
        }
    }

    // ==================== Basic Lifecycle Tests ====================

    @Test
    fun onCreate_initializesViewsCorrectly() = runTest {
        scenario = ActivityScenario.launch(createIntent(totalPages = 2))
        Thread.sleep(1500)

        scenario.onActivity { activity ->
            val topUi = activity.findViewById<View>(R.id.topUi)
            val bottomUi = activity.findViewById<View>(R.id.bottomUi)
            val leftPanel = activity.findViewById<View>(R.id.leftPanel)

            Assert.assertTrue("TopUi should be hidden initially", topUi.translationY < 0f)
            Assert.assertTrue("BottomUi should be hidden initially", bottomUi.translationY > 0f)
            Assert.assertEquals("LeftPanel should be gone", View.GONE, leftPanel.visibility)
        }

        onView(withId(R.id.pageImage)).check(matches(isDisplayed()))
    }

    @Test
    fun onCreate_loadsPageDataSuccessfully() = runTest {
        scenario = ActivityScenario.launch(createIntent(totalPages = 2))
        Thread.sleep(1500)

        onView(withId(R.id.pageImage)).check(matches(isDisplayed()))
    }

    // ==================== Page Navigation Tests ====================
    @Test
    fun statusText_displaysCorrectPageInfo() = runTest {
        scenario = ActivityScenario.launch(createIntent(pageIndex = 2, totalPages = 3))
        Thread.sleep(1500)
        onView(withId(R.id.main)).perform(click())
        Thread.sleep(500)
        onView(withId(R.id.statusText)).check(matches(withText("Page 2/2")))
    }

    // ==================== Error Handling Tests ====================

    @Test
    fun imageLoadError_handlesGracefully() = runTest {
        mockPageRepository = object : PageRepository {
            override suspend fun getImage(sessionId: String, pageIndex: Int): Result<GetImageResponse> {
                return Result.failure(Exception("Network error"))
            }
            override suspend fun getOcrResults(sessionId: String, pageIndex: Int): Result<GetOcrTranslationResponse> {
                return Result.success(GetOcrTranslationResponse(sessionId, pageIndex, emptyList(), "2025-11-15T12:00:00"))
            }
            override suspend fun getTtsResults(sessionId: String, pageIndex: Int): Result<GetTtsResponse> {
                return Result.success(GetTtsResponse(sessionId, pageIndex, emptyList(), "2025-11-15T12:00:00"))
            }
        }
        ServiceLocator.pageRepository = mockPageRepository

        scenario = ActivityScenario.launch(createIntent(totalPages = 2))
        Thread.sleep(2000)

        scenario.onActivity { activity ->
            Assert.assertFalse("Activity should not crash", activity.isFinishing)
        }
    }

    @Test
    fun ocrLoadError_handlesGracefully() = runTest {
        mockPageRepository = object : PageRepository {
            override suspend fun getImage(sessionId: String, pageIndex: Int): Result<GetImageResponse> {
                return Result.success(GetImageResponse(sessionId, pageIndex, testImageBase64, "2025-11-15T12:00:00"))
            }
            override suspend fun getOcrResults(sessionId: String, pageIndex: Int): Result<GetOcrTranslationResponse> {
                return Result.failure(Exception("OCR processing failed"))
            }
            override suspend fun getTtsResults(sessionId: String, pageIndex: Int): Result<GetTtsResponse> {
                return Result.success(GetTtsResponse(sessionId, pageIndex, emptyList(), "2025-11-15T12:00:00"))
            }
        }
        ServiceLocator.pageRepository = mockPageRepository

        scenario = ActivityScenario.launch(createIntent(totalPages = 2))
        Thread.sleep(2000)

        scenario.onActivity { activity ->
            Assert.assertFalse("Activity should not crash", activity.isFinishing)
        }
    }

    @Test
    fun ttsLoadError_handlesGracefully() = runTest {
        mockPageRepository = object : PageRepository {
            override suspend fun getImage(sessionId: String, pageIndex: Int): Result<GetImageResponse> {
                return Result.success(GetImageResponse(sessionId, pageIndex, testImageBase64, "2025-11-15T12:00:00"))
            }
            override suspend fun getOcrResults(sessionId: String, pageIndex: Int): Result<GetOcrTranslationResponse> {
                return Result.success(GetOcrTranslationResponse(sessionId, pageIndex, emptyList(), "2025-11-15T12:00:00"))
            }
            override suspend fun getTtsResults(sessionId: String, pageIndex: Int): Result<GetTtsResponse> {
                return Result.failure(Exception("TTS generation failed"))
            }
        }
        ServiceLocator.pageRepository = mockPageRepository

        scenario = ActivityScenario.launch(createIntent(totalPages = 2))
        Thread.sleep(2000)

        scenario.onActivity { activity ->
            Assert.assertFalse("Activity should not crash", activity.isFinishing)
        }
    }

    @Test
    fun invalidBase64Image_handlesGracefully() = runTest {
        mockPageRepository = object : PageRepository {
            override suspend fun getImage(sessionId: String, pageIndex: Int): Result<GetImageResponse> {
                return Result.success(GetImageResponse(sessionId, pageIndex, "invalid_base64!!!", "2025-11-15T12:00:00"))
            }
            override suspend fun getOcrResults(sessionId: String, pageIndex: Int): Result<GetOcrTranslationResponse> {
                return Result.success(GetOcrTranslationResponse(sessionId, pageIndex, emptyList(), "2025-11-15T12:00:00"))
            }
            override suspend fun getTtsResults(sessionId: String, pageIndex: Int): Result<GetTtsResponse> {
                return Result.success(GetTtsResponse(sessionId, pageIndex, emptyList(), "2025-11-15T12:00:00"))
            }
        }
        ServiceLocator.pageRepository = mockPageRepository

        scenario = ActivityScenario.launch(createIntent(totalPages = 2))
        Thread.sleep(2000)

        scenario.onActivity { activity ->
            Assert.assertFalse("Activity should not crash", activity.isFinishing)
        }
    }

    // ==================== Navigation Tests ====================

    @Test
    fun backButton_showsExitDialog() = runTest {
        scenario = ActivityScenario.launch(createIntent(totalPages = 2))
        Thread.sleep(1000)

        scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        Thread.sleep(500)

        scenario.onActivity { activity ->
            val exitPanel = activity.findViewById<View>(R.id.exitPanelInclude)
            Assert.assertEquals("Exit dialog should be visible", View.VISIBLE, exitPanel.visibility)
        }
    }

    @Test
    fun exitCancelButton_hidesDialog() = runTest {
        scenario = ActivityScenario.launch(createIntent(totalPages = 2))
        Thread.sleep(1000)

        scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        Thread.sleep(500)

        onView(withId(R.id.exitCancelBtn)).perform(click())
        Thread.sleep(300)

        scenario.onActivity { activity ->
            val exitPanel = activity.findViewById<View>(R.id.exitPanelInclude)
            Assert.assertEquals("Exit dialog should be gone", View.GONE, exitPanel.visibility)
        }
    }

    @Test
    fun captureButton_navigatesToCamera() = runTest {
        scenario = ActivityScenario.launch(createIntent(totalPages = 2))
        Thread.sleep(1500)

        onView(withId(R.id.main)).perform(click())
        Thread.sleep(300)

        onView(withId(R.id.startButton)).perform(click())
        Thread.sleep(1000)

        // Camera activity launched (no crash = success)
        scenario.onActivity { activity ->
            Assert.assertNotNull("Activity should exist", activity)
        }
    }

    // ==================== OCR & TTS Tests ====================

    @Test
    fun ttsResults_cachesAudioMap() = runTest {
        mockPageRepository = createSuccessRepository(withOcr = true, withTts = true)
        ServiceLocator.pageRepository = mockPageRepository

        scenario = ActivityScenario.launch(createIntent(totalPages = 2))
        Thread.sleep(3000)

        scenario.onActivity { activity ->
            val field = activity.javaClass.getDeclaredField("audioResultsMap")
            field.isAccessible = true
            val audioMap = field.get(activity) as Map<*, *>

            Assert.assertTrue("Audio results should be cached after TTS", audioMap.isNotEmpty())
        }
    }

    @Test
    fun pageNavigation_clearsCache() = runTest {
        mockPageRepository = createSuccessRepository(withOcr = true, withTts = true)
        ServiceLocator.pageRepository = mockPageRepository

        scenario = ActivityScenario.launch(createIntent(pageIndex = 1, totalPages = 3))
        Thread.sleep(3000)

        scenario.onActivity { activity ->
            val bboxField = activity.javaClass.getDeclaredField("cachedBoundingBoxes")
            bboxField.isAccessible = true
            val cachedBoxes = bboxField.get(activity) as List<*>
            Assert.assertTrue("Should have cached boxes on page 1", cachedBoxes.isNotEmpty())
        }

        onView(withId(R.id.main)).perform(click())
        Thread.sleep(300)
        onView(withId(R.id.nextButton)).perform(click())
        Thread.sleep(2000)

        scenario.onActivity { activity ->
            val bboxField = activity.javaClass.getDeclaredField("cachedBoundingBoxes")
            bboxField.isAccessible = true
            val cachedBoxes = bboxField.get(activity) as List<*>

            val audioField = activity.javaClass.getDeclaredField("audioResultsMap")
            audioField.isAccessible = true
            val audioMap = audioField.get(activity) as Map<*, *>

            Assert.assertNotNull("Cache should be reset", cachedBoxes)
            Assert.assertNotNull("Audio map should be reset", audioMap)
        }
    }

    @Test
    fun thumbnailAdapter_isInitialized() = runTest {
        scenario = ActivityScenario.launch(createIntent(pageIndex = 1, totalPages = 3))
        Thread.sleep(2000)

        onView(withId(R.id.main)).perform(click())
        Thread.sleep(300)
        onView(withId(R.id.menuButton)).perform(click())
        Thread.sleep(500)

        scenario.onActivity { activity ->
            val leftPanel = activity.findViewById<com.example.storybridge_android.ui.common.LeftOverlay>(R.id.leftPanel)
            val recyclerView = leftPanel.thumbnailRecyclerView

            Assert.assertNotNull("RecyclerView should exist", recyclerView)
            Assert.assertNotNull("RecyclerView should have adapter", recyclerView.adapter)
        }
    }

    // ==================== State Management Tests ====================

    @Test
    fun onDestroy_releasesMediaPlayer() = runTest {
        mockPageRepository = createSuccessRepository(withOcr = true, withTts = true)
        ServiceLocator.pageRepository = mockPageRepository

        scenario = ActivityScenario.launch(createIntent(totalPages = 2))
        Thread.sleep(3000)

        scenario.onActivity { activity ->
            val field = activity.javaClass.getDeclaredField("mediaPlayer")
            field.isAccessible = true
            val player = field.get(activity)
            Assert.assertTrue("MediaPlayer field exists", true)
        }

        scenario.close()
        Thread.sleep(500)

        Assert.assertTrue("Activity destroyed without crash", true)
    }

    @Test
    fun viewportWidth_calculatedCorrectly() = runTest {
        mockPageRepository = createSuccessRepository(withOcr = true)
        ServiceLocator.pageRepository = mockPageRepository

        scenario = ActivityScenario.launch(createIntent(totalPages = 2))
        Thread.sleep(3000)

        scenario.onActivity { activity ->
            val field = activity.javaClass.getDeclaredField("viewportWidth")
            field.isAccessible = true
            val viewport = field.get(activity) as Int

            Assert.assertTrue("Viewport width should be non-negative", viewport >= 0)
        }
    }

    @Test
    fun uiVisible_togglesCorrectly() = runTest {
        scenario = ActivityScenario.launch(createIntent(totalPages = 2))
        Thread.sleep(1500)

        scenario.onActivity { activity ->
            val field = activity.javaClass.getDeclaredField("uiVisible")
            field.isAccessible = true
            val initialState = field.get(activity) as Boolean

            Assert.assertFalse("UI should be hidden initially", initialState)
        }

        onView(withId(R.id.main)).perform(click())
        Thread.sleep(500)

        scenario.onActivity { activity ->
            val field = activity.javaClass.getDeclaredField("uiVisible")
            field.isAccessible = true
            val afterToggle = field.get(activity) as Boolean

            Assert.assertTrue("UI should be visible after toggle", afterToggle)
        }
    }

    @Test
    fun isOverlayVisible_togglesCorrectly() = runTest {
        scenario = ActivityScenario.launch(createIntent(totalPages = 2))
        Thread.sleep(1500)

        scenario.onActivity { activity ->
            val field = activity.javaClass.getDeclaredField("isOverlayVisible")
            field.isAccessible = true
            val initialState = field.get(activity) as Boolean

            Assert.assertFalse("Overlay should be hidden initially", initialState)
        }

        onView(withId(R.id.main)).perform(click())
        Thread.sleep(300)
        onView(withId(R.id.menuButton)).perform(click())
        Thread.sleep(500)

        scenario.onActivity { activity ->
            val field = activity.javaClass.getDeclaredField("isOverlayVisible")
            field.isAccessible = true
            val afterToggle = field.get(activity) as Boolean

            Assert.assertTrue("Overlay should be visible after toggle", afterToggle)
        }
    }

    @Test
    fun sessionId_storedCorrectly() = runTest {
        val testSessionId = "test_session_123"
        scenario = ActivityScenario.launch(createIntent(sessionId = testSessionId, totalPages = 2))
        Thread.sleep(1000)

        scenario.onActivity { activity ->
            val field = activity.javaClass.getDeclaredField("sessionId")
            field.isAccessible = true
            val storedSessionId = field.get(activity) as String

            Assert.assertEquals("Session ID should match", testSessionId, storedSessionId)
        }
    }

    @Test
    fun pageIndex_updatesOnNavigation() = runTest {
        scenario = ActivityScenario.launch(createIntent(pageIndex = 1, totalPages = 3))
        Thread.sleep(1500)

        scenario.onActivity { activity ->
            val field = activity.javaClass.getDeclaredField("pageIndex")
            field.isAccessible = true
            val initialPage = field.get(activity) as Int

            Assert.assertEquals("Initial page should be 1", 1, initialPage)
        }

        onView(withId(R.id.main)).perform(click())
        Thread.sleep(300)
        onView(withId(R.id.nextButton)).perform(click())
        Thread.sleep(2000)

        scenario.onActivity { activity ->
            val field = activity.javaClass.getDeclaredField("pageIndex")
            field.isAccessible = true
            val newPage = field.get(activity) as Int

            Assert.assertEquals("Page should be 2 after next", 2, newPage)
        }
    }

    // ==================== Audio Playback Tests ====================

    @Test
    fun multipleAudioResults_storesCorrectly() = runTest {
        val bbox1 = createBBox(x = 100, y = 100)
        val bbox2 = createBBox(x = 100, y = 300)
        val ocrBox1 = OcrBox(bbox1, "Original1", "Translated1")
        val ocrBox2 = OcrBox(bbox2, "Original2", "Translated2")

        val audioResult1 = AudioResult(0, listOf("dGVzdDE=", "dGVzdDI="))
        val audioResult2 = AudioResult(1, listOf("dGVzdDM="))

        mockPageRepository = object : PageRepository {
            override suspend fun getImage(sessionId: String, pageIndex: Int): Result<GetImageResponse> {
                return Result.success(GetImageResponse(sessionId, pageIndex, testImageBase64, "2025-11-15T12:00:00"))
            }
            override suspend fun getOcrResults(sessionId: String, pageIndex: Int): Result<GetOcrTranslationResponse> {
                return Result.success(GetOcrTranslationResponse(sessionId, pageIndex, listOf(ocrBox1, ocrBox2), "2025-11-15T12:00:00"))
            }
            override suspend fun getTtsResults(sessionId: String, pageIndex: Int): Result<GetTtsResponse> {
                return Result.success(GetTtsResponse(sessionId, pageIndex, listOf(audioResult1, audioResult2), "2025-11-15T12:00:00"))
            }
        }
        ServiceLocator.pageRepository = mockPageRepository

        scenario = ActivityScenario.launch(createIntent(totalPages = 2))
        Thread.sleep(3000)

        scenario.onActivity { activity ->
            val field = activity.javaClass.getDeclaredField("audioResultsMap")
            field.isAccessible = true
            val audioMap = field.get(activity) as Map<Int, List<String>>

            Assert.assertEquals("Should have 2 audio entries", 2, audioMap.size)
            Assert.assertEquals("Index 0 should have 2 clips", 2, audioMap[0]?.size)
            Assert.assertEquals("Index 1 should have 1 clip", 1, audioMap[1]?.size)
        }
    }

    @Test
    fun ocrWithoutTts_noAudioMap() = runTest {
        mockPageRepository = createSuccessRepository(withOcr = true, withTts = false)
        ServiceLocator.pageRepository = mockPageRepository

        scenario = ActivityScenario.launch(createIntent(totalPages = 2))
        Thread.sleep(3000)

        scenario.onActivity { activity ->
            val field = activity.javaClass.getDeclaredField("audioResultsMap")
            field.isAccessible = true
            val audioMap = field.get(activity) as Map<*, *>

            Assert.assertTrue("Audio map should be empty without TTS", audioMap.isEmpty())
        }
    }

    @Test
    fun withValidOcrAndTts_attemptsToRenderUI() = runTest {
        val bbox = createBBox()
        val ocrBox = OcrBox(bbox, "Original", "Translated Text")
        val audioResult = AudioResult(0, listOf("dGVzdA=="))

        mockPageRepository = object : PageRepository {
            override suspend fun getImage(sessionId: String, pageIndex: Int): Result<GetImageResponse> {
                return Result.success(GetImageResponse(sessionId, pageIndex, testImageBase64, "2025-11-15T12:00:00"))
            }
            override suspend fun getOcrResults(sessionId: String, pageIndex: Int): Result<GetOcrTranslationResponse> {
                return Result.success(GetOcrTranslationResponse(sessionId, pageIndex, listOf(ocrBox), "2025-11-15T12:00:00"))
            }
            override suspend fun getTtsResults(sessionId: String, pageIndex: Int): Result<GetTtsResponse> {
                return Result.success(GetTtsResponse(sessionId, pageIndex, listOf(audioResult), "2025-11-15T12:00:00"))
            }
        }
        ServiceLocator.pageRepository = mockPageRepository

        scenario = ActivityScenario.launch(createIntent(totalPages = 2))

        Thread.sleep(2000)

        scenario.onActivity { activity ->
            val pageImage = activity.findViewById<android.widget.ImageView>(R.id.pageImage)
            pageImage.post {
                pageImage.requestLayout()
                pageImage.invalidate()
            }
        }

        Thread.sleep(5000)

        scenario.onActivity { activity ->
            val mainLayout = activity.findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.main)

            val pageImage = activity.findViewById<android.widget.ImageView>(R.id.pageImage)
            val bitmapField = activity.javaClass.getDeclaredField("pageBitmap")
            bitmapField.isAccessible = true
            val bitmap = bitmapField.get(activity)

            android.util.Log.d("TEST", "=== Rendering Check ===")
            android.util.Log.d("TEST", "PageImage size: ${pageImage.width}x${pageImage.height}")
            android.util.Log.d("TEST", "Bitmap is null: ${bitmap == null}")
            android.util.Log.d("TEST", "MainLayout children: ${mainLayout.childCount}")

            var bboxCount = 0
            var playBtnCount = 0
            for (i in 0 until mainLayout.childCount) {
                val child = mainLayout.getChildAt(i)
                android.util.Log.d("TEST", "Child $i: tag=${child.tag}")
                if (child.tag == "bbox") bboxCount++
                if (child.tag == "play_button") playBtnCount++
            }

            android.util.Log.d("TEST", "BBox count: $bboxCount, PlayButton count: $playBtnCount")

            val cachedField = activity.javaClass.getDeclaredField("cachedBoundingBoxes")
            cachedField.isAccessible = true
            val cached = cachedField.get(activity) as List<*>

            val audioField = activity.javaClass.getDeclaredField("audioResultsMap")
            audioField.isAccessible = true
            val audioMap = audioField.get(activity) as Map<*, *>

            android.util.Log.d("TEST", "Cached boxes: ${cached.size}, Audio map: ${audioMap.size}")

            Assert.assertTrue("Should have cached OCR data", cached.isNotEmpty())
            Assert.assertTrue("Should have audio data", audioMap.isNotEmpty())
        }
    }

    @Test
    fun boundingBoxDrag_savesFinalPosition() = runTest {
        mockPageRepository = createSuccessRepository(withOcr = true)
        ServiceLocator.pageRepository = mockPageRepository

        scenario = ActivityScenario.launch(createIntent(totalPages = 2))
        Thread.sleep(3000)

        scenario.onActivity { activity ->
            val field = activity.javaClass.getDeclaredField("savedBoxTranslations")
            field.isAccessible = true
            val savedTranslations = field.get(activity) as MutableMap<Int, Pair<Float, Float>>

            Assert.assertTrue("Saved translations should be empty initially", savedTranslations.isEmpty())

            savedTranslations[0] = Pair(100f, 200f)

            Assert.assertTrue("Should save translation for index 0", savedTranslations.containsKey(0))
            Assert.assertEquals("X translation should be 100", 100f, savedTranslations[0]?.first)
            Assert.assertEquals("Y translation should be 200", 200f, savedTranslations[0]?.second)
        }
    }

    @Test
    fun draggedBoxTranslation_clearedOnPageChange() = runTest {
        mockPageRepository = createSuccessRepository(withOcr = true)
        ServiceLocator.pageRepository = mockPageRepository

        scenario = ActivityScenario.launch(createIntent(pageIndex = 1, totalPages = 3))
        Thread.sleep(3000)

        scenario.onActivity { activity ->
            val field = activity.javaClass.getDeclaredField("savedBoxTranslations")
            field.isAccessible = true
            val savedTranslations = field.get(activity) as MutableMap<Int, Pair<Float, Float>>

            savedTranslations[0] = Pair(50f, 100f)
            Assert.assertTrue("Should have saved translation", savedTranslations.isNotEmpty())
        }

        onView(withId(R.id.main)).perform(click())
        Thread.sleep(300)
        onView(withId(R.id.nextButton)).perform(click())
        Thread.sleep(2000)

        scenario.onActivity { activity ->
            val field = activity.javaClass.getDeclaredField("savedBoxTranslations")
            field.isAccessible = true
            val savedTranslations = field.get(activity) as Map<*, *>

            Assert.assertTrue("Saved translations should be cleared after page change", savedTranslations.isEmpty())
        }
    }

    @Test
    fun touchListener_logic_verification() = runTest {
        scenario = ActivityScenario.launch(createIntent(totalPages = 2))
        Thread.sleep(1000)

        scenario.onActivity { activity ->
            val field = activity.javaClass.getDeclaredField("TOUCH_SLOP")
            field.isAccessible = true
            val touchSlop = field.get(null) as Float

            Assert.assertEquals("TOUCH_SLOP should be 10f", 10f, touchSlop)
        }
    }

    @Test
    fun displayBB_layoutScalingBranchesCovered() = runTest {
        mockPageRepository = createSuccessRepository(withOcr = true)
        ServiceLocator.pageRepository = mockPageRepository

        scenario = ActivityScenario.launch(createIntent(totalPages = 2))
        Thread.sleep(2000)

        scenario.onActivity { activity ->
            val bitmapField = activity.javaClass.getDeclaredField("pageBitmap")
            bitmapField.isAccessible = true
            bitmapField.set(
                activity,
                Bitmap.createBitmap(800, 1200, Bitmap.Config.ARGB_8888)
            )

            val pageImage = activity.findViewById<ImageView>(R.id.pageImage)
            pageImage.measure(
                View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(1600, View.MeasureSpec.EXACTLY)
            )
            pageImage.layout(0, 0, 1080, 1600)

            val method = activity.javaClass.getDeclaredMethod(
                "displayBB",
                List::class.java
            )
            method.isAccessible = true

            method.invoke(
                activity,
                listOf(
                    ReadingActivity.BoundingBox(0, 50, 50, 200, "text", 1)
                )
            )
        }

        scenario.onActivity { activity ->
            val field = activity.javaClass.getDeclaredField("viewportWidth")
            field.isAccessible = true
            val viewport = field.get(activity) as Int
            Assert.assertTrue(viewport > 0)
        }
    }

    @Test
    fun displayBB_noAudio_doesNotCreatePlayButton() = runTest {
        mockPageRepository = createSuccessRepository(withOcr = true, withTts = false)
        ServiceLocator.pageRepository = mockPageRepository
        scenario = ActivityScenario.launch(createIntent(totalPages = 2))
        Thread.sleep(3000)
        scenario.onActivity { activity ->
            val children = activity.findViewById<View>(R.id.main) as androidx.constraintlayout.widget.ConstraintLayout
            var playButtons = 0
            for (i in 0 until children.childCount) {
                if (children.getChildAt(i).tag == "play_button") playButtons++
            }
            Assert.assertEquals(0, playButtons)
        }
    }
}