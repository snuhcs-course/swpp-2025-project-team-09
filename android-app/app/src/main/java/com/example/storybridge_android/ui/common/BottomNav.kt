package com.example.storybridge_android.ui.common

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
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

    private val prevButton: Button
    private val nextButton: Button
    private val statusText: TextView

    init {
        LayoutInflater.from(context).inflate(R.layout.bottom_nav, this, true)

        prevButton = findViewById(R.id.prevButton)
        nextButton = findViewById(R.id.nextButton)
        statusText = findViewById(R.id.statusText)

        setupClickListeners()

        // Initially hide buttons
        prevButton.visibility = INVISIBLE
        nextButton.visibility = INVISIBLE
    }

    private fun setupClickListeners() {
        prevButton.setOnClickListener {
            onPrevButtonClickListener?.invoke()
        }

        findViewById<Button>(R.id.startButton).setOnClickListener {
            onCaptureButtonClickListener?.invoke()
        }

        nextButton.setOnClickListener {
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

    fun updatePageStatus(currentPage: Int, totalPages: Int) {
        // currentPage를 그대로 사용 (1부터 시작), totalPages에서 1을 빼서 커버 제외
        statusText.text = "page $currentPage/${totalPages - 1}"

        // Show/hide buttons based on current page
        prevButton.visibility = if (currentPage > 1) VISIBLE else INVISIBLE
        nextButton.visibility = if (currentPage < totalPages - 1) VISIBLE else INVISIBLE
    }
}
