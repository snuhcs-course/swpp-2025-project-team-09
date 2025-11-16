package com.example.storybridge_android

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import com.example.storybridge_android.ui.setting.AppSettings
import java.util.Locale

class StoryBridgeApplication : Application() {

    override fun attachBaseContext(base: Context?) {
        val context = base?.let {
            val lang = AppSettings.getLanguage(it, "en")
            val locale = Locale(lang)
            val config = Configuration()
            config.setLocale(locale)
            it.createConfigurationContext(config)
        }
        super.attachBaseContext(context)
    }

    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        Log.d("App", "StoryBridgeApplication started")
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    companion object {
        fun applyLanguage(context: Context) {
            val languageCode = AppSettings.getLanguage(context, "en")
            val locale = Locale(languageCode)
            Locale.setDefault(locale)
            val config = Configuration(context.resources.configuration)
            config.setLocale(locale)
            config.setLayoutDirection(locale)
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
        }
    }
}
