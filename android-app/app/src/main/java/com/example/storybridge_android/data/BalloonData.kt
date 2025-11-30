package com.example.storybridge_android.data

enum class BalloonColor {
    RED, GREEN, BLUE
}

data class BalloonData(
    var x: Float,              // Balloon center X
    var y: Float,              // Balloon center Y
    var width: Float,          // Width to render on screen
    var height: Float,         // Height to render on screen
    val color: BalloonColor,   // Balloon color
    val lineIndex: Int,        // Line index where text appears (0, 1, 2)
    val text: String,          // Text shown when the balloon pops

    // Pop state
    var isPopping: Boolean = false,    // Currently popping
    var isPopped: Boolean = false,     // Fully popped
    var popElapsed: Long = 0L         // Elapsed time since pop started (ms)
)
