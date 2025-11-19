package com.example.storybridge_android.ui.common

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import com.example.storybridge_android.ui.setting.AppSettings
import java.util.Locale

open class BaseActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        val langCode = AppSettings.getLanguage(newBase, "en")
        val locale = when (langCode) {
            "zh" -> Locale.CHINESE
            "vi" -> Locale("vi")
            else -> Locale.ENGLISH
        }

        Locale.setDefault(locale)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)

        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
    }
}