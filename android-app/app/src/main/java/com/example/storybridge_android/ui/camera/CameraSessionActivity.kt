package com.example.storybridge_android.ui.camera

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.storybridge_android.R
import com.example.storybridge_android.network.UploadCoverResponse
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
    private lateinit var lang: String

    private lateinit var retakePanel: View
    private lateinit var retakeConfirmBtn: Button
    private lateinit var retakeCancelBtn: Button

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
            if (result.resultCode == RESULT_OK && imagePath != null) {
                if (isCover) {
                    uploadAndValidateCover(imagePath)
                } else {
                    navigateToLoading(imagePath)
                }
            } else {
                viewModel.handleCameraResult(result.resultCode, imagePath)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_camera_session)

        sessionId = intent.getStringExtra("session_id")
        pageIndex = intent.getIntExtra("page_index", 0)
        isCover = intent.getBooleanExtra("is_cover", false)
        lang = AppSettings.getLanguage(this)

        if (sessionId == null) {
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        initRetakePanel()
        observeViewModel()
    }

    private fun initRetakePanel() {
        retakePanel = findViewById(R.id.retakePanel)
        retakeConfirmBtn = findViewById(R.id.retakeConfirmBtn)
        retakeCancelBtn = findViewById(R.id.retakeCancelBtn)

        retakeConfirmBtn.setOnClickListener {
            retakePanel.visibility = View.GONE
            startCamera()
        }

        retakeCancelBtn.setOnClickListener {
            retakePanel.visibility = View.GONE
            discardSessionAndFinish()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launchWhenStarted {
            viewModel.uiState.collectLatest { state ->
                when (state) {
                    is SessionUiState.Idle -> startCamera()
                    is SessionUiState.UploadLoading -> {
                        Log.d(TAG, "Uploading cover image...")
                    }
                    is SessionUiState.UploadSuccess -> {
                        navigateToVoiceSelect(state.response)
                    }
                    is SessionUiState.NoTextDetected -> {
                        showRetakeDialog()
                    }
                    is SessionUiState.Success -> {
                        if (isCover) {
                            navigateToVoiceSelect(state.imagePath)
                        } else {
                            navigateToLoading(state.imagePath)
                        }
                    }
                    is SessionUiState.Cancelled -> {
                        if (shouldDiscardSession()) {
                            discardSessionAndFinish()
                        } else {
                            setResult(RESULT_CANCELED)
                            finish()
                        }
                    }
                    is SessionUiState.Error -> {
                        Log.e(TAG, state.message)
                        showRetakeDialog()
                    }
                }
            }
        }
    }

    private fun uploadAndValidateCover(imagePath: String) {
        sessionId?.let { sid ->
            viewModel.uploadCoverImage(sid, lang, imagePath)
        }
    }

    private fun showRetakeDialog() {
        retakePanel.visibility = View.VISIBLE
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

    private fun navigateToVoiceSelect(response: UploadCoverResponse) {
        val intent = Intent(this, VoiceSelectActivity::class.java)
        intent.putExtra("session_id", sessionId)
        intent.putExtra("title", response.title)
        intent.putExtra("translated_title", response.translated_title)
        startActivity(intent)

        setResult(RESULT_OK, Intent().putExtra("page_added", true))
        finish()
    }

    private fun navigateToVoiceSelect(imagePath: String) {
        val intent = Intent(this, VoiceSelectActivity::class.java)
        intent.putExtra("session_id", sessionId)
        intent.putExtra("image_path", imagePath)
        intent.putExtra("lang", lang)
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
        intent.putExtra("lang", lang)
        startActivity(intent)

        setResult(RESULT_OK, Intent().putExtra("page_added", true))
        finish()
    }

    private fun shouldDiscardSession(): Boolean {
        return isCover || pageIndex == 1
    }
}