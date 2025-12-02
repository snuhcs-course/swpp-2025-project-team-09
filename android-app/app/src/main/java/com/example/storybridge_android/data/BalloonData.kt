package com.example.storybridge_android.data

enum class BalloonColor {
    RED, GREEN, BLUE, YELLOW
}

/**
 * Base sealed class for all balloon types
 */
sealed class Balloon(
    var x: Float = 0f,              // Balloon center X
    var y: Float = 0f,              // Balloon center Y
    var width: Float = 0f,          // Width to render on screen
    var height: Float = 0f,         // Height to render on screen
    var isPopping: Boolean = false,    // Currently popping
    var popElapsed: Long = 0L         // Elapsed time since pop started (ms)
)

/**
 * Balloon for congratulation/finish screen with text display
 */
data class CongratulationBalloon(
    val color: BalloonColor,   // Balloon color
    val lineIndex: Int,        // Line index where text appears (0, 1, 2)
    val text: String,          // Text shown when the balloon pops
    var isPopped: Boolean = false     // Fully popped
) : Balloon()

/**
 * Balloon for loading screen - simple floating animation
 */
data class LoadingBalloon(
    var speed: Float          // Upward movement speed (px/frame)
) : Balloon()

// Factory functions for convenient construction with all parameters

fun CongratulationBalloon(
    x: Float, y: Float, width: Float, height: Float,
    color: BalloonColor, lineIndex: Int, text: String,
    isPopping: Boolean = false, isPopped: Boolean = false, popElapsed: Long = 0L
) = CongratulationBalloon(color, lineIndex, text, isPopped).apply {
    this.x = x; this.y = y; this.width = width; this.height = height
    this.isPopping = isPopping; this.popElapsed = popElapsed
}

fun LoadingBalloon(
    x: Float, y: Float, width: Float, height: Float,
    speed: Float, isPopping: Boolean = false, popElapsed: Long = 0L
) = LoadingBalloon(speed).apply {
    this.x = x; this.y = y; this.width = width; this.height = height
    this.isPopping = isPopping; this.popElapsed = popElapsed
}
