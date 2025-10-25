package com.example.storybridge_android

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class CameraSessionActivity : AppCompatActivity() {

    private var sessionId: String? = null
    private var pageIndex: Int = 0

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
                    navigateToLoading(imagePath)
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