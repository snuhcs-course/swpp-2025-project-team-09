package com.example.storybridge_android

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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

        setupTopNavigationBar()
        setupStartButton()
    }

    private fun navigateToSettings() {
        // TODO: SettinsActivity로 이동하는 로직 구현
        // TODO: onCreate에서 이 함수를 버튼에 바인딩하는 함수를 호출하면 될 것 같습니다.
    }

    private fun navigateToStartSession() {
        // TODO: StartSession.Activity로 이동하는 로직 구현
    }

    private fun navigateToLoadingSession() {
        // TODO: LoadingActivity로 이동하는 로직 구현
    }

    private fun loadSessionCard() {
        // TODO: SessionCard.kt 컴포넌트들을 화면에 로딩
    }



    /**
     * TopNavigationBar의 클릭 이벤트 연결
     */
    private fun setupTopNavigationBar() {
        val topNav = findViewById<TopNavigationBar>(R.id.topNavigationBar)
        topNav.setOnSettingsClickListener {
            onSettingsClick()
            // TODO: 이 부분을 navigateToSettings()로 대체하는 식으로 구현하면 될 것 같습니다.
        }
    }

    /**
     * "Start New Reading" 버튼 초기화 및 클릭 이벤트 연결
     */
    private fun setupStartButton() {
        val startButton = findViewById<Button>(R.id.startNewReadingButton)
        startButton.setOnClickListener {
            onStartNewReadingClick()
        }
    }

    /**
     * ⚙️ 설정 버튼 클릭 시 호출
     */
    private fun onSettingsClick() {
        val intent = Intent(this, SettingActivity::class.java)
        startActivity(intent)
    }

    /**
     * Start New Reading 버튼 클릭 시 호출
     */
    private fun onStartNewReadingClick() {
        val intent = Intent(this, VoiceSelectActivity::class.java)
        startActivity(intent)
    }
}
