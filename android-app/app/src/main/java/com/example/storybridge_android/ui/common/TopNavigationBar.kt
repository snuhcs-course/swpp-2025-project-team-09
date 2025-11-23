package com.example.storybridge_android.ui.common

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.storybridge_android.R

class TopNavigationBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    init {
        LayoutInflater.from(context).inflate(R.layout.top_navigation_bar, this, true)
        findViewById<ImageView>(R.id.navbarSettingsButton)

    }

    fun setOnSettingsClickListener(listener: () -> Unit) {
        findViewById<ImageView>(R.id.navbarSettingsButton).setOnClickListener { listener() }
    }
}