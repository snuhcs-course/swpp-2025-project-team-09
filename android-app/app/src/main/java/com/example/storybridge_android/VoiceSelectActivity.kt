package com.example.storybridge_android

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class VoiceSelectActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_voice_select)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupClickListeners()
    }

    private fun navigateToCamera() {
        val intent = Intent(this, CameraActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun setupClickListeners() {
        val manButton = findViewById<Button>(R.id.manButton)
        val womanButton = findViewById<Button>(R.id.womanButton)

        manButton.setOnClickListener {
            AppSettings.setVoice(this, "male")
            navigateToCamera()
        }

        womanButton.setOnClickListener {
            AppSettings.setVoice(this, "female")
            navigateToCamera()
        }
    }
}
