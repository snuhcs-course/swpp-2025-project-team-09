package com.example.storybridge_android.ui.session

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.storybridge_android.R
import com.example.storybridge_android.ui.camera.CameraSessionActivity

class ContentInstructionActivity : AppCompatActivity() {

    private var sessionId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_content_instruction)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.contentInstructionMain)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        sessionId = intent.getStringExtra("session_id")

        if (sessionId == null) {
            Toast.makeText(this, "Session error. Please try again.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val startButton = findViewById<Button>(R.id.contentInstructionButton)
        startButton.setOnClickListener {
            goToCamera()
        }
    }

    private fun goToCamera() {
        val intent = Intent(this, CameraSessionActivity::class.java).apply {
            putExtra("session_id", sessionId)
            putExtra("page_index", 0)  // first page
        }
        startActivity(intent)
        finish()
    }
}
