package com.example.storybridge_android.ui.session

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
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
    private var imagePath: String? = null
    private var lang: String? = null
    private var mediaPlayer: MediaPlayer? = null

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
        imagePath = intent.getStringExtra("image_path")
        lang = intent.getStringExtra("lang")

        Log.d("VoiceSelectActivity", "session_id=$sessionId")
        Log.d("VoiceSelectActivity", "image_path=$imagePath")
        Log.d("VoiceSelectActivity", "lang=$lang")

        if (sessionId == null) {
            Toast.makeText(this, "Session error. Please try again.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // 백그라운드에서 cover 이미지 업로드 시작
        if (imagePath != null && lang != null) {
            viewModel.uploadCoverInBackground(sessionId!!, lang!!, imagePath!!)
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
            playLocalAudio(R.raw.voice_man)
            updateButtonState(manButton)
        }

        womanButton.setOnClickListener {
            AppSettings.setVoice(this, FEMALE_VOICE)
            viewModel.selectVoice(sessionId!!, FEMALE_VOICE)
            playLocalAudio(R.raw.voice_woman)
            updateButtonState(womanButton)
        }

        nextButton.setOnClickListener {
            goToContentInstruction()
        }

        lifecycleScope.launchWhenStarted {
            viewModel.success.collectLatest {
                Log.d("VoiceSelectActivity", "✓ Voice selection saved successfully")
                // API 완료 후 Next 버튼 활성화
                nextButton.isEnabled = true
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.error.collectLatest { msg ->
                Toast.makeText(this@VoiceSelectActivity, msg, Toast.LENGTH_SHORT).show()
            }
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                AlertDialog.Builder(this@VoiceSelectActivity)
                    .setTitle(getString(R.string.exit_dialog_title))
                    .setMessage(getString(R.string.exit_dialog_message))
                    .setPositiveButton(getString(R.string.exit_dialog_confirm)) { _, _ ->
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                    .setNegativeButton(getString(R.string.exit_dialog_cancel), null)
                    .show()
            }
        })
    }

    private fun playLocalAudio(audioResId: Int) {
        try {
            // Release any existing player
            mediaPlayer?.release()

            // Create new MediaPlayer with local resource
            mediaPlayer = MediaPlayer.create(this, audioResId)
            mediaPlayer?.start()
            Log.d("VoiceSelectActivity", "▶ Playing local voice preview")

            mediaPlayer?.setOnCompletionListener {
                it.release()
                mediaPlayer = null
                Log.d("VoiceSelectActivity", "✓ Playback finished")
            }
        } catch (e: Exception) {
            Log.e("VoiceSelectActivity", "✗ Error playing audio: ${e.message}", e)
            Toast.makeText(this, "Failed to play audio preview", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun goToContentInstruction() {
        val intent = Intent(this, ContentInstructionActivity::class.java).apply {
            putExtra("session_id", sessionId)
        }
        startActivity(intent)
        finish()
    }
}
