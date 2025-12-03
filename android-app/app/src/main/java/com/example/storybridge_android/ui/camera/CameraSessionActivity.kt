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
import com.example.storybridge_android.ui.common.BaseActivity
import com.example.storybridge_android.ui.session.instruction.ContentInstructionActivity
import com.example.storybridge_android.ui.session.loading.LoadingActivity
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

    private lateinit var retakeCameraPanel: View
    private lateinit var retakeCameraConfirmBtn: Button
    private lateinit var retakeCameraCancelBtn: Button

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
                navigateToLoading(imagePath)
            } else {
                viewModel.handleCameraResult(result.resultCode, imagePath)
            }
        }

    private val loadingLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                navigateToContentInstruction()
            } else {
                val needRetake = result.data?.getBooleanExtra("retake", false) == true
                if (needRetake) {
                    showRetakeDialog()
                }
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
        initRetakeCameraPanel()
        observeViewModel()
    }

    private fun initRetakePanel() {
        retakePanel = findViewById(R.id.retakePanel)
        retakeConfirmBtn = findViewById(R.id.retakeConfirmBtn)
        retakeCancelBtn = findViewById(R.id.retakeCancelBtn)

        // Initially hide the retake panel
        retakePanel.visibility = View.GONE

        retakeConfirmBtn.setOnClickListener {
            retakePanel.visibility = View.GONE
            // Give user another chance to take a photo
            startCamera()
        }

        retakeCancelBtn.setOnClickListener {
            retakePanel.visibility = View.GONE
            discardSessionAndFinish()
        }
    }

    private fun initRetakeCameraPanel() {
        retakeCameraPanel = findViewById(R.id.retakeCameraPanel)
        retakeCameraConfirmBtn = findViewById(R.id.retakeCameraConfirmBtn)
        retakeCameraCancelBtn = findViewById(R.id.retakeCameraCancelBtn)

        // Initially hide the camera retake panel
        retakeCameraPanel.visibility = View.GONE

        retakeCameraConfirmBtn.setOnClickListener {
            retakeCameraPanel.visibility = View.GONE
            // Give user another chance to take a photo
            startCamera()
        }

        retakeCameraCancelBtn.setOnClickListener {
            retakeCameraPanel.visibility = View.GONE
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
                        navigateToContentInstruction()
                    }
                    is SessionUiState.NoTextDetected -> {
                        showRetakeDialog()
                    }
                    is SessionUiState.Success -> {
                        if (isCover) navigateToContentInstruction()
                        else navigateToLoading(state.imagePath)
                    }
                    is SessionUiState.Cancelled -> {
                        // User cancelled camera - show camera retake dialog
                        showRetakeCameraDialog()
                    }
                    is SessionUiState.Error -> {
                        Log.e(TAG, state.message)
                        showRetakeDialog()
                    }
                }
            }
        }
    }

    private fun showRetakeDialog() {
        retakePanel.visibility = View.VISIBLE
    }

    private fun showRetakeCameraDialog() {
        retakeCameraPanel.visibility = View.VISIBLE
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

    private fun navigateToContentInstruction() {
        val intent = Intent(this, ContentInstructionActivity::class.java).apply {
            putExtra("session_id", sessionId)
        }
        startActivity(intent)
        finish()
    }

    private fun navigateToLoading(imagePath: String) {
        val intent = Intent(this, LoadingActivity::class.java).apply {
            putExtra("session_id", sessionId)
            putExtra("page_index", pageIndex)
            putExtra("image_path", imagePath)
            putExtra("is_cover", isCover)
            putExtra("lang", lang)
        }
        loadingLauncher.launch(intent)
    }
}