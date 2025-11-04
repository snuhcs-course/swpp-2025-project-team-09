package com.example.storybridge_android.ui.camera

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.storybridge_android.ui.session.LoadingActivity
import kotlinx.coroutines.flow.collectLatest

class CameraSessionActivity : AppCompatActivity() {

    private var sessionId: String? = null
    private var pageIndex: Int = 0
    private var isCover: Boolean = false

    private val viewModel: CameraSessionViewModel by viewModels { CameraSessionViewModelFactory() }

    companion object {
        private const val TAG = "CameraSessionActivity"
    }

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val imagePath = result.data?.getStringExtra("image_path")
            viewModel.handleCameraResult(result.resultCode, imagePath)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionId = intent.getStringExtra("session_id")
        pageIndex = intent.getIntExtra("page_index", 0)
        isCover = intent.getBooleanExtra("is_cover", false)

        if (sessionId == null) {
            finish()
            return
        }

        lifecycleScope.launchWhenStarted {
            viewModel.uiState.collectLatest { state ->
                when (state) {
                    is SessionUiState.Idle -> startCamera()
                    is SessionUiState.Success -> {
                        navigateToLoading(state.imagePath)
                    }
                    is SessionUiState.Cancelled -> finish()
                    is SessionUiState.Error -> {
                        Log.e(TAG, state.message)
                        finish()
                    }
                }
            }
        }
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
        intent.putExtra("is_cover", isCover)
        startActivity(intent)
        finish()
    }
}
