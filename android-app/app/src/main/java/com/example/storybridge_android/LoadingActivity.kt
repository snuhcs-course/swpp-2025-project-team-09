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

    companion object {
        private const val TAG = "LoadingActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading)

        Log.d(TAG, "=== LoadingActivity onCreate ===")

        loadingBar = findViewById(R.id.loadingBar)

        sessionId = intent.getStringExtra("session_id") ?: run {
            Log.e(TAG, "✗ No session_id provided")
            finish()
            return
        }
        pageIndex = intent.getIntExtra("page_index", 0)
        lang = intent.getStringExtra("lang") ?: "en"
        imagePath = intent.getStringExtra("image_path")

        Log.d(TAG, "Session ID: $sessionId")
        Log.d(TAG, "Page index: $pageIndex")
        Log.d(TAG, "Language: $lang")
        Log.d(TAG, "Image path: $imagePath")

        if (imagePath == null) {
            Log.e(TAG, "✗ No image path provided")
            finish()
            return
        }

        uploadImage()
    }

    private fun uploadImage() {
        Log.d(TAG, "=== Uploading Image ===")

        val file = File(imagePath!!)
        if (!file.exists()) {
            Log.e(TAG, "✗ Image file does not exist: $imagePath")
            finish()
            return
        }

        Log.d(TAG, "File exists: ${file.absolutePath}")
        Log.d(TAG, "File size: ${file.length()} bytes")

        val bytes = FileInputStream(file).readBytes()
        val base64 = Base64.encodeToString(bytes, Base64.DEFAULT)

        Log.d(TAG, "Base64 encoded, length: ${base64.length}")

        val req = UploadImageRequest(
            session_id = sessionId,
            page_index = pageIndex,
            lang = lang,
            image_base64 = base64
        )

        Log.d(TAG, "Making API call to /process/upload...")

        RetrofitClient.processApi.uploadImage(req)
            .enqueue(object : Callback<UploadImageResponse> {
                override fun onResponse(
                    call: Call<UploadImageResponse>,
                    response: Response<UploadImageResponse>
                ) {
                    Log.d(TAG, "=== Upload Response Received ===")
                    Log.d(TAG, "Response code: ${response.code()}")

                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body != null) {
                            pageIndex = body.page_index
                            Log.d(TAG, "✓ Upload successful")
                            Log.d(TAG, "Updated page_index: $pageIndex")
                            Log.d(TAG, "Status: ${body.status}")
                            pollStatus()
                        } else {
                            Log.e(TAG, "✗ Upload success but body is null")
                            finish()
                        }
                    } else {
                        Log.e(TAG, "✗ Upload failed: ${response.code()}")
                        try {
                            Log.e(TAG, "Error body: ${response.errorBody()?.string()}")
                        } catch (e: Exception) {
                            Log.e(TAG, "Could not read error body", e)
                        }
                        finish()
                    }
                }

                override fun onFailure(call: Call<UploadImageResponse>, t: Throwable) {
                    Log.e(TAG, "✗ Upload error: ${t.message}", t)
                    finish()
                }
            })
    }

    private fun pollStatus() {
        Log.d(TAG, "=== Starting OCR Status Polling ===")

        val api = RetrofitClient.processApi
        var runnable: Runnable? = null
        var pollCount = 0

        runnable = Runnable {
            if (!isPolling) {
                Log.d(TAG, "Polling stopped")
                return@Runnable
            }

            pollCount++
            Log.d(TAG, "OCR Poll attempt #$pollCount")

            api.checkOcrStatus(sessionId, pageIndex)
                .enqueue(object : Callback<CheckOcrResponse> {
                    override fun onResponse(
                        call: Call<CheckOcrResponse>,
                        response: Response<CheckOcrResponse>
                    ) {
                        if (!response.isSuccessful) {
                            Log.e(TAG, "OCR status check failed: ${response.code()}")
                            handler.postDelayed(runnable!!, 1500)
                            return
                        }

                        val body = response.body()
                        if (body != null) {
                            loadingBar.progress = body.progress
                            Log.d(TAG, "OCR Status: ${body.status}, Progress: ${body.progress}%")

                            if (body.status == "ready") {
                                Log.d(TAG, "✓ OCR is ready! Moving to TTS check...")
                                checkTtsStatus()
                            } else {
                                Log.d(TAG, "OCR still processing, will retry in 1 second...")
                                handler.postDelayed(runnable!!, 1000)
                            }
                        } else {
                            Log.e(TAG, "OCR response body is null")
                            handler.postDelayed(runnable!!, 1500)
                        }
                    }

                    override fun onFailure(
                        call: Call<CheckOcrResponse>,
                        t: Throwable
                    ) {
                        Log.e(TAG, "OCR polling error: ${t.message}", t)
                        handler.postDelayed(runnable!!, 1500)
                    }
                })
        }

        handler.post(runnable)
    }

    private fun checkTtsStatus() {
        Log.d(TAG, "=== Checking TTS Status ===")

        val api = RetrofitClient.processApi
        var ttsCheckCount = 0

        fun checkStatus() {
            if (!isPolling) {
                Log.d(TAG, "Polling stopped")
                return
            }

            ttsCheckCount++
            Log.d(TAG, "TTS check attempt #$ttsCheckCount")

            api.checkTtsStatus(sessionId, pageIndex)
                .enqueue(object : Callback<CheckTtsResponse> {
                    override fun onResponse(
                        call: Call<CheckTtsResponse>,
                        response: Response<CheckTtsResponse>
                    ) {
                        if (!response.isSuccessful) {
                            Log.e(TAG, "TTS status check failed: ${response.code()}")
                            handler.postDelayed({ checkStatus() }, 1500)
                            return
                        }

                        val body = response.body()
                        if (body != null) {
                            Log.d(TAG, "TTS Status: ${body.status}, Progress: ${body.progress}%")

                            if (body.status == "ready") {
                                Log.d(TAG, "✓ TTS is ready! Navigating to ReadingActivity...")
                                isPolling = false
                                navigateToReading()
                            } else {
                                Log.d(TAG, "TTS still processing, will retry in 1 second...")
                                handler.postDelayed({ checkStatus() }, 1000)
                            }
                        } else {
                            Log.e(TAG, "TTS response body is null")
                            handler.postDelayed({ checkStatus() }, 1500)
                        }
                    }

                    override fun onFailure(call: Call<CheckTtsResponse>, t: Throwable) {
                        Log.e(TAG, "TTS polling error: ${t.message}", t)
                        handler.postDelayed({ checkStatus() }, 1500)
                    }
                })
        }

        checkStatus()
    }

    private fun navigateToReading() {
        Log.d(TAG, "=== Navigating to ReadingActivity ===")
        Log.d(TAG, "Session ID: $sessionId")
        Log.d(TAG, "Page index: $pageIndex")

        val intent = Intent(this, ReadingActivity::class.java)
        intent.putExtra("session_id", sessionId)
        intent.putExtra("page_index", pageIndex)

        Log.d(TAG, "Starting ReadingActivity...")
        startActivity(intent)

        Log.d(TAG, "Finishing LoadingActivity")
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        isPolling = false
        handler.removeCallbacksAndMessages(null)
        Log.d(TAG, "=== Activity destroyed ===")
    }
}