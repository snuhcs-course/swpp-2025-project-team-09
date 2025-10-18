package com.example.storybridge_android

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.storybridge_android.R

class FinishActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_finish)

        // 시스템 인셋 적용
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val mainButton: View = findViewById(R.id.mainButton)

        // 3초 뒤에 버튼 보이게 하기
        Handler(Looper.getMainLooper()).postDelayed({
            mainButton.visibility = View.VISIBLE
        }, 3000)

        // 버튼 클릭 시 메인으로 이동
        mainButton.setOnClickListener {
            navigateToMain()
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun displayStats() {
        // TODO: 학습 내역 요약 보여줌
    }

    private fun decideSave() {
        // TODO: 저장할지 안 할지 결정
    }

    private fun endSession() {
        // TODO: 세션 종료를 서버에 알림
    }
}
