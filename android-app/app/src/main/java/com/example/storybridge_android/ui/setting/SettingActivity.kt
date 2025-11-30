package com.example.storybridge_android.ui.setting

import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.storybridge_android.R
import com.example.storybridge_android.StoryBridgeApplication
import com.example.storybridge_android.network.UserLangRequest
import com.example.storybridge_android.ui.common.BaseActivity
import com.example.storybridge_android.ui.common.TopNavigationBar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SettingActivity : BaseActivity() {
    private val viewModel: SettingViewModel by viewModels {
        SettingViewModelFactory()
    }
    private lateinit var languageGroup: RadioGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_setting)

        setupTopBar()
        setupLanguageOptions()
        setupSaveButton()
        setupBackPressedHandler()
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
        val vietnamese = findViewById<RadioButton>(R.id.radioVietnamese)
        val currentLang = AppSettings.getLanguage(this)

        when (currentLang) {
            "en" -> english.isChecked = true
            "zh" -> chinese.isChecked = true
            "vi" -> vietnamese.isChecked = true
        }

        english.setOnClickListener {
            AppSettings.setLanguage(this, "en")
            recreate()
        }
        chinese.setOnClickListener {
            AppSettings.setLanguage(this, "zh")
            recreate()
        }
        vietnamese.setOnClickListener {
            AppSettings.setLanguage(this, "vi")
            recreate()
        }
    }

    private fun setupSaveButton() {
        val saveButton = findViewById<Button>(R.id.btnBack)
        saveButton.setOnClickListener {
            val deviceInfo = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
            val selectedLang = when (languageGroup.checkedRadioButtonId) {
                R.id.radioEnglish -> "en"
                R.id.radioChinese -> "zh"
                R.id.radioVietnamese -> "vi"
                else -> "en"
            }
            val request = UserLangRequest(device_info = deviceInfo, language_preference = selectedLang)
            viewModel.updateLanguage(request)
        }
    }

    private fun setupBackPressedHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                findViewById<Button>(R.id.btnBack).performClick()
            }
        })
    }

    private fun observeLangResponse() {
        lifecycleScope.launch {
            viewModel.langResponse.collectLatest { response ->
                if (response == null) return@collectLatest

                if (response.isSuccessful) {
                    val selectedLang = when (languageGroup.checkedRadioButtonId) {
                        R.id.radioEnglish -> "en"
                        R.id.radioChinese -> "zh"
                        R.id.radioVietnamese -> "vi"
                        else -> "en"
                    }

                    AppSettings.setLanguage(this@SettingActivity, selectedLang)
                    setResult(RESULT_OK)
                    finish()

                } else {
                    Log.e("SettingActivity", "PATCH failed: ${response.code()}")
                }
            }
        }
    }
}