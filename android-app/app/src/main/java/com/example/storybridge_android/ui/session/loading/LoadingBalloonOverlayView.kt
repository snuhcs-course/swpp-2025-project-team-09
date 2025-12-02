package com.example.storybridge_android.ui.session.loading

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.example.storybridge_android.R
import com.example.storybridge_android.data.LoadingBalloon
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

class LoadingBalloonOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val balloons = mutableListOf<LoadingBalloon>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Update interval (≈ 60fps)
    private val frameDelay = 16L

    // Total duration of pop animation
    private val popDuration = 400L

    // Minimum number of balloons to maintain on screen
    private val minBalloonCount = 5

    // PNG bitmaps (yellow balloons only)
    private val balloonBitmap: Bitmap by lazy {
        BitmapFactory.decodeResource(resources, R.drawable.balloon_yellow)
    }
    private val popBitmap1: Bitmap by lazy {
        BitmapFactory.decodeResource(resources, R.drawable.balloon_pop_1_yellow)
    }
    private val popBitmap2: Bitmap by lazy {
        BitmapFactory.decodeResource(resources, R.drawable.balloon_pop_2_yellow)
    }

    private val updateRunnable = object : Runnable {
        override fun run() {
            updateBalloons()
            invalidate()
            postDelayed(this, frameDelay)
        }
    }

    init {
        // Start animation immediately
        post(updateRunnable)

        // Create first balloon as soon as view is laid out
        viewTreeObserver.addOnGlobalLayoutListener(object : android.view.ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                viewTreeObserver.removeOnGlobalLayoutListener(this)
                if (balloons.isEmpty()) {
                    // Start with just one balloon, updateBalloons will add more gradually
                    addRandomBalloon()
                }
            }
        })
    }

    /**
     * Create a balloon with random position/size/speed
     */
    private fun addRandomBalloon() {
        if (width == 0 || height == 0) return

        val baseSize = (width.coerceAtMost(height) / 4.5f)
        val scale = Random.nextDouble(0.7, 1.2).toFloat()
        val balloonWidth = baseSize * scale

        val aspectRatio = balloonBitmap.height.toFloat() / balloonBitmap.width.toFloat()
        val balloonHeight = balloonWidth * aspectRatio

        val halfW = balloonWidth / 2f

        // Try to find a non-overlapping position
        var x = 0f
        var y = 0f
        var attempts = 0
        val maxAttempts = 10

        do {
            x = Random.nextDouble(halfW.toDouble(), (width - halfW).toDouble()).toFloat()
            y = height.toFloat() + balloonHeight   // Start below screen
            attempts++
        } while (attempts < maxAttempts && isOverlapping(x, y, balloonWidth, balloonHeight))

        val speed = Random.nextInt(8, 15).toFloat()

        balloons.add(
            LoadingBalloon(
                x = x,
                y = y,
                width = balloonWidth,
                height = balloonHeight,
                speed = speed
            )
        )
    }

    /**
     * Check if a balloon at the given position would overlap with existing balloons
     */
    private fun isOverlapping(x: Float, y: Float, width: Float, height: Float): Boolean {
        val minDistance = (width + height) / 3f  // Minimum distance between balloon centers

        for (b in balloons) {
            if (b.isPopping) continue  // Don't check against popping balloons

            val dx = x - b.x
            val dy = y - b.y
            val distance = kotlin.math.sqrt(dx * dx + dy * dy)

            val requiredDistance = minDistance + (b.width + b.height) / 3f
            if (distance < requiredDistance) {
                return true
            }
        }
        return false
    }

    /**
     * Update balloon states every frame
     */
    private fun updateBalloons() {
        if (width == 0 || height == 0) return

        val iterator = balloons.iterator()
        while (iterator.hasNext()) {
            val b = iterator.next()

            if (!b.isPopping) {
                // Not popped yet: move upward
                b.y -= b.speed

                val top = b.y - b.height / 2f
                if (top + b.height < 0) {
                    // Completely off screen, remove
                    iterator.remove()
                }
            } else {
                // Popping: accumulate elapsed time
                b.popElapsed += frameDelay
                if (b.popElapsed >= popDuration) {
                    // Pop animation complete, remove
                    iterator.remove()
                }
            }
        }

        // Maintain minimum balloon count
        if (balloons.size < minBalloonCount) {
            addRandomBalloon()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        for (b in balloons) {
            val left = b.x - b.width / 2f
            val top = b.y - b.height / 2f
            val right = b.x + b.width / 2f
            val bottom = b.y + b.height / 2f
            val dstRect = RectF(left, top, right, bottom)

            if (!b.isPopping) {
                // Normal balloon
                paint.alpha = 255
                canvas.drawBitmap(balloonBitmap, null, dstRect, paint)
            } else {
                // Pop progress 0~1
                val tRaw = b.popElapsed.toFloat() / popDuration.toFloat()
                val t = min(1f, tRaw)

                // Alpha fade (gradually transparent)
                val alpha = ((1f - t) * 255f).toInt().coerceIn(0, 255)
                paint.alpha = alpha

                // Choose effect bitmap based on t intervals
                val bmp = if (t < 0.4f) {
                    // Early: large explosion image
                    popBitmap1
                } else {
                    // Later: outer ring/fragments image
                    popBitmap2
                }

                canvas.drawBitmap(bmp, null, dstRect, paint)
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
                    if (b.isPopping) continue

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
     * Stop the animation loop
     */
    fun stopAnimation() {
        removeCallbacks(updateRunnable)
    }
}
