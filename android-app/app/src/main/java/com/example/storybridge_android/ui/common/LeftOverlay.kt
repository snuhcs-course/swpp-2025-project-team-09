package com.example.storybridge_android.ui.common

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.storybridge_android.R
import android.widget.Button
import android.widget.LinearLayout

class LeftOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    var onCloseButtonClick: (() -> Unit)? = null
    private val pageListContainer: LinearLayout

    init {
        LayoutInflater.from(context).inflate(R.layout.left_panel, this, true)
        pageListContainer = findViewById(R.id.pageListContainer)
        setupClickListeners()
    }

    private fun setupClickListeners() {
        val closeButton = findViewById<Button>(R.id.closeOverlayButton)
        closeButton.setOnClickListener {
            onCloseButtonClick?.invoke()
        }
    }

    fun setOnCloseButtonClickListener(listener: () -> Unit) {
        onCloseButtonClick = listener
    }
}
