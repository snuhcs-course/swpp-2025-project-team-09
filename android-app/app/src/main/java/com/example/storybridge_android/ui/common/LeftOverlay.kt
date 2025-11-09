package com.example.storybridge_android.ui.common

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.example.storybridge_android.R

class LeftOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    val thumbnailRecyclerView: RecyclerView

    init {
        LayoutInflater.from(context).inflate(R.layout.left_panel, this, true)
        thumbnailRecyclerView = findViewById(R.id.thumbnailRecyclerView)
    }
}
