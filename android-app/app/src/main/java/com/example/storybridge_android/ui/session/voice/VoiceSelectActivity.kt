package com.example.storybridge_android.ui.session.voice

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import com.example.storybridge_android.data.MALE_VOICE
import com.example.storybridge_android.data.FEMALE_VOICE
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import com.example.storybridge_android.R
import com.example.storybridge_android.data.SessionRepositoryImpl
import com.example.storybridge_android.ui.setting.AppSettings
import kotlinx.coroutines.flow.collectLatest

import kotlinx.coroutines.launch
import android.widget.FrameLayout
import com.example.storybridge_android.ui.common.BaseActivity
import com.example.storybridge_android.ui.session.instruction.ContentInstructionActivity

class VoiceSelectActivity : BaseActivity() {

    private var sessionId: String? = null
    private var imagePath: String? = null
    private var lang: String? = null
    private var mediaPlayer: MediaPlayer? = null

    private val viewModel: VoiceSelectViewModel by viewModels {
        VoiceSelectViewModelFactory()
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
            Toast.makeText(
                this,
                getString(R.string.error_session_invalid),
                Toast.LENGTH_LONG
            ).show()

            finish()
            return
        }

        val exitPanel = findViewById<FrameLayout>(R.id.exitPanelInclude)
        val exitConfirm = findViewById<Button>(R.id.exitConfirmBtn)
        val exitCancel = findViewById<Button>(R.id.exitCancelBtn)

        val manButton = findViewById<Button>(R.id.manButton)
        val womanButton = findViewById<Button>(R.id.womanButton)
        val nextButton = findViewById<Button>(R.id.nextButton)

        fun updateButtonState(selected: Button) {
            listOf(manButton, womanButton).forEach { it.isSelected = it == selected }
        }

        nextButton.isEnabled = false

        manButton.setOnClickListener {
            AppSettings.setVoice(this, MALE_VOICE)
            viewModel.selectVoice(sessionId!!, MALE_VOICE)
            playLocalAudio(getVoiceResourceId(MALE_VOICE))
            updateButtonState(manButton)
        }

        womanButton.setOnClickListener {
            AppSettings.setVoice(this, FEMALE_VOICE)
            viewModel.selectVoice(sessionId!!, FEMALE_VOICE)
            playLocalAudio(getVoiceResourceId(FEMALE_VOICE))
            updateButtonState(womanButton)
        }

        nextButton.setOnClickListener {
            goToContentInstruction()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.success.collectLatest {
                    Log.d("VoiceSelectActivity", "✓ Voice selection saved successfully")
                    nextButton.isEnabled = true
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.error.collectLatest { msg ->
                    Toast.makeText(
                        this@VoiceSelectActivity,
                        getString(R.string.error_generic, msg),
                        Toast.LENGTH_SHORT
                    ).show()

                }
            }
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                exitPanel.visibility = View.VISIBLE
            }
        })
        exitConfirm.setOnClickListener {
            val id = sessionId ?: return@setOnClickListener

            lifecycleScope.launch {
                val result = SessionRepositoryImpl().discardSession(id)
                result.onSuccess {
                    finish()
                }.onFailure {
                    Toast.makeText(
                        this@VoiceSelectActivity,
                        getString(R.string.error_discard_session_failed),
                        Toast.LENGTH_SHORT
                    ).show()

                    finish()
                }
            }
        }
        exitCancel.setOnClickListener {
            exitPanel.visibility = View.GONE
        }

    }

    private fun getVoiceResourceId(voiceType: String): Int {
        val language = AppSettings.getLanguage(this, "en")
        return when (language) {
            "zh" -> if (voiceType == MALE_VOICE) R.raw.zh_man else R.raw.zh_woman
            "vi" -> if (voiceType == MALE_VOICE) R.raw.vn_man else R.raw.vn_woman
            else -> if (voiceType == MALE_VOICE) R.raw.en_man else R.raw.en_woman
        }
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
            Toast.makeText(
                this,
                getString(R.string.error_audio_preview_failed),
                Toast.LENGTH_SHORT
            ).show()

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
