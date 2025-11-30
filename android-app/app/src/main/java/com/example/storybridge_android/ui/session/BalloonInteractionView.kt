package com.example.storybridge_android.ui.session

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.res.ResourcesCompat
import com.example.storybridge_android.R
import com.example.storybridge_android.data.BalloonColor
import com.example.storybridge_android.data.BalloonData
import kotlin.math.min

class BalloonInteractionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val balloons = mutableListOf<BalloonData>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 64f
        color = 0xFF000000.toInt()
        typeface = ResourcesCompat.getFont(context, R.font.pinkfong_baby_shark_bold)
        isFakeBoldText = true
    }

    // Update interval (≈ 60fps)
    private val frameDelay = 16L

    // Total duration of pop animation
    private val popDuration = 400L

    // Bitmaps per balloon color
    private val balloonBitmaps = mutableMapOf<BalloonColor, Bitmap>()
    private val popBitmaps = mutableMapOf<BalloonColor, List<Bitmap>>()

    // Callbacks
    var onBalloonPopped: ((Int, String) -> Unit)? = null  // (lineIndex, text)
    var onAllBalloonsPopped: (() -> Unit)? = null

    private val updateRunnable = object : Runnable {
        override fun run() {
            updateBalloons()
            invalidate()
            postDelayed(this, frameDelay)
        }
    }

    init {
        loadBitmaps()
        post(updateRunnable)
    }
    /**
     * Load balloon bitmaps
     */
    private fun loadBitmaps() {
        // Red balloon
        balloonBitmaps[BalloonColor.RED] = BitmapFactory.decodeResource(
            resources, R.drawable.balloon_red
        )
        popBitmaps[BalloonColor.RED] = listOf(
            BitmapFactory.decodeResource(resources, R.drawable.balloon_pop_1_red),
            BitmapFactory.decodeResource(resources, R.drawable.balloon_pop_2_red)
        )

        // Green balloon
        balloonBitmaps[BalloonColor.GREEN] = BitmapFactory.decodeResource(
            resources, R.drawable.balloon_green
        )
        popBitmaps[BalloonColor.GREEN] = listOf(
            BitmapFactory.decodeResource(resources, R.drawable.balloon_pop_1_green),
            BitmapFactory.decodeResource(resources, R.drawable.balloon_pop_2_green)
        )

        // Blue balloon
        balloonBitmaps[BalloonColor.BLUE] = BitmapFactory.decodeResource(
            resources, R.drawable.balloon_blue
        )
        popBitmaps[BalloonColor.BLUE] = listOf(
            BitmapFactory.decodeResource(resources, R.drawable.balloon_pop_1_blue),
            BitmapFactory.decodeResource(resources, R.drawable.balloon_pop_2_blue)
        )
    }
    /**
     * Set balloon data
     */
    fun setBalloons(balloonList: List<BalloonData>) {
        balloons.clear()
        balloons.addAll(balloonList)
        invalidate()
    }
    /**
     * Update balloon states every frame
     */
    private fun updateBalloons() {
        var allPopped = true

        for (b in balloons) {
            if (b.isPopping) {
                // While popping: accumulate elapsed time
                b.popElapsed += frameDelay
                if (b.popElapsed >= popDuration) {
                    // Pop animation finished
                    b.isPopping = false
                    b.isPopped = true
                    // Invoke callback when balloon fully popped
                    onBalloonPopped?.invoke(b.lineIndex, b.text)
                }
            }

            if (!b.isPopped) {
                allPopped = false
            }
        }
        // Check if all balloons are popped
        if (allPopped && balloons.isNotEmpty()) {
            onAllBalloonsPopped?.invoke()
            // Clear callback to fire only once
            onAllBalloonsPopped = null
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        for (b in balloons) {
            // Skip drawing if already popped
            if (b.isPopped) continue
            val left = b.x - b.width / 2f
            val top = b.y - b.height / 2f
            val right = b.x + b.width / 2f
            val bottom = b.y + b.height / 2f
            val dstRect = RectF(left, top, right, bottom)

            if (!b.isPopping) {
                // Normal balloon
                paint.alpha = 255
                val bitmap = balloonBitmaps[b.color]
                bitmap?.let {
                    canvas.drawBitmap(it, null, dstRect, paint)
                }
            } else {
                // Pop progress 0~1
                val tRaw = b.popElapsed.toFloat() / popDuration.toFloat()
                val t = min(1f, tRaw)

                // Alpha fade (gradually transparent)
                val alpha = ((1f - t) * 255f).toInt().coerceIn(0, 255)
                paint.alpha = alpha

                // Choose effect bitmap based on t intervals
                val bitmaps = popBitmaps[b.color] ?: emptyList()
                val bmp = if (t < 0.4f && bitmaps.isNotEmpty()) {
                    bitmaps[0]
                } else if (bitmaps.size > 1) {
                    bitmaps[1]
                } else {
                    bitmaps.firstOrNull()
                }

                bmp?.let {
                    canvas.drawBitmap(it, null, dstRect, paint)
                }
            }
        }

        // Restore alpha before next draw
        paint.alpha = 255
    }

    /**
     * On touch, softly pop the balloon at the position
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val tx = event.x
                val ty = event.y

                // Check from topmost drawn balloon
                for (i in balloons.size - 1 downTo 0) {
                    val b = balloons[i]
                    if (b.isPopping || b.isPopped) continue

                    val left = b.x - b.width / 2f
                    val top = b.y - b.height / 2f
                    val right = b.x + b.width / 2f
                    val bottom = b.y + b.height / 2f

                    if (tx in left..right && ty in top..bottom) {
                        // Switch to popping state
                        b.isPopping = true
                        b.popElapsed = 0L
                        break
                    }
                }
            }
        }
        return true
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeCallbacks(updateRunnable)
    }

    /**
     * Stop the animation loop (for testing)
     */
    fun stopAnimation() {
        removeCallbacks(updateRunnable)
    }

    /**
     * Get balloon text by line index (useful for testing)
     */
    fun getBalloonText(lineIndex: Int): String? {
        return balloons.find { it.lineIndex == lineIndex }?.text
    }
}
