package com.example.storybridge_android.ui.session

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.storybridge_android.R
import com.example.storybridge_android.ui.camera.CameraSessionActivity
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.lifecycleScope
import com.example.storybridge_android.data.SessionRepositoryImpl
import com.example.storybridge_android.ui.common.BaseActivity
import kotlinx.coroutines.launch

class ContentInstructionActivity : BaseActivity() {

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

        val exitPanel = findViewById<FrameLayout>(R.id.exitPanelInclude)
        val exitConfirmBtn = findViewById<Button>(R.id.exitConfirmBtn)
        val exitCancelBtn = findViewById<Button>(R.id.exitCancelBtn)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                exitPanel.visibility = View.VISIBLE
            }
        })

        exitCancelBtn.setOnClickListener {
            exitPanel.visibility = View.GONE
        }

        exitConfirmBtn.setOnClickListener {
            lifecycleScope.launch {
                val result = SessionRepositoryImpl().discardSession(sessionId)
                finish()
            }
        }

        viewModel.navigateToCamera.observe(this) { shouldNavigate ->
            if (shouldNavigate == true) {
                goToCamera(sessionId)
            }
        }

        findViewById<Button>(R.id.contentInstructionButton).setOnClickListener {
            viewModel.onStartClicked()
        }
    }

    private fun goToCamera(sessionId: String) {
        val intent = Intent(this, CameraSessionActivity::class.java).apply {
            putExtra("session_id", sessionId)
            putExtra("page_index", 1)
        }
        startActivity(intent)
        finish()
    }
}
