package com.example.storybridge_android

import android.os.Bundle
import android.widget.RadioButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.storybridge_android.ui.TopNavigationBar

class SettingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_setting)

        setupTopBar()
        setupLanguageOptions()
        setupVoiceOptions()
    }

    private fun navigateToMain() {
        //TODO: MainActivity로 이동하는 로직 구현
    }

    private fun setLangPreference() {
        //TODO: 유저의 language preference 업데이트
    }


    // 아래 함수는 적당히 수정 부탁드립니다.
    // 사실 top navigation이 그닥 재사용이 안 될 것 같아서 걍 없애도 될 것 같아요


    private fun setupTopBar() {
        val topBar = findViewById<TopNavigationBar>(R.id.topNavigationBar)

        topBar.setOnSettingsClickListener {
            // SettingsActivity 안에선 굳이 다른 Settings를 열 필요가 없음
            finish() // or Toast.makeText(this, "Already in settings", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupLanguageOptions() {
        val english = findViewById<RadioButton>(R.id.radioEnglish)
        val vietnamese = findViewById<RadioButton>(R.id.radioVietnamese)

        // 현재 선택된 언어 불러오기
        val currentLang = AppSettings.getLanguage(this)
        when (currentLang) {
            "en" -> english.isChecked = true
            "vi" -> vietnamese.isChecked = true
        }

        english.setOnClickListener {
            AppSettings.setLanguage(this, "en")
        }
        vietnamese.setOnClickListener {
            AppSettings.setLanguage(this, "vi")
        }
    }

    private fun setupVoiceOptions() {
        val male = findViewById<RadioButton>(R.id.radioMan)
        val female = findViewById<RadioButton>(R.id.radioWoman)

        val currentVoice = AppSettings.getVoice(this)
        when (currentVoice) {
            "MAN" -> male.isChecked = true
            "WOMAN" -> female.isChecked = true
        }

        male.setOnClickListener {
            AppSettings.setVoice(this, "MAN")
        }
        female.setOnClickListener {
            AppSettings.setVoice(this, "WOMAN")
        }
    }
}
