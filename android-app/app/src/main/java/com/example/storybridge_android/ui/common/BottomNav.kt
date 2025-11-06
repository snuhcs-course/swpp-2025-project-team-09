package com.example.storybridge_android.ui.common

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageButton
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.storybridge_android.R


class BottomNav @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var onPrevButtonClickListener: (() -> Unit)? = null
    private var onCaptureButtonClickListener: (() -> Unit)? = null
    private var onNextButtonClickListener: (() -> Unit)? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.bottom_nav, this, true)
        setupClickListeners()
        findViewById<ImageButton>(R.id.prevButton).visibility = INVISIBLE
        findViewById<ImageButton>(R.id.nextButton).visibility = INVISIBLE
    }

    private fun setupClickListeners() {
        findViewById<ImageButton>(R.id.prevButton).setOnClickListener {
            onPrevButtonClickListener?.invoke()
        }

        findViewById<Button>(R.id.startButton).setOnClickListener {
            onCaptureButtonClickListener?.invoke()
        }

        findViewById<ImageButton>(R.id.nextButton).setOnClickListener {
            onNextButtonClickListener?.invoke()
        }
    }

    fun setOnPrevButtonClickListener(listener: () -> Unit) {
        onPrevButtonClickListener = listener
    }

    fun setOnCaptureButtonClickListener(listener: () -> Unit) {
        onCaptureButtonClickListener = listener
    }

    fun setOnNextButtonClickListener(listener: () -> Unit) {
        onNextButtonClickListener = listener
    }

    fun configure(hasPrevious: Boolean, hasNext: Boolean) {
        findViewById<ImageButton>(R.id.prevButton).visibility = if (hasPrevious) VISIBLE else INVISIBLE
        findViewById<ImageButton>(R.id.nextButton).visibility = if (hasNext) VISIBLE else INVISIBLE
    }
}
