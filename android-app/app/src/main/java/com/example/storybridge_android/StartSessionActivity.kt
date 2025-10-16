package com.example.storybridge_android

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class StartSessionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_start_session)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun navigateToVoiceSelect() {
        // TODO: VoiceSelectActivity로 이동
    }

    private fun startCamera() {
        // TODO: 표지 사진을 찍기 위한 카메라 로직 구현
    }

    private fun uploadImage() {
        // TODO: api interface 사용해서 이미지 서버에 업로드
    }

    private fun startSession() {
        // TODO: 세션의 기본 정보를 서버에 업로드. 새로운 세션이 시작됨을 서버에 알림.
    }

    // 뭔가 함수들 역할이 명확히 구분되지 않는 것 같기도 해서 구현하면서 편하신 대로 수정해주세요.
}