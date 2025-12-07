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

    // retakePanel - for "no text detected" errors
    private lateinit var retakePanel: View
    private lateinit var retakeConfirmBtn: Button
    private lateinit var retakeCancelBtn: Button

    // exitPanel - for camera cancellation
    private lateinit var exitPanel: View
    private lateinit var exitConfirmBtn: Button
    private lateinit var exitCancelBtn: Button

    private val viewModel: CameraSessionViewModel by viewModels {
        testViewModelFactory ?: CameraSessionViewModelFactory()
    }

    // Track if camera has been launched to prevent multiple launches
    private var cameraLaunched = false

    companion object {
        private const val TAG = "CameraSessionActivity"
        var testViewModelFactory: ViewModelProvider.Factory? = null
        var testMode: Boolean = false
    }

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            Log.d(TAG, "Camera result received: resultCode=${result.resultCode}")
            cameraLaunched = false  // Reset flag

            val imagePath = result.data?.getStringExtra("image_path")
            if (result.resultCode == RESULT_OK && imagePath != null) {
                Log.d(TAG, "Camera success, navigating to loading")
                navigateToLoading(imagePath)
            } else {
                Log.d(TAG, "Camera cancelled or failed, notifying ViewModel")
                viewModel.handleCameraResult(result.resultCode, imagePath)
            }
        }

    private val loadingLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            Log.d(TAG, "Loading result received: resultCode=${result.resultCode}")

            if (result.resultCode == RESULT_OK) {
                navigateToContentInstruction()
            } else {
                val needRetake = result.data?.getBooleanExtra("retake", false) == true
                if (needRetake) {
                    Log.d(TAG, "Retake requested from loading")
                    showRetakeDialog()
                } else {
                    Log.d(TAG, "Loading cancelled without retake")
                    showExitDialog()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate called")
        enableEdgeToEdge()
        setContentView(R.layout.activity_camera_session)

        sessionId = intent.getStringExtra("session_id")
        pageIndex = intent.getIntExtra("page_index", 0)
        isCover = intent.getBooleanExtra("is_cover", false)
        lang = AppSettings.getLanguage(this)

        Log.d(TAG, "Session info - ID: $sessionId, page: $pageIndex, isCover: $isCover")

        if (sessionId == null) {
            Log.e(TAG, "No session ID provided, finishing")
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        initRetakePanel()
        initExitPanel()
        observeViewModel()
    }

    private fun initRetakePanel() {
        retakePanel = findViewById(R.id.retakePanel) ?: run {
            Log.e(TAG, "retakePanel not found!")
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        retakeConfirmBtn = findViewById(R.id.retakeConfirmBtn) ?: run {
            Log.e(TAG, "retakeConfirmBtn not found!")
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        retakeCancelBtn = findViewById(R.id.retakeCancelBtn) ?: run {
            Log.e(TAG, "retakeCancelBtn not found!")
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        // Initially hide the retake panel
        retakePanel.visibility = View.GONE

        // Retake button - try camera again
        retakeConfirmBtn.setOnClickListener {
            Log.d(TAG, "Retake confirm clicked")
            retakePanel.visibility = View.GONE
            // Reset state BEFORE launching camera so state transition works
            viewModel.resetState()
            startCamera()
        }

        // Cancel button - exit session
        retakeCancelBtn.setOnClickListener {
            Log.d(TAG, "Retake cancel clicked")
            retakePanel.visibility = View.GONE
            discardSessionAndFinish()
        }
    }

    private fun initExitPanel() {
        exitPanel = findViewById(R.id.exitPanel) ?: run {
            Log.e(TAG, "exitPanel not found!")
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        exitConfirmBtn = findViewById(R.id.exitConfirmBtn) ?: run {
            Log.e(TAG, "exitConfirmBtn not found!")
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        exitCancelBtn = findViewById(R.id.exitCancelBtn) ?: run {
            Log.e(TAG, "exitCancelBtn not found!")
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        // Initially hide the exit panel
        exitPanel.visibility = View.GONE

        // Confirm button - exit session
        exitConfirmBtn.setOnClickListener {
            Log.d(TAG, "Exit confirm clicked")
            exitPanel.visibility = View.GONE
            discardSessionAndFinish()
        }

        // Cancel button - try camera again
        exitCancelBtn.setOnClickListener {
            Log.d(TAG, "Exit cancel clicked")
            exitPanel.visibility = View.GONE
            // Reset state BEFORE launching camera so state transition works
            viewModel.resetState()
            startCamera()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launchWhenStarted {
            viewModel.uiState.collectLatest { state ->
                Log.d(TAG, "UI State changed: ${state.javaClass.simpleName}")

                when (state) {
                    is SessionUiState.Idle -> {
                        Log.d(TAG, "State: Idle - starting camera")
                        hideAllPanels()
                        startCamera()
                    }
                    is SessionUiState.UploadLoading -> {
                        Log.d(TAG, "State: UploadLoading")
                        hideAllPanels()
                    }
                    is SessionUiState.UploadSuccess -> {
                        Log.d(TAG, "State: UploadSuccess")
                        hideAllPanels()
                        navigateToContentInstruction()
                    }
                    is SessionUiState.NoTextDetected -> {
                        Log.d(TAG, "State: NoTextDetected - showing retake dialog")
                        showRetakeDialog()
                    }
                    is SessionUiState.Success -> {
                        Log.d(TAG, "State: Success - imagePath=${state.imagePath}")
                        hideAllPanels()
                        if (isCover) navigateToContentInstruction()
                        else navigateToLoading(state.imagePath)
                    }
                    is SessionUiState.Cancelled -> {
                        Log.d(TAG, "State: Cancelled - showing exit dialog")
                        showExitDialog()
                    }
                    is SessionUiState.Error -> {
                        Log.e(TAG, "State: Error - ${state.message}")
                        showRetakeDialog()
                    }
                }
            }
        }
    }

    private fun hideAllPanels() {
        Log.d(TAG, "Hiding all panels")
        retakePanel.visibility = View.GONE
        exitPanel.visibility = View.GONE
    }

    private fun showRetakeDialog() {
        Log.d(TAG, "Showing retake dialog")
        exitPanel.visibility = View.GONE
        retakePanel.visibility = View.VISIBLE
    }

    private fun showExitDialog() {
        Log.d(TAG, "Showing exit dialog")
        retakePanel.visibility = View.GONE
        exitPanel.visibility = View.VISIBLE
    }

    private fun discardSessionAndFinish() {
        Log.d(TAG, "Discarding session and finishing")
        setResult(RESULT_CANCELED)
        finish()

        lifecycleScope.launch {
            sessionId?.let { sid ->
                if (pageIndex <= 1) {
                    viewModel.discardSession(sid)
                    Log.d(TAG, "Session will be discarded (pageIndex: $pageIndex)")
                } else {
                    Log.d(TAG, "Session kept (pageIndex: $pageIndex)")
                }
            }
        }
    }

    private fun startCamera() {
        if (testMode) {
            Log.d(TAG, "Test mode enabled, skipping camera launch")
            return
        }

        if (cameraLaunched) {
            Log.w(TAG, "Camera already launched, ignoring duplicate call")
            return
        }

        Log.d(TAG, "Launching camera activity")
        cameraLaunched = true

        val intent = Intent(this, CameraActivity::class.java).apply {
            putExtra("is_cover", isCover)
        }
        cameraLauncher.launch(intent)
    }

    private fun navigateToContentInstruction() {
        Log.d(TAG, "Navigating to ContentInstruction")
        val intent = Intent(this, ContentInstructionActivity::class.java).apply {
            putExtra("session_id", sessionId)
        }
        startActivity(intent)
        finish()
    }

    private fun navigateToLoading(imagePath: String) {
        Log.d(TAG, "Navigating to Loading with imagePath: $imagePath")
        val intent = Intent(this, LoadingActivity::class.java).apply {
            putExtra("session_id", sessionId)
            putExtra("page_index", pageIndex)
            putExtra("image_path", imagePath)
            putExtra("is_cover", isCover)
            putExtra("lang", lang)
        }
        loadingLauncher.launch(intent)
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume - cameraLaunched=$cameraLaunched")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause")
    }
}