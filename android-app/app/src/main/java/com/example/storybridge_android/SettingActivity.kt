package com.example.storybridge_android

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.storybridge_android.network.RetrofitClient
import com.example.storybridge_android.network.UserLangRequest
import com.example.storybridge_android.network.UserLangResponse
import com.example.storybridge_android.ui.TopNavigationBar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SettingActivity : AppCompatActivity() {

    private lateinit var languageGroup: RadioGroup
    private lateinit var voiceGroup: RadioGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_setting)

        setupTopBar()
        setupLanguageOptions()
        setupVoiceOptions()
        setupSaveButton()
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }

    private fun setupTopBar() {
        val topBar = findViewById<TopNavigationBar>(R.id.topNavigationBar)
        topBar.setOnSettingsClickListener { finish() }
    }

    private fun setupLanguageOptions() {
        languageGroup = findViewById(R.id.languageGroup)
        val english = findViewById<RadioButton>(R.id.radioEnglish)
        val Chinese = findViewById<RadioButton>(R.id.radioChinese)

        val currentLang = AppSettings.getLanguage(this)
        when (currentLang) {
            "en" -> english.isChecked = true
            "cn" -> Chinese.isChecked = true
        }

        english.setOnClickListener { AppSettings.setLanguage(this, "en") }
        Chinese.setOnClickListener { AppSettings.setLanguage(this, "cn") }
    }

    private fun setupVoiceOptions() {
        voiceGroup = findViewById(R.id.voiceGroup)
        val male = findViewById<RadioButton>(R.id.radioMan)
        val female = findViewById<RadioButton>(R.id.radioWoman)

        val currentVoice = AppSettings.getVoice(this)
        when (currentVoice) {
            "MAN" -> male.isChecked = true
            "WOMAN" -> female.isChecked = true
        }

        male.setOnClickListener { AppSettings.setVoice(this, "MAN") }
        female.setOnClickListener { AppSettings.setVoice(this, "WOMAN") }
    }

    private fun setupSaveButton() {
        val saveButton = findViewById<Button>(R.id.btnSaveSettings)

        saveButton.setOnClickListener {
            val deviceInfo = android.provider.Settings.Secure.getString(
                contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            )

            // 선택된 언어 확인
            val selectedLang = when (findViewById<RadioGroup>(R.id.languageGroup).checkedRadioButtonId) {
                R.id.radioEnglish -> "en"
                R.id.radioChinese -> "cn"
                else -> "en"
            }

            // 요청 바디 생성
            val request = UserLangRequest(
                device_info = deviceInfo,
                language_preference = selectedLang
            )

            // PATCH 요청
            val call = RetrofitClient.userApi.userLang(request)
            call.enqueue(object : Callback<UserLangResponse> {
                override fun onResponse(
                    call: Call<UserLangResponse>,
                    response: Response<UserLangResponse>
                ) {
                    if (response.isSuccessful) {
                        // 로컬에도 반영
                        AppSettings.setLanguage(this@SettingActivity, selectedLang)
                        finish() // 설정 저장 후 종료
                    } else {
                        Log.e("SettingActivity", "PATCH failed: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<UserLangResponse>, t: Throwable) {
                    Log.e("SettingActivity", "API error: ${t.message}")
                }
            })
        }
    }
}
