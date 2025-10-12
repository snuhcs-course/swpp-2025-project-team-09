package com.example.storybridge_android

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class LandingActivity : AppCompatActivity() {

    private lateinit var landingLayout: ConstraintLayout
    private lateinit var languageLayout: ConstraintLayout
    private lateinit var btnEnglish: Button
    private lateinit var btnVietnamese: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_landing_first)

        // 시스템 바 padding 적용
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.landing)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Landing 화면 참조 (뒤에 어디에 사용되나요?)
        landingLayout = findViewById(R.id.landing)

        // 3초 후 Language Selection 화면으로 전환
        Handler(Looper.getMainLooper()).postDelayed({
            showLanguageSelection()
        }, 1000)
    }

    private fun showLanguageSelection() {
        // Language Selection 레이아웃으로 전환
        setContentView(R.layout.activity_landing_second)

        languageLayout = findViewById(R.id.language_se)
        btnEnglish = findViewById(R.id.btnEnglish)
        btnVietnamese = findViewById(R.id.btnVietnamese)

        btnEnglish.setOnClickListener {
            AppSettings.setLanguage(this, "en")
            navigateToMain()
        }
        btnVietnamese.setOnClickListener {
            AppSettings.setLanguage(this, "vi")
            navigateToMain()
        }
    }


    // 버튼 클릭 시 MainActivity로 이동
    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}