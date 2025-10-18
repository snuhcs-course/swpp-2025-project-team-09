package com.example.storybridge_android

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.storybridge_android.databinding.ActivityFinishBinding
import com.example.storybridge_android.network.EndSessionRequest
import com.example.storybridge_android.network.EndSessionResponse
import com.example.storybridge_android.network.RetrofitClient
import com.example.storybridge_android.network.SessionStatsResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FinishActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFinishBinding
    private val sessionApi = RetrofitClient.sessionApi
    private lateinit var sessionId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFinishBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionId = intent.getStringExtra("session_id") ?: ""

        // 세션 종료 → 통계 표시
        endSession()

        // 3초 뒤 버튼 보이기
        Handler(Looper.getMainLooper()).postDelayed({
            binding.mainButton.visibility = View.VISIBLE
        }, 3000)

        // 버튼 클릭 시 메인으로
        binding.mainButton.setOnClickListener {
            navigateToMain()
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun endSession() {
        if (sessionId.isEmpty()) return
        val req = EndSessionRequest(sessionId)
        sessionApi.endSession(req).enqueue(object : Callback<EndSessionResponse> {
            override fun onResponse(call: Call<EndSessionResponse>, response: Response<EndSessionResponse>) {
                if (response.isSuccessful) {
                    displayStats()
                }
            }
            override fun onFailure(call: Call<EndSessionResponse>, t: Throwable) {
                t.printStackTrace()
            }
        })
    }

    private fun displayStats() {
        sessionApi.getSessionStats(sessionId).enqueue(object : Callback<SessionStatsResponse> {
            override fun onResponse(call: Call<SessionStatsResponse>, response: Response<SessionStatsResponse>) {
                if (response.isSuccessful) {
                    val stats = response.body()
                    if (stats != null) {
                        val summary = """
                            Session: ${stats.session_id}
                            Pages: ${stats.total_pages}
                            Time: ${stats.total_time_spent}s
                            Words: ${stats.total_words_read}
                        """.trimIndent()
                        binding.sessionSummary.text = summary
                        binding.sessionSummary.visibility = View.VISIBLE
                    }
                }
            }
            override fun onFailure(call: Call<SessionStatsResponse>, t: Throwable) {
                t.printStackTrace()
            }
        })
    }
}
