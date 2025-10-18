package com.example.storybridge_android

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.storybridge_android.ui.TopNavigationBar
import android.widget.ImageView
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

        setupClickListeners()
        loadSessionCard()
    }

    private fun navigateToSettings() {
        val intent = Intent(this, SettingActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToStartSession() {
        val intent = Intent(this, StartSessionActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToLoadingSession() {
        val intent = Intent(this, LoadingActivity::class.java)
        startActivity(intent)
    }

    private fun setupClickListeners() {
        // 설정 아이콘 클릭 리스너 설정
        val settingsButton = findViewById<ImageView>(R.id.settingsIcon)
        settingsButton.setOnClickListener {
            navigateToSettings()
        }

        // "Start New Reading" 버튼 클릭 리스너 설정
        val startButton = findViewById<Button>(R.id.startNewReadingButton)
        startButton.setOnClickListener {
            navigateToStartSession()
        }

        // TODO: 이전 세션 카드를 클릭했을 때 navigateToLoadingSession() 호출하는 로직 추가
        // 예시:
        // val sessionCard = findViewById<View>(R.id.some_session_card)
        // sessionCard.setOnClickListener {
        //     navigateToLoadingSession()
        // }
    }

    private fun loadSessionCard() {
        // TODO: SessionCard.kt 컴포넌트들을 화면에 로딩
    }
}
