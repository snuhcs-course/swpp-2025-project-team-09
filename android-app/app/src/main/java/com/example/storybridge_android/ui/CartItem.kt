package com.example.storybridge_android.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.storybridge_android.R

class CardItem @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val bookImage: ImageView
    private val bookTitle: TextView
    private val bookProgress: TextView
    private val nextButton: ImageView

    init {
        LayoutInflater.from(context).inflate(R.layout.card_item, this, true)

        // 필요한 뷰만 참조
        bookImage = findViewById(R.id.bookImage)
        bookTitle = findViewById(R.id.bookTitle)
        bookProgress = findViewById(R.id.bookProgress)
        nextButton = findViewById(R.id.nextButton)
    }

    fun setBookImage(resId: Int) {
        bookImage.setImageResource(resId)
    }

    fun setBookTitle(title: String) {
        bookTitle.text = title
    }

    fun setBookProgress(progress: String) {
        bookProgress.text = progress
    }

    fun setOnNextClickListener(listener: () -> Unit) {
        nextButton.setOnClickListener { listener() }
    }
}
