package com.example.storybridge_android

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class CameraSessionActivity : AppCompatActivity() {

    private var sessionId: String? = null
    private var pageIndex: Int = 0

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val imagePath = result.data?.getStringExtra("image_path")
                if (!imagePath.isNullOrEmpty()) {
                    Log.d("CameraSession", "Captured image path: $imagePath")
                    navigateToLoading(imagePath)
                }
            } else {
                Log.d("CameraSession", "Camera cancelled or failed")
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionId = intent.getStringExtra("session_id")
        pageIndex = intent.getIntExtra("page_index", 0)
        startCamera()
    }

    private fun startCamera() {
        val intent = Intent(this, CameraActivity::class.java)
        cameraLauncher.launch(intent)
    }

    private fun navigateToLoading(imagePath: String) {
        val intent = Intent(this, LoadingActivity::class.java)
        intent.putExtra("session_id", sessionId)
        intent.putExtra("page_index", pageIndex)
        intent.putExtra("image_path", imagePath)
        startActivity(intent)
        finish()
    }
}
