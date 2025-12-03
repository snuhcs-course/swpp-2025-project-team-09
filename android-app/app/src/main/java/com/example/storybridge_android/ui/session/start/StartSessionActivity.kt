package com.example.storybridge_android.ui.session.start

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.OnBackPressedCallback
import com.example.storybridge_android.R
import com.example.storybridge_android.ui.camera.CameraSessionActivity
import com.example.storybridge_android.ui.common.BaseActivity
import com.example.storybridge_android.ui.session.voice.VoiceSelectActivity
import com.example.storybridge_android.ui.setting.AppSettings

class StartSessionActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_session)

        val sessionId = intent.getStringExtra("session_id")
        if (sessionId == null) {
            finish()
            return
        }

        val startButton = findViewById<Button>(R.id.startSessionButton)
        startButton.setOnClickListener {
            navigateToCameraForCover(sessionId)
        }

        // Handle back button press to go back to voice selection
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateBackToVoiceSelection(sessionId)
            }
        })
    }

    private fun navigateToCameraForCover(sessionId: String) {
        val intent = Intent(this, CameraSessionActivity::class.java).apply {
            putExtra("session_id", sessionId)
            putExtra("page_index", 0)
            putExtra("is_cover", true)
        }
        startActivity(intent)
        finish()
    }

    private fun navigateBackToVoiceSelection(sessionId: String) {
        val intent = Intent(this, VoiceSelectActivity::class.java).apply {
            putExtra("session_id", sessionId)
            putExtra("lang", AppSettings.getLanguage(this@StartSessionActivity))
        }
        startActivity(intent)
        finish()
    }
}