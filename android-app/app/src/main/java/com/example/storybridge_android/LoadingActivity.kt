package com.example.storybridge_android

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import com.example.storybridge_android.network.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.example.storybridge_android.network.CheckOcrTranslationResponse
import com.example.storybridge_android.network.UploadImageResponse

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
        val api = RetrofitClient.processApi
        var runnable: Runnable? = null

        runnable = Runnable {
            if (!isPolling) return@Runnable

            api.checkOcrTranslationStatus(sessionId, pageIndex)
                .enqueue(object : Callback<CheckOcrTranslationResponse> {
                    override fun onResponse(
                        call: Call<CheckOcrTranslationResponse>,
                        response: Response<CheckOcrTranslationResponse>
                    ) {
                        val body = response.body() ?: return
                        loadingBar.progress = body.progress

                        if (body.status == "ready") {
                            isPolling = false
                            fetchFinalResult()
                        } else {
                            handler.postDelayed(runnable!!, 800)
                        }
                    }

                    override fun onFailure(
                        call: Call<CheckOcrTranslationResponse>,
                        t: Throwable
                    ) {
                        t.printStackTrace()
                        handler.postDelayed(runnable!!, 1500)
                    }
                })
        }

        handler.post(runnable)
    }


    private fun fetchFinalResult() {
        val api = RetrofitClient.processApi
        val req = UploadImageRequest(sessionId, pageIndex, lang, "")

        api.uploadImage(req).enqueue(object : Callback<UploadImageResponse> {
            override fun onResponse(
                call: Call<UploadImageResponse>,
                response: Response<UploadImageResponse>
            ) {
                if (response.isSuccessful) {
                    val intent = Intent(this@LoadingActivity, ReadingActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }

            override fun onFailure(call: Call<UploadImageResponse>, t: Throwable) {
                t.printStackTrace()
            }
        })
    }


    override fun onDestroy() {
        super.onDestroy()
        isPolling = false
        handler.removeCallbacksAndMessages(null)
    }
}
