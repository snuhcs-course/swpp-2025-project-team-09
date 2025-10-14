package com.example.storybridge_android.ui

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
        // Settings 클릭 이벤트
        // UI 이벤트만 담당 Intent는 MainActivity에서 처리
        findViewById<ImageView>(R.id.settingsIcon)

    }

    fun setOnSettingsClickListener(listener: () -> Unit) {
        findViewById<ImageView>(R.id.settingsIcon).setOnClickListener { listener() }
    }
}