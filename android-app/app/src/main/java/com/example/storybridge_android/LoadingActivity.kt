package com.example.storybridge_android

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import com.example.storybridge_android.network.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileInputStream

class LoadingActivity : AppCompatActivity() {

    private lateinit var loadingBar: ProgressBar
    private val handler = Handler(Looper.getMainLooper())
    private var isPolling = true
    private lateinit var sessionId: String
    private var pageIndex: Int = 0
    private var lang: String = "en"
    private var imagePath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading)

        loadingBar = findViewById(R.id.loadingBar)

        sessionId = intent.getStringExtra("session_id") ?: return finish()
        pageIndex = intent.getIntExtra("page_index", 0)
        lang = intent.getStringExtra("lang") ?: "en"
        imagePath = intent.getStringExtra("image_path")

        if (imagePath == null) {
            Log.e("LoadingActivity", "No image path provided")
            finish()
            return
        }

        uploadImage()
    }

    private fun uploadImage() {
        val file = File(imagePath!!)
        val bytes = FileInputStream(file).readBytes()
        val base64 = Base64.encodeToString(bytes, Base64.DEFAULT)

        val req = UploadImageRequest(
            session_id = sessionId,
            page_index = pageIndex,
            lang = lang,
            image_base64 = base64
        )

        RetrofitClient.processApi.uploadImage(req)
            .enqueue(object : Callback<UploadImageResponse> {
                override fun onResponse(
                    call: Call<UploadImageResponse>,
                    response: Response<UploadImageResponse>
                ) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body != null) {
                            // 서버가 반환한 최신 page_index로 업데이트
                            pageIndex = body.page_index
                            Log.d("LoadingActivity", "Upload success → new page_index: $pageIndex")
                            pollStatus()
                        } else {
                            Log.e("LoadingActivity", "Upload success but body is null")
                            finish()
                        }
                    } else {
                        Log.e("LoadingActivity", "Upload failed: ${response.code()}")
                        finish()
                    }
                }

                override fun onFailure(call: Call<UploadImageResponse>, t: Throwable) {
                    Log.e("LoadingActivity", "Upload error: ${t.message}")
                    finish()
                }
            })
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
                        val body = response.body()
                        if (body != null) {
                            loadingBar.progress = body.progress
                            Log.d("LoadingActivity", "OCR progress: ${body.progress}")

                            if (body.status == "ready") {
                                Log.d("LoadingActivity", "OCR ready → checking TTS")
                                checkTtsStatus()
                            } else {
                                handler.postDelayed(runnable!!, 1000)
                            }
                        }
                    }

                    override fun onFailure(
                        call: Call<CheckOcrTranslationResponse>,
                        t: Throwable
                    ) {
                        Log.e("LoadingActivity", "Polling error: ${t.message}")
                        handler.postDelayed(runnable!!, 1500)
                    }
                })
        }

        handler.post(runnable)
    }

    private fun checkTtsStatus() {
        val api = RetrofitClient.processApi

        api.checkTtsStatus(sessionId, pageIndex)
            .enqueue(object : Callback<CheckTtsResponse> {
                override fun onResponse(
                    call: Call<CheckTtsResponse>,
                    response: Response<CheckTtsResponse>
                ) {
                    val body = response.body()
                    if (body != null && body.status == "ready") {
                        Log.d("LoadingActivity", "TTS ready → moving to ReadingActivity")
                        isPolling = false
                        navigateToReading()
                    } else {
                        handler.postDelayed({ checkTtsStatus() }, 1000)
                    }
                }

                override fun onFailure(call: Call<CheckTtsResponse>, t: Throwable) {
                    Log.e("LoadingActivity", "TTS polling error: ${t.message}")
                    handler.postDelayed({ checkTtsStatus() }, 1500)
                }
            })
    }

    private fun navigateToReading() {
        val intent = Intent(this, ReadingActivity::class.java)
        intent.putExtra("session_id", sessionId)
        intent.putExtra("page_index", pageIndex)
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        isPolling = false
        handler.removeCallbacksAndMessages(null)
    }
}
