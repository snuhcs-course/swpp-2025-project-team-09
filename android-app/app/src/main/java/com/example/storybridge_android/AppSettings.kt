package com.example.storybridge_android

import android.content.Context
import android.content.SharedPreferences

/**
 * AppSettings
 * -------------
 * 앱 전체에서 공통으로 사용하는 설정 저장소 (언어, 목소리 등)
 * SharedPreferences 기반의 전역 싱글톤
 */
import androidx.core.content.edit

object AppSettings {

    private const val PREFS_NAME = "AppSettings"
    private const val KEY_LANGUAGE = "language"
    private const val KEY_VOICE = "voice"
    private const val KEY_FIRST_RUN = "isFirstRun"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun setLanguage(context: Context, languageCode: String) {
        prefs(context).edit {
            putString(KEY_LANGUAGE, languageCode)
        }
    }

    fun getLanguage(context: Context, default: String = "en"): String =
        prefs(context).getString(KEY_LANGUAGE, default) ?: default

    fun setVoice(context: Context, voiceType: String) {
        prefs(context).edit {
            putString(KEY_VOICE, voiceType)
        }
    }

    fun getVoice(context: Context, default: String = "male"): String =
        prefs(context).getString(KEY_VOICE, default) ?: default

    fun isFirstRun(context: Context): Boolean =
        prefs(context).getBoolean(KEY_FIRST_RUN, true)

    fun setFirstRunDone(context: Context) {
        prefs(context).edit {
            putBoolean(KEY_FIRST_RUN, false)
        }
    }

    fun clearAll(context: Context) {
        prefs(context).edit {
            clear()
        }
    }
}