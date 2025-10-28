package com.example.storybridge_android.ui.landing

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.storybridge_android.ui.setting.AppSettings
import com.example.storybridge_android.ui.main.MainActivity
import com.example.storybridge_android.R
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LandingActivity : AppCompatActivity() {
    private val viewModel: LandingViewModel by viewModels {
        LandingViewModelFactory()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        findViewById<Button>(R.id.btnEnglish).setOnClickListener {
            AppSettings.setLanguage(this, "en")
            navigateToMain()
        }
        findViewById<Button>(R.id.btnChinese).setOnClickListener {
            AppSettings.setLanguage(this, "ch")
            navigateToMain()
        }
    }

    private fun showError(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}