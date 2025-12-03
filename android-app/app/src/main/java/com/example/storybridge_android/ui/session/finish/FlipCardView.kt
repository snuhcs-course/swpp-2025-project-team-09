package com.example.storybridge_android.ui.session.finish

import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import com.example.storybridge_android.R
import com.example.storybridge_android.databinding.ViewFlipCardBinding

class FlipCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val binding = ViewFlipCardBinding.inflate(LayoutInflater.from(context), this)
    private var isFront = true
    var onFlipped: (() -> Unit)? = null

    init {
        setOnClickListener { flip() }
    }

    fun setData(english: String, korean: String) {
        binding.textFront.text = english
        binding.textBack.text = korean
    }

    private fun flip() {
        if (isFront) {
            binding.cardFront.visibility = View.GONE
            binding.cardBack.visibility = View.VISIBLE
        } else {
            binding.cardBack.visibility = View.GONE
            binding.cardFront.visibility = View.VISIBLE
        }

        isFront = !isFront

        if (!isFront) {
            onFlipped?.invoke()   // 처음 뒤집힐 때만 콜백
        }
    }
}