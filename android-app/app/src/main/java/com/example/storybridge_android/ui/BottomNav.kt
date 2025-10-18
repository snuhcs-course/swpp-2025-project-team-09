package com.example.storybridge_android.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
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
        LayoutInflater.from(context).inflate(R.layout.sample_bottom_nav, this, true)
        setupClickListeners()
        findViewById<Button>(R.id.prevButton).visibility = View.INVISIBLE
        findViewById<Button>(R.id.nextButton).visibility = View.INVISIBLE
    }

    private fun setupClickListeners() {
        findViewById<Button>(R.id.prevButton).setOnClickListener {
            onPrevButtonClickListener?.invoke()
        }

        findViewById<Button>(R.id.startButton).setOnClickListener {
            onCaptureButtonClickListener?.invoke()
        }

        findViewById<Button>(R.id.nextButton).setOnClickListener {
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
        findViewById<Button>(R.id.prevButton).visibility = if (hasPrevious) View.VISIBLE else View.INVISIBLE
        findViewById<Button>(R.id.nextButton).visibility = if (hasNext) View.VISIBLE else View.INVISIBLE
    }
}
