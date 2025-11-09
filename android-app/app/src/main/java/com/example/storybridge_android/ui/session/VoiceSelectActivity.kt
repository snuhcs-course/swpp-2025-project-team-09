package com.example.storybridge_android.ui.session

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.storybridge_android.R
import com.example.storybridge_android.data.SessionRepositoryImpl
import com.example.storybridge_android.ui.setting.AppSettings
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

const val MALE_VOICE = "onyx"
const val FEMALE_VOICE = "shimmer"

class VoiceSelectActivity : AppCompatActivity() {

    private var sessionId: String? = null
    private var maleTts: String? = null
    private var femaleTts: String? = null
    private var bookTitle: String? = null

    private val viewModel: VoiceSelectViewModel by viewModels {
        VoiceSelectViewModelFactory(SessionRepositoryImpl())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_voice_select)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        sessionId = intent.getStringExtra("session_id")
        bookTitle = intent.getStringExtra("book_title") ?: "Unknown Book"
        maleTts = intent.getStringExtra("male_tts")
        femaleTts = intent.getStringExtra("female_tts")

        Log.d("VoiceSelectActivity", "session_id=$sessionId")
        Log.d("VoiceSelectActivity", "book_title=$bookTitle")
        Log.d("VoiceSelectActivity", "male_tts=${maleTts?.length}, female_tts=${femaleTts?.length}")

        if (sessionId == null) {
            Toast.makeText(this, "Session error. Please try again.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val manButton = findViewById<Button>(R.id.manButton)
        val womanButton = findViewById<Button>(R.id.womanButton)
        val nextButton = findViewById<Button>(R.id.nextButton)

        fun updateButtonState(selected: Button) {
            listOf(manButton, womanButton).forEach { it.isSelected = it == selected }
        }

        val currentLang = AppSettings.getLanguage(this)

        when (currentLang) {
            "en", "zh" -> {
                manButton.text = getString(R.string.male_voice)
                womanButton.text = getString(R.string.female_voice)
                nextButton.text = getString(R.string.next)
            }
            else -> {
                manButton.text = getString(R.string.male_voice)
                womanButton.text = getString(R.string.female_voice)
                nextButton.text = getString(R.string.next)
            }
        }

        nextButton.isEnabled = false

        manButton.setOnClickListener {
            AppSettings.setVoice(this, MALE_VOICE)
            viewModel.selectVoice(sessionId!!, MALE_VOICE)
            playTts(maleTts, MALE_VOICE)
            nextButton.isEnabled = true
            updateButtonState(manButton)
        }

        womanButton.setOnClickListener {
            AppSettings.setVoice(this, FEMALE_VOICE)
            viewModel.selectVoice(sessionId!!, FEMALE_VOICE)
            playTts(femaleTts, FEMALE_VOICE)
            nextButton.isEnabled = true
            updateButtonState(womanButton)
        }

        nextButton.setOnClickListener {
            goToContentInstruction()
        }

        lifecycleScope.launchWhenStarted {
            viewModel.success.collectLatest {
                Log.d("VoiceSelectActivity", "✓ Voice selection saved successfully")
                nextButton.isEnabled = true
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.error.collectLatest { msg ->
                Toast.makeText(this@VoiceSelectActivity, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun playTts(ttsBase64: String?, voice: String) {
        if (ttsBase64.isNullOrEmpty()) {
            Log.w("VoiceSelectActivity", "No TTS data for $voice, skipping playback.")
            Toast.makeText(this, "$voice voice preview not available", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val audioBytes = Base64.decode(ttsBase64, Base64.DEFAULT)
            val tempFile = File.createTempFile("tts_preview", ".mp3", cacheDir)
            FileOutputStream(tempFile).use { it.write(audioBytes) }

            val player = MediaPlayer()
            player.setDataSource(tempFile.absolutePath)
            player.prepare()
            player.start()
            Log.d("VoiceSelectActivity", "▶ Playing $voice voice preview")

            player.setOnCompletionListener {
                player.release()
                tempFile.delete()
                Log.d("VoiceSelectActivity", "✓ Playback finished")
            }
        } catch (e: Exception) {
            Log.e("VoiceSelectActivity", "✗ Error playing TTS: ${e.message}", e)
            Toast.makeText(this, "Failed to play audio preview", Toast.LENGTH_SHORT).show()
        }
    }

    private fun goToContentInstruction() {
        val intent = Intent(this, ContentInstructionActivity::class.java).apply {
            putExtra("session_id", sessionId)
        }
        startActivity(intent)
        finish()
    }
}
