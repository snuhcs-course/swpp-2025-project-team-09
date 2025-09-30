package com.example.storybridge_android

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ReadingActivity : AppCompatActivity() {

    private lateinit var mainLayout: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_reading)
        mainLayout = findViewById(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        addOverlayBox(400, 700, 300, 150, "번역된 텍스트를 인풋으로 넘기면 오버레이 박스 안에 등장")
    }

    private fun addOverlayBox(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        text: String
    ) {
        // 박스를 담을 FrameLayout 생성
        val boxLayout = FrameLayout(this)
        val boxParams = FrameLayout.LayoutParams(width, height)
        boxParams.leftMargin = x
        boxParams.topMargin = y

        // 반투명 검은색 배경
        boxLayout.setBackgroundColor(getColor(R.color.black_50)) // R.color.semi_transparent_black: #80000000

        // 텍스트 뷰 생성
        val textView = TextView(this)
        textView.text = text
        textView.setTextColor(getColor(R.color.white)) // 흰색 텍스트
        textView.textSize = 16f
        textView.gravity = Gravity.CENTER

        // 텍스트 뷰를 박스 안에 맞추기
        val textParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        textView.layoutParams = textParams

        // FrameLayout 안에 TextView 추가
        boxLayout.addView(textView)

        // 메인 레이아웃에 박스 추가
        mainLayout.addView(boxLayout, boxParams)
    }

}