package com.example.storybridge_android.ui.camera

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.storybridge_android.ui.session.LoadingActivity
import com.example.storybridge_android.ui.session.VoiceSelectActivity
import com.example.storybridge_android.ui.setting.AppSettings
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
        enableEdgeToEdge()

        sessionId = intent.getStringExtra("session_id")
        pageIndex = intent.getIntExtra("page_index", 0)
        isCover = intent.getBooleanExtra("is_cover", false)

        if (sessionId == null) {
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        lifecycleScope.launchWhenStarted {
            viewModel.uiState.collectLatest { state ->
                when (state) {
                    is SessionUiState.Idle -> startCamera()
                    is SessionUiState.Success -> {
                        if (isCover) {
                            // Cover 촬영 시 바로 VoiceSelectActivity로 이동
                            navigateToVoiceSelect(state.imagePath)
                        } else {
                            // 일반 페이지는 기존대로 LoadingActivity로
                            navigateToLoading(state.imagePath)
                        }
                    }
                    is SessionUiState.Cancelled -> {
                        // User cancelled - return to previous activity (ReadingActivity)
                        setResult(RESULT_CANCELED)
                        finish()
                    }
                    is SessionUiState.Error -> {
                        Log.e(TAG, state.message)
                        setResult(RESULT_CANCELED)
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

    private fun navigateToVoiceSelect(imagePath: String) {
        val intent = Intent(this, VoiceSelectActivity::class.java)
        intent.putExtra("session_id", sessionId)
        intent.putExtra("image_path", imagePath)
        intent.putExtra("lang", AppSettings.getLanguage(this))
        startActivity(intent)

        // Signal success back to ReadingActivity
        setResult(RESULT_OK, Intent().putExtra("page_added", true))
        finish()
    }

    private fun navigateToLoading(imagePath: String) {
        val intent = Intent(this, LoadingActivity::class.java)
        intent.putExtra("session_id", sessionId)
        intent.putExtra("page_index", pageIndex)
        intent.putExtra("image_path", imagePath)
        intent.putExtra("is_cover", isCover)
        intent.putExtra("lang", AppSettings.getLanguage(this))  // 현재 언어 설정 전달
        startActivity(intent)

        // Signal success back to ReadingActivity
        setResult(RESULT_OK, Intent().putExtra("page_added", true))
        finish()
    }
}