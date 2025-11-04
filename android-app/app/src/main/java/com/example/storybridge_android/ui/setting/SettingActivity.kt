package com.example.storybridge_android.ui.setting

import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.storybridge_android.R
import com.example.storybridge_android.data.UserRepositoryImpl
import com.example.storybridge_android.network.UserLangRequest
import com.example.storybridge_android.ui.common.TopNavigationBar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SettingActivity : AppCompatActivity() {
    private lateinit var languageGroup: RadioGroup
    private lateinit var voiceGroup: RadioGroup

    private val viewModel: SettingViewModel by viewModels {
        SettingViewModelFactory(UserRepositoryImpl())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_setting)

        setupTopBar()
        setupLanguageOptions()
        setupVoiceOptions()
        setupSaveButton()
        observeLangResponse()
    }

    private fun setupTopBar() {
        val topBar = findViewById<TopNavigationBar>(R.id.topNavigationBar)
        topBar.setOnSettingsClickListener { finish() }
    }

    private fun setupLanguageOptions() {
        languageGroup = findViewById(R.id.languageGroup)
        val english = findViewById<RadioButton>(R.id.radioEnglish)
        val chinese = findViewById<RadioButton>(R.id.radioChinese)
        val currentLang = AppSettings.getLanguage(this)

        when (currentLang) {
            "en" -> english.isChecked = true
            "cn" -> chinese.isChecked = true
        }

        english.setOnClickListener { AppSettings.setLanguage(this, "en") }
        chinese.setOnClickListener { AppSettings.setLanguage(this, "cn") }
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
            val deviceInfo = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
            val selectedLang = when (languageGroup.checkedRadioButtonId) {
                R.id.radioEnglish -> "en"
                R.id.radioChinese -> "cn"
                else -> "en"
            }
            val request = UserLangRequest(device_info = deviceInfo, language_preference = selectedLang)
            viewModel.updateLanguage(request)
        }
    }

    private fun observeLangResponse() {
        lifecycleScope.launch {
            viewModel.langResponse.collectLatest { response ->
                if (response == null) return@collectLatest
                if (response.isSuccessful) {
                    val selectedLang = when (languageGroup.checkedRadioButtonId) {
                        R.id.radioEnglish -> "en"
                        R.id.radioChinese -> "cn"
                        else -> "en"
                    }
                    AppSettings.setLanguage(this@SettingActivity, selectedLang)
                    finish()
                } else {
                    Log.e("SettingActivity", "PATCH failed: ${response.code()}")
                }
            }
        }
    }
}
