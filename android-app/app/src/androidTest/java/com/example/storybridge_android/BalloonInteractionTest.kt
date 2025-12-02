package com.example.storybridge_android.ui.session

import android.content.Context
import android.view.MotionEvent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.storybridge_android.data.BalloonColor
import com.example.storybridge_android.data.CongratulationBalloon
import com.example.storybridge_android.ui.session.finish.FinishBalloonView
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BalloonInteractionTest {

    private lateinit var context: Context
    private lateinit var balloonView: FinishBalloonView

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        balloonView = FinishBalloonView(context)
    }

    @After
    fun tearDown() {
        balloonView.stopAnimation()
    }

    // ========== Initialization Tests ==========

    @Test
    fun view_initializes_successfully() {
        assertNotNull(balloonView)
    }

    @Test
    fun view_startsWithNoBalloons() {
        assertNull(balloonView.getBalloonText(0))
        assertNull(balloonView.getBalloonText(1))
    }

    // ========== setBalloons Tests ==========

    @Test
    fun setBalloons_withEmptyList_clearsExistingBalloons() {
        // Given
        val initialBalloons = listOf(
            CongratulationBalloon(100f, 100f, 200f, 200f, BalloonColor.RED, 0, "Test")
        )
        balloonView.setBalloons(initialBalloons)

        // When
        balloonView.setBalloons(emptyList())

        // Then
        assertNull(balloonView.getBalloonText(0))
    }

    @Test
    fun setBalloons_withValidList_storesBalloons() {
        // Given
        val balloons = listOf(
            CongratulationBalloon(100f, 100f, 200f, 200f, BalloonColor.RED, 0, "Hello"),
            CongratulationBalloon(300f, 300f, 200f, 200f, BalloonColor.BLUE, 1, "World")
        )

        // When
        balloonView.setBalloons(balloons)

        // Then
        assertEquals("Hello", balloonView.getBalloonText(0))
        assertEquals("World", balloonView.getBalloonText(1))
    }

    @Test
    fun setBalloons_multipleTimes_replacesExistingBalloons() {
        // Given
        val firstBalloons = listOf(
            CongratulationBalloon(100f, 100f, 200f, 200f, BalloonColor.RED, 0, "First")
        )
        balloonView.setBalloons(firstBalloons)

        // When
        val secondBalloons = listOf(
            CongratulationBalloon(200f, 200f, 200f, 200f, BalloonColor.GREEN, 1, "Second")
        )
        balloonView.setBalloons(secondBalloons)

        // Then
        assertNull(balloonView.getBalloonText(0))
        assertEquals("Second", balloonView.getBalloonText(1))
    }

    // ========== getBalloonText Tests ==========

    @Test
    fun getBalloonText_withValidLineIndex_returnsCorrectText() {
        // Given
        val balloons = listOf(
            CongratulationBalloon(100f, 100f, 200f, 200f, BalloonColor.RED, 0, "Line 0"),
            CongratulationBalloon(200f, 200f, 200f, 200f, BalloonColor.GREEN, 1, "Line 1"),
            CongratulationBalloon(300f, 300f, 200f, 200f, BalloonColor.BLUE, 2, "Line 2")
        )
        balloonView.setBalloons(balloons)

        // When & Then
        assertEquals("Line 0", balloonView.getBalloonText(0))
        assertEquals("Line 1", balloonView.getBalloonText(1))
        assertEquals("Line 2", balloonView.getBalloonText(2))
    }

    @Test
    fun getBalloonText_withInvalidLineIndex_returnsNull() {
        // Given
        val balloons = listOf(
            CongratulationBalloon(100f, 100f, 200f, 200f, BalloonColor.RED, 0, "Test")
        )
        balloonView.setBalloons(balloons)

        // When & Then
        assertNull(balloonView.getBalloonText(99))
        assertNull(balloonView.getBalloonText(-1))
    }

    // ========== Touch Event Tests ==========

    @Test
    fun onTouchEvent_outsideBalloon_doesNothing() {
        // Given
        val balloon = CongratulationBalloon(500f, 500f, 200f, 200f, BalloonColor.RED, 0, "Untouched")
        balloonView.setBalloons(listOf(balloon))

        // When
        val touchEvent = MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_DOWN, 100f, 100f, 0)
        balloonView.onTouchEvent(touchEvent)
        touchEvent.recycle()

        // Then
        assertFalse(balloon.isPopping)
        assertFalse(balloon.isPopped)
    }

    @Test
    fun onTouchEvent_onAlreadyPoppedBalloon_doesNothing() {
        // Given
        val balloon = CongratulationBalloon(500f, 500f, 200f, 200f, BalloonColor.RED, 0, "Already Popped")
        balloon.isPopped = true
        balloonView.setBalloons(listOf(balloon))

        var callbackCount = 0
        balloonView.onBalloonPopped = { _, _ ->
            callbackCount++
        }

        // When
        val touchEvent = MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_DOWN, 500f, 500f, 0)
        balloonView.onTouchEvent(touchEvent)
        touchEvent.recycle()

        // Wait a bit
        Thread.sleep(100)

        // Then
        assertEquals(0, callbackCount)
    }

    // ========== Callback Tests ==========

    @Test
    fun onAllBalloonsPopped_callback_notInvokedWithEmptyList() {
        // Given
        balloonView.setBalloons(emptyList())

        var callbackInvoked = false
        balloonView.onAllBalloonsPopped = {
            callbackInvoked = true
        }

        // Wait some time
        Thread.sleep(500)

        // Then
        assertFalse(callbackInvoked)
    }

    // ========== Color Variant Tests ==========

    @Test
    fun setBalloons_withDifferentColors_allColorsSupported() {
        // Given
        val balloons = listOf(
            CongratulationBalloon(100f, 100f, 200f, 200f, BalloonColor.RED, 0, "Red"),
            CongratulationBalloon(300f, 300f, 200f, 200f, BalloonColor.GREEN, 1, "Green"),
            CongratulationBalloon(500f, 500f, 200f, 200f, BalloonColor.BLUE, 2, "Blue")
        )

        // When
        balloonView.setBalloons(balloons)

        // Then
        assertEquals("Red", balloonView.getBalloonText(0))
        assertEquals("Green", balloonView.getBalloonText(1))
        assertEquals("Blue", balloonView.getBalloonText(2))
    }

    // ========== Animation Control Tests ==========

    @Test
    fun stopAnimation_stopsUpdateLoop() {
        // Given
        val balloon = CongratulationBalloon(500f, 500f, 200f, 200f, BalloonColor.RED, 0, "Test")
        balloonView.setBalloons(listOf(balloon))

        // When
        balloonView.stopAnimation()

        val touch = MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_DOWN, 500f, 500f, 0)
        balloonView.onTouchEvent(touch)
        touch.recycle()

        Thread.sleep(500)

        // Then - Should be popping but not popped (update loop stopped)
        assertTrue(balloon.isPopping)
        assertFalse(balloon.isPopped)
    }

    // ========== Edge Case Tests ==========

    @Test
    fun onTouchEvent_withNoBalloons_doesNotCrash() {
        // When
        val touch = MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_DOWN, 500f, 500f, 0)
        val result = balloonView.onTouchEvent(touch)
        touch.recycle()

        // Then
        assertTrue(result)
    }

    @Test
    fun balloon_withZeroDimensions_handledGracefully() {
        // Given
        val balloon = CongratulationBalloon(500f, 500f, 0f, 0f, BalloonColor.RED, 0, "Zero")
        balloonView.setBalloons(listOf(balloon))

        // When
        val touch = MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_DOWN, 500f, 500f, 0)
        balloonView.onTouchEvent(touch)
        touch.recycle()

        // Then
        assertEquals("Zero", balloonView.getBalloonText(0))
    }
}