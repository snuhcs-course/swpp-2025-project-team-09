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

        val manButton = findViewById<Button>(R.id.manButton)
        val womanButton = findViewById<Button>(R.id.womanButton)

        val voiceOptions = mapOf(
            manButton to "man",
            womanButton to "woman"
        )

        voiceOptions.forEach { (button, voiceType) ->
            button.setOnClickListener {
                val intent = Intent(this, CameraActivity::class.java)

                // 2. 주소(Key) 관리를 위해 상수 사용 + Map에서 가져온 값(Value) 사용
                intent.putExtra(CameraActivity.VOICE_TYPE, voiceType)

                startActivity(intent)
            }
        }
    }
}