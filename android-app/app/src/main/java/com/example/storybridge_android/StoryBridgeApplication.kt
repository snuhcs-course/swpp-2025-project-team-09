package com.example.storybridge_android

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import com.example.storybridge_android.ui.setting.AppSettings
import java.util.Locale

class StoryBridgeApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Apply saved language on app start
        applyLanguage(this)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Reapply language when configuration changes
        applyLanguage(this)
    }

    companion object {
        /**
         * Apply the saved language setting to the entire app.
         * This should be called in Application.onCreate() and whenever language changes.
         */
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
