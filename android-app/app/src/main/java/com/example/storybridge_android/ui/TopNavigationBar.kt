package com.example.storybridge_android.ui

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import android.widget.ImageView
import android.widget.TextView
import com.example.storybridge_android.R
import com.example.storybridge_android.SettingActivity


class TopNavigationBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    init {
        LayoutInflater.from(context).inflate(R.layout.top_navigation_bar, this, true)

        // Settings 클릭 이벤트
        findViewById<ImageView>(R.id.settingsIcon).setOnClickListener {
            // SettingActivity로 이동
            // context.startActivity(Intent(context, SettingActivity::class.java))
        }
    }

    fun setOnSettingsClickListener(listener: () -> Unit) {
        findViewById<ImageView>(R.id.settingsIcon).setOnClickListener { listener() }
    }
}