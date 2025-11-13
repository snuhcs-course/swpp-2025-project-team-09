package com.example.storybridge_android.ui.camera

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.storybridge_android.R
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import kotlinx.coroutines.flow.collectLatest

class CameraActivity : AppCompatActivity() {

    private val viewModel: CameraViewModel by viewModels {
        CameraViewModelFactory(application)
    }

    private lateinit var scannerLauncher: ActivityResultLauncher<IntentSenderRequest>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>

    companion object {
        private const val TAG = "CameraActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_camera)

        initLaunchers()
        observeUiState()

        if (!viewModel.checkGooglePlayServices()) {
            Toast.makeText(this, "Google Play Services required", Toast.LENGTH_LONG).show()
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        checkPermissionAndStart()
    }

    private fun initLaunchers() {
        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) viewModel.checkModuleAndInitScanner()
            else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
                setResult(RESULT_CANCELED)
                finish()
            }
        }

        scannerLauncher = registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val scanningResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
                viewModel.handleScanningResult(scanningResult, contentResolver)
            } else {
                // User cancelled the scan - return to previous activity
                Toast.makeText(this, "Scan canceled", Toast.LENGTH_SHORT).show()
                setResult(RESULT_CANCELED)
                finish()
            }
        }
    }

    private fun checkPermissionAndStart() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED -> {
                viewModel.checkModuleAndInitScanner()
            }
            else -> permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun observeUiState() {
        lifecycleScope.launchWhenStarted {
            viewModel.uiState.collectLatest { state ->
                when {
                    state.isInstalling -> Log.d(TAG, "Installing scanner module...")
                    state.isReady -> {
                        Log.d(TAG, "Scanner ready → launching once")
                        viewModel.consumeReadyFlag()
                        startScan()
                    }
                    state.imagePath != null -> {
                        val intent = Intent().apply {
                            putExtra("image_path", state.imagePath)
                        }
                        setResult(RESULT_OK, intent)
                        finish()
                    }
                    state.error != null -> {
                        Toast.makeText(this@CameraActivity, state.error, Toast.LENGTH_SHORT).show()
                        setResult(RESULT_CANCELED)
                        finish()
                    }
                }
            }
        }
    }

    private fun startScan() {
        viewModel.prepareScannerIntent(
            activity = this,
            onReady = { intentSender ->
                val request = IntentSenderRequest.Builder(intentSender).build()
                scannerLauncher.launch(request)
            },
            onError = { err ->
                Toast.makeText(this, err, Toast.LENGTH_SHORT).show()
                setResult(RESULT_CANCELED)
                finish()
            }
        )
    }
}