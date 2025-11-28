package com.example.storybridge_android.ui.setting

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object AppSettings {

    private const val PREFS_NAME = "AppSettings"
    private const val KEY_LANGUAGE = "language"
    private const val KEY_VOICE = "voice"

    private fun prefs(context: Context): SharedPreferences {
        val ctx = context.applicationContext ?: context
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun setLanguage(context: Context, languageCode: String) {
        prefs(context).edit().putString(KEY_LANGUAGE, languageCode).commit()
    }

    fun getLanguage(context: Context, default: String = "en"): String =
        prefs(context).getString(KEY_LANGUAGE, default) ?: default

    fun setVoice(context: Context, voiceType: String) {
        prefs(context).edit {
            putString(KEY_VOICE, voiceType)
        }
    }

    fun clearAll(context: Context) {
        prefs(context).edit {
            clear()
        }
    }
}