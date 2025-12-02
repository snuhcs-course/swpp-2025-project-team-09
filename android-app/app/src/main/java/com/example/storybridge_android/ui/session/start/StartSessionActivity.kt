package com.example.storybridge_android.ui.session.start

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import com.example.storybridge_android.R
import com.example.storybridge_android.ui.camera.CameraSessionActivity
import com.example.storybridge_android.ui.common.BaseActivity

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
}
