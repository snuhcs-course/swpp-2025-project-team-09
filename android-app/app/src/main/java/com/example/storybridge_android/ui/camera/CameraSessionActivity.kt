package com.example.storybridge_android.ui.camera

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.storybridge_android.ui.common.BaseActivity
import com.example.storybridge_android.ui.session.LoadingActivity
import com.example.storybridge_android.ui.session.VoiceSelectActivity
import com.example.storybridge_android.ui.setting.AppSettings
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CameraSessionActivity : BaseActivity() {

    private var sessionId: String? = null
    private var pageIndex: Int = 0
    private var isCover: Boolean = false

    private val viewModel: CameraSessionViewModel by viewModels {
        testViewModelFactory ?: CameraSessionViewModelFactory()
    }

    companion object {
        private const val TAG = "CameraSessionActivity"
        var testViewModelFactory: ViewModelProvider.Factory? = null
        var testMode: Boolean = false
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
                            navigateToVoiceSelect(state.imagePath)
                        } else {
                            navigateToLoading(state.imagePath)
                        }
                    }
                    is SessionUiState.Cancelled -> {
                        discardSessionAndFinish()
                    }
                    is SessionUiState.Error -> {
                        Log.e(TAG, state.message)
                        discardSessionAndFinish()
                    }
                }
            }
        }
    }

    private fun discardSessionAndFinish() {
        setResult(RESULT_CANCELED)
        finish()

        lifecycleScope.launch {
            sessionId?.let { sid ->
                viewModel.discardSession(sid)
            }
        }
    }


    private fun startCamera() {
        if (testMode) return

        val intent = Intent(this, CameraActivity::class.java).apply {
            putExtra("is_cover", isCover)
        }
        cameraLauncher.launch(intent)
    }

    private fun navigateToVoiceSelect(imagePath: String) {
        val intent = Intent(this, VoiceSelectActivity::class.java)
        intent.putExtra("session_id", sessionId)
        intent.putExtra("image_path", imagePath)
        intent.putExtra("lang", AppSettings.getLanguage(this))
        startActivity(intent)

        setResult(RESULT_OK, Intent().putExtra("page_added", true))
        finish()
    }

    private fun navigateToLoading(imagePath: String) {
        val intent = Intent(this, LoadingActivity::class.java)
        intent.putExtra("session_id", sessionId)
        intent.putExtra("page_index", pageIndex)
        intent.putExtra("image_path", imagePath)
        intent.putExtra("is_cover", isCover)
        intent.putExtra("lang", AppSettings.getLanguage(this))
        startActivity(intent)

        setResult(RESULT_OK, Intent().putExtra("page_added", true))
        finish()
    }
}