package com.example.storybridge_android.util

import android.content.Context
import android.content.SharedPreferences
import com.example.storybridge_android.ui.setting.AppSettings
import io.mockk.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class AppSettingsTest {
    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    @Before
    fun setup() {
        context = mockk()
        prefs = mockk()
        editor = mockk()

        every { context.applicationContext } returns context
        every { context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE) } returns prefs
        every { prefs.edit() } returns editor

        every { editor.putString(any(), any()) } returns editor
        every { editor.clear() } returns editor
        every { editor.apply() } just Runs
        every { editor.commit() } returns true

        // getString default behavior: return null unless specified
        every { prefs.getString(any(), any()) } answers { secondArg() }
    }

    @After
    fun tearDown() {
        // No cleanup needed
    }

    @Test
    fun setLanguage_savesCorrectValue() {
        AppSettings.setLanguage(context, "zh")

        verify { editor.putString("language", "zh") }
        verify { editor.commit() }
    }

    @Test
    fun setLanguage_savesVietnamese() {
        AppSettings.setLanguage(context, "vi")

        verify { editor.putString("language", "vi") }
        verify { editor.commit() }
    }

    @Test
    fun getLanguage_returnsValueFromPrefs() {
        every { prefs.getString("language", "en") } returns "zh"

        val lang = AppSettings.getLanguage(context)

        assertEquals("zh", lang)
    }

    @Test
    fun getLanguage_returnsVietnamese() {
        every { prefs.getString("language", "en") } returns "vi"

        val lang = AppSettings.getLanguage(context)

        assertEquals("vi", lang)
    }

    @Test
    fun setVoice_savesCorrectValue() {
        AppSettings.setVoice(context, "WOMAN")

        verify { editor.putString("voice", "WOMAN") }
        verify { editor.apply() }
    }

    @Test
    fun clearAll_clearsPrefs() {
        AppSettings.clearAll(context)

        verify { editor.clear() }
        verify { editor.apply() }
    }
}