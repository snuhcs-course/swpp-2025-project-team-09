package com.example.storybridge_android.ui.session

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.storybridge_android.R
import com.example.storybridge_android.ui.camera.CameraSessionActivity
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog

class ContentInstructionActivity : AppCompatActivity() {

    private val viewModel: ContentInstructionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_content_instruction)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.contentInstructionMain)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val sessionId = intent.getStringExtra("session_id")
        if (sessionId == null) {
            Toast.makeText(this, "Session error. Please try again.", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        viewModel.setSessionId(sessionId)

        viewModel.navigateToCamera.observe(this) { shouldNavigate ->
            if (shouldNavigate == true) {
                goToCamera(sessionId)
            }
        }

        findViewById<android.widget.Button>(R.id.contentInstructionButton).setOnClickListener {
            viewModel.onStartClicked()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                AlertDialog.Builder(this@ContentInstructionActivity)
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

    private fun goToCamera(sessionId: String) {
        val intent = Intent(this, CameraSessionActivity::class.java).apply {
            putExtra("session_id", sessionId)
            // 본문 페이지 인덱스 1부터 시작
            putExtra("page_index", 1)
        }
        startActivity(intent)
        finish()
    }
}