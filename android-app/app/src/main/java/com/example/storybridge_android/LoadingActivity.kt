package com.example.storybridge_android

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import com.example.storybridge_android.network.*

class LoadingActivity : AppCompatActivity() {

    private lateinit var loadingBar: ProgressBar
    private val handler = Handler(Looper.getMainLooper())
    private var isPolling = true
    private lateinit var sessionId: String
    private var pageIndex: Int = 0
    private var lang: String = "en"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading)

        loadingBar = findViewById(R.id.loadingBar)

        sessionId = intent.getStringExtra("session_id") ?: "home"
        pageIndex = intent.getIntExtra("page_index", 0)
        lang = intent.getStringExtra("lang") ?: "en"

        pollStatus()
    }

    private fun pollStatus() {
        val api = MockApiClient

        val runnable = object : Runnable {
            override fun run() {
                if (!isPolling) return

                try {
                    // MockApiClient는 즉시 응답 반환
                    val response = api.checkOcrTranslationStatus(sessionId, pageIndex).execute()
                    val body = response.body() ?: return

                    loadingBar.progress = body.progress

                    if (body.status == "ready") {
                        isPolling = false
                        fetchFinalResult()
                    } else {
                        // 다음 주기 재호출
                        handler.postDelayed(this, 800)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    handler.postDelayed(this, 1500)
                }
            }
        }

        handler.post(runnable)
    }

    private fun fetchFinalResult() {
        val api = MockApiClient
        try {
            val req = UploadImageRequest(sessionId, pageIndex, lang, "")
            val response = api.uploadImage(req).execute()
            if (response.isSuccessful) {
                val intent = Intent(this, ReadingActivity::class.java)
                startActivity(intent)
                finish()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isPolling = false
        handler.removeCallbacksAndMessages(null)
    }
}
