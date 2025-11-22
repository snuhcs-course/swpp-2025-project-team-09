package com.example.storybridge_android.ui.session

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
import com.example.storybridge_android.R
import com.example.storybridge_android.data.SessionRepositoryImpl
import com.example.storybridge_android.ui.setting.AppSettings
import kotlinx.coroutines.flow.collectLatest

import kotlinx.coroutines.launch
import android.widget.FrameLayout
import com.example.storybridge_android.ui.common.BaseActivity

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
            Toast.makeText(this, "Session error. Please try again.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

<<<<<<< HEAD
        // 백그라운드에서 cover 이미지 업로드 시작 -> 주석처리
       // if (imagePath != null && lang != null) {
        //    viewModel.uploadCoverInBackground(sessionId!!, lang!!, imagePath!!)
        //}
=======
        val exitPanel = findViewById<FrameLayout>(R.id.exitPanelInclude)
        val exitConfirm = findViewById<Button>(R.id.exitConfirmBtn)
        val exitCancel = findViewById<Button>(R.id.exitCancelBtn)

        // 백그라운드에서 cover 이미지 업로드 시작
        if (imagePath != null && lang != null) {
            viewModel.uploadCoverInBackground(sessionId!!, lang!!, imagePath!!)
        }
>>>>>>> db-change

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

        lifecycleScope.launchWhenStarted {
            viewModel.success.collectLatest {
                Log.d("VoiceSelectActivity", "✓ Voice selection saved successfully")
                // API 완료 후 Next 버튼 활성화
                nextButton.isEnabled = true
                // 커버 이미지 업로드 이 때 실행
                if (imagePath != null && lang != null) {
                    viewModel.uploadCoverInBackground(sessionId!!, lang!!, imagePath!!)
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.error.collectLatest { msg ->
                Toast.makeText(this@VoiceSelectActivity, msg, Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(this@VoiceSelectActivity, "Failed to discard session", Toast.LENGTH_SHORT).show()
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
