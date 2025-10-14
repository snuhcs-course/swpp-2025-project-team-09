package com.example.storybridge_android

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.storybridge_android.databinding.ActivityFinishBinding

class FinishActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFinishBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFinishBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 3초 뒤에 버튼 보이게 하기
        Handler(Looper.getMainLooper()).postDelayed({
            binding.mainButton.visibility = View.VISIBLE
        }, 3000)

        // 버튼 클릭 시 MainActivity로 이동
        binding.mainButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // 현재 FinishActivity 종료
        }
    }

    private fun navigateToMain() {
        // TODO: MainActivity로 이동하는 로직 구현
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