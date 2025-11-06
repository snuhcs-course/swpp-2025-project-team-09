package com.example.storybridge_android.ui.common

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.storybridge_android.R
import android.widget.ImageButton
import android.widget.Button

class TopNav @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var onMenuButtonClickListener: (() -> Unit)? = null
    private var onFinishButtonClickListener: (() -> Unit)? = null

    init {

        LayoutInflater.from(context).inflate(R.layout.top_nav, this, true)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        findViewById<ImageButton>(R.id.menuButton).setOnClickListener {
            onMenuButtonClickListener?.invoke()
        }

        findViewById<Button>(R.id.finishButton).setOnClickListener {
            onFinishButtonClickListener?.invoke()
        }
    }

    fun setOnMenuButtonClickListener(listener: () -> Unit) {
        onMenuButtonClickListener = listener
    }

    fun setOnFinishButtonClickListener(listener: () -> Unit) {
        onFinishButtonClickListener = listener
    }
}
