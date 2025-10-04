package com.example.storybridge_android

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.Button
import android.content.Intent

import com.example.storybridge_android.ui.TopNavigationBar

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val topNav = findViewById<TopNavigationBar>(R.id.topNavigationBar)
        topNav.setOnSettingsClickListener {
            // 클릭 시 SettingActivity로 이동 (TopNavigationBar.kt 내부에서도 이미 처리 가능)
            // 여기서 추가로 다른 작업 가능
        }

        // Start New Reading 버튼 클릭 시 VoiceSelectActivity로 이동
        val startButton = findViewById<Button>(R.id.startNewReadingButton)
        startButton.setOnClickListener {
            val intent = Intent(this, VoiceSelectActivity::class.java)
            startActivity(intent)
        }
    }
}