package com.example.storybridge_android

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageProcessor
import com.example.storybridge_android.network.RetrofitClient
import com.example.storybridge_android.network.UploadFrontRequest
import com.example.storybridge_android.network.UploadFrontResponse
import com.example.storybridge_android.network.UploadImageResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

class CameraSessionActivity : AppCompatActivity() {

    private var sessionId: String? = null
    private var pageIndex: Int = 0
    private var isFront: Boolean = false


    companion object {
        private const val TAG = "CameraSessionActivity"
    }

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            Log.d(TAG, "=== Camera Result Received ===")
            Log.d(TAG, "Result code: ${result.resultCode} (OK=$RESULT_OK, CANCELED=$RESULT_CANCELED)")

            if (result.resultCode == RESULT_OK) {
                val imagePath = result.data?.getStringExtra("image_path")
                Log.d(TAG, "Image path: $imagePath")

                if (!imagePath.isNullOrEmpty()) {
                    Log.d(TAG, "✓ Valid image path received")
                    navigateAfterScan(imagePath)
                } else {
                    Log.e(TAG, "✗ Image path is null or empty")
                    finish()
                }
            } else {
                Log.d(TAG, "Camera was cancelled by user or failed")
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "=== CameraSessionActivity onCreate ===")

        sessionId = intent.getStringExtra("session_id")
        pageIndex = intent.getIntExtra("page_index", 0)
        isFront = intent.getBooleanExtra("is_front", false)

        Log.d(TAG, "Received session_id: $sessionId")
        Log.d(TAG, "Received page_index: $pageIndex")

        if (sessionId == null) {
            Log.e(TAG, "✗ No session_id received! Cannot proceed.")
            finish()
            return
        }

        startCamera()
    }

    private fun startCamera() {
        Log.d(TAG, "=== Starting CameraActivity ===")
        val intent = Intent(this, CameraActivity::class.java)
        Log.d(TAG, "Launching CameraActivity...")
        cameraLauncher.launch(intent)
        Log.d(TAG, "CameraActivity launched, waiting for result...")
    }

    private fun navigateAfterScan(imagePath: String) {
        if (isFront) {
            // ✅ 표지: 로딩 화면 없이 업로드만 수행하고 보이스 선택으로
            uploadFrontAndGoToVoiceSelect(imagePath)
        } else {
            // ✅ 내부 페이지: 기존 로딩 화면 사용
            navigateToLoading(imagePath)
        }
    }

    private fun uploadFrontAndGoToVoiceSelect(imagePath: String, lang: String = "en") {
        val sid = sessionId ?: return finish()
        val file = File(imagePath)
        if (!file.exists()) { finish(); return }

        val base64 = android.util.Base64.encodeToString(file.readBytes(), android.util.Base64.DEFAULT)
        val req = UploadFrontRequest(session_id = sid, lang = lang, image_base64 = base64)

        RetrofitClient.processApi.uploadFront(req)
            .enqueue(object : retrofit2.Callback<UploadFrontResponse> {
                override fun onResponse(
                    call: Call<UploadFrontResponse>,
                    response: Response<UploadFrontResponse>   // ← 네 코드의 ImageProcessor.Response 는 오타
                ) {
                    if (response.isSuccessful) {
                        val newIndex = response.body()?.page_index ?: 0
                        goToVoiceSelect(newIndex)   // 서버가 준 page_index를 그대로 사용
                    } else {
                        finish()
                    }
                }
                override fun onFailure(call: Call<UploadFrontResponse>, t: Throwable) {
                    finish()
                }
            })
    }



    private fun goToVoiceSelect(pageIdx: Int) {
        val intent = Intent(this, VoiceSelectActivity::class.java)
        intent.putExtra("session_id", sessionId)
        intent.putExtra("page_index", pageIdx) // 일반적으로 0
        startActivity(intent)
        finish()
    }

    private fun navigateToLoading(imagePath: String) {
        Log.d(TAG, "=== Navigating to LoadingActivity ===")
        Log.d(TAG, "Session ID: $sessionId")
        Log.d(TAG, "Page index: $pageIndex")
        Log.d(TAG, "Image path: $imagePath")

        val intent = Intent(this, LoadingActivity::class.java)
        intent.putExtra("session_id", sessionId)
        intent.putExtra("page_index", pageIndex)
        intent.putExtra("image_path", imagePath)

        Log.d(TAG, "Starting LoadingActivity...")
        startActivity(intent)

        Log.d(TAG, "Finishing CameraSessionActivity")
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "=== Activity destroyed ===")
    }
}