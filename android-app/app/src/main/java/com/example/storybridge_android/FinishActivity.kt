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
}