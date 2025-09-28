package com.example.storybridge_android

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

import android.widget.Button
import android.content.Intent
import android.os.Handler
import android.os.Looper

class LandingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_landing)

        // 시스템 바 padding 적용
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 언어 선택 버튼
        val btnEnglish = findViewById<Button>(R.id.btnEnglish)
        val btnVietnamese = findViewById<Button>(R.id.btnVietnamese)
        btnEnglish.setOnClickListener { navigateToMain() }
        btnVietnamese.setOnClickListener { navigateToMain() }

        // 3초 후 버튼 표시
        Handler(Looper.getMainLooper()).postDelayed({
            btnEnglish.visibility = Button.VISIBLE
            btnVietnamese.visibility = Button.VISIBLE
        }, 3000) //
    }

    // 버튼 클릭 시 MainActivity로 이동
    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}