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
        // 처음 앱을 실행했을 때 landing 화면에서 voice preference까지 선택하도록 해야 해서
        // landinglayout의 내용물을 toggle 하는 방식으로 구현해 놨습니다.
        landingLayout = findViewById(R.id.landing)

        //TODO: 유저가 앱을 처음 열었는지 아닌지 확인해서 처음이라면 언어 설정.

        // 3초 후 Language Selection 화면으로 전환
        Handler(Looper.getMainLooper()).postDelayed({
            showLanguageSelection()
        }, 1000)
    }


    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }


    private fun checkIsRegistered() {
        //TODO: 유저가 앱을 처음 열었는지 아닌지 확인
    }

    private fun setLangPreference() {
        //TODO: 앱을 처음 열었더라면 언어 설정 + 적절한 화면 띄우기
        //TODO: 이거 구현하고 나면 아래 showLanguageSelection은 지워 주세요
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

}