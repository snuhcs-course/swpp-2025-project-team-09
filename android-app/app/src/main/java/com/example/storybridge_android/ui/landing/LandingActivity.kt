package com.example.storybridge_android.ui.landing

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.activity.viewModels
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.example.storybridge_android.ui.setting.AppSettings
import com.example.storybridge_android.ui.main.MainActivity
import com.example.storybridge_android.R
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.example.storybridge_android.ui.common.BaseActivity

class LandingActivity : BaseActivity() {
    private val viewModel: LandingViewModel by viewModels {
        LandingViewModelFactory()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_landing_first)

        val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)

        lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                when (state) {
                    is LandingUiState.Loading -> Unit
                    is LandingUiState.NavigateMain -> navigateToMain()
                    is LandingUiState.ShowLanguageSelect -> showLanguageSelection()
                    is LandingUiState.Error -> showError(state.message)
                }
            }
        }

        viewModel.checkUser(deviceId)
    }

    private fun showLanguageSelection() {
        setContentView(R.layout.activity_landing_second)
        var selectedLang: String? = null

        val btnEnglish = findViewById<Button>(R.id.btnEnglish)
        val btnChinese = findViewById<Button>(R.id.btnChinese)
        val btnVietnamese = findViewById<Button>(R.id.btnVietnamese)

        val buttons = listOf(btnEnglish, btnChinese, btnVietnamese)
        fun updateButtonState(selected: Button) {
            buttons.forEach { it.isSelected = it == selected }
        }

        btnEnglish.setOnClickListener {
            selectedLang = "en"
            AppSettings.setLanguage(this, "en")
            updateButtonState(btnEnglish)
        }

        btnChinese.setOnClickListener {
            selectedLang = "zh"
            AppSettings.setLanguage(this, "zh")
            updateButtonState(btnChinese)
        }

        btnVietnamese.setOnClickListener {
            selectedLang = "vi"
            AppSettings.setLanguage(this, "vi")
            updateButtonState(btnVietnamese)
        }

        findViewById<Button>(R.id.startButton).setOnClickListener {
            if (selectedLang != null) {
                navigateToTutorial()
            } else {
                Toast.makeText(this, getString(R.string.error_select_language), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showError(msg: String) {
        Toast.makeText(
            this,
            getString(R.string.error_generic, msg),
            Toast.LENGTH_LONG
        ).show()
    }


    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun navigateToTutorial() {
        startActivity(Intent(this, TutorialActivity::class.java))
        finish()
    }
}