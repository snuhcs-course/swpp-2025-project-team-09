package com.example.storybridge_android

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity

class LoadingActivity : AppCompatActivity() {
    private lateinit var loadingBar: ProgressBar
    private val handler = Handler(Looper.getMainLooper())
    private var progress = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading)

        loadingBar = findViewById(R.id.loadingBar)

        // 3초 동안 0 → 100% 진행
        val updateInterval = 30L      // ms 단위 (Long)
        val totalDuration = 3000      // 전체 시간 (ms)
        val steps = totalDuration / updateInterval   // 몇 번 업데이트 되는지
        val stepSize = 100 / steps.toInt()           // 한 번 업데이트 시 증가할 퍼센트

        val runnable = object : Runnable {
            override fun run() {
                if (progress < 100) {
                    progress += stepSize
                    loadingBar.progress = progress
                    handler.postDelayed(this, updateInterval) // ✅ Long 타입 사용
                } else {
                    // 다 차면 ReadingActivity로 이동
                    startActivity(Intent(this@LoadingActivity, ReadingActivity::class.java))
                    finish()
                }
            }
        }
        handler.post(runnable)
    }
}
