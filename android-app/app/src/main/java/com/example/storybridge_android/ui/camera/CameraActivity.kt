package com.example.storybridge_android.ui.camera

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.storybridge_android.R
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import java.io.File
import java.io.FileOutputStream

class CameraActivity : AppCompatActivity() {

    private var scanner: GmsDocumentScanner? = null
    private lateinit var scannerLauncher: ActivityResultLauncher<IntentSenderRequest>
    private lateinit var cameraPermissionLauncher: ActivityResultLauncher<String>
    private var retryCount = 0
    private val MAX_RETRIES = 2
    private var isModuleInstalling = false

    companion object {
        private const val TAG = "CameraActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        Log.d(TAG, "=== CameraActivity onCreate ===")
        Log.d(TAG, "Android Version: ${Build.VERSION.SDK_INT}")
        Log.d(TAG, "Device: ${Build.MANUFACTURER} ${Build.MODEL}")

        // 🔹 권한 요청 launcher 먼저 등록
        initPermissionLauncher()

        // 🔹 스캐너 결과 launcher 등록
        initScannerLauncher()

        // 🔹 Google Play Services 확인
        if (!checkGooglePlayServices()) {
            Log.e(TAG, "Google Play Services not available")
            Toast.makeText(this, "Google Play Services is required", Toast.LENGTH_LONG).show()
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        // 🔹 권한 확인 및 요청
        checkAndRequestPermission()
    }

    private fun initPermissionLauncher() {
        cameraPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            Log.d(TAG, "Camera permission result: $isGranted")
            if (isGranted) {
                checkModuleAvailability()
            } else {
                Log.w(TAG, "Camera permission denied by user")
                Toast.makeText(this, "Camera permission is required to scan documents", Toast.LENGTH_LONG).show()
                setResult(RESULT_CANCELED)
                finish()
            }
        }
    }

    private fun checkAndRequestPermission() {
        Log.d(TAG, "Checking camera permission...")
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                Log.d(TAG, "✓ Camera permission already granted")
                checkModuleAvailability()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                Log.d(TAG, "Showing permission rationale")
                Toast.makeText(
                    this,
                    "Camera permission is needed to scan documents",
                    Toast.LENGTH_LONG
                ).show()
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            else -> {
                Log.d(TAG, "Requesting camera permission")
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun checkGooglePlayServices(): Boolean {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this)

        Log.d(TAG, "Checking Google Play Services...")
        Log.d(TAG, "Status code: $resultCode (SUCCESS=0)")

        if (resultCode != ConnectionResult.SUCCESS) {
            Log.e(TAG, "✗ Google Play Services not available: ${getPlayServicesErrorName(resultCode)}")
            if (googleApiAvailability.isUserResolvableError(resultCode)) {
                googleApiAvailability.getErrorDialog(this, resultCode, 9000)?.show()
            }
            return false
        }
        Log.d(TAG, "✓ Google Play Services available")
        return true
    }

    private fun getPlayServicesErrorName(errorCode: Int): String {
        return when (errorCode) {
            ConnectionResult.SERVICE_MISSING -> "SERVICE_MISSING"
            ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED -> "SERVICE_VERSION_UPDATE_REQUIRED"
            ConnectionResult.SERVICE_DISABLED -> "SERVICE_DISABLED"
            ConnectionResult.SERVICE_INVALID -> "SERVICE_INVALID"
            else -> "ERROR_$errorCode"
        }
    }

    private fun checkModuleAvailability() {
        if (isModuleInstalling) {
            Log.d(TAG, "Module installation already in progress, skipping check")
            return
        }

        Log.d(TAG, "=== Checking ML Kit Module Availability ===")

        val moduleInstallClient = ModuleInstall.getClient(this)
        val options = GmsDocumentScannerOptions.Builder().build()
        val optionalModuleApi = GmsDocumentScanning.getClient(options)

        moduleInstallClient
            .areModulesAvailable(optionalModuleApi)
            .addOnSuccessListener { response ->
                if (response.areModulesAvailable()) {
                    Log.d(TAG, "✓ ML Kit document scanner module is available")
                    initScannerAndLaunch()
                } else {
                    Log.w(TAG, "✗ ML Kit document scanner module not available")
                    Log.d(TAG, "Requesting module installation...")
                    requestModuleInstall()
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "✗ Failed to check module availability", e)
                Log.e(TAG, "Error type: ${e.javaClass.simpleName}")
                Log.e(TAG, "Error message: ${e.message}")

                // 모듈 체크 실패 시에도 스캐너 초기화 시도
                Log.d(TAG, "Attempting to initialize scanner despite check failure...")
                initScannerAndLaunch()
            }
    }

    private fun requestModuleInstall() {
        if (isModuleInstalling) {
            Log.d(TAG, "Module installation already in progress")
            return
        }

        isModuleInstalling = true
        Log.d(TAG, "=== Starting ML Kit Module Installation ===")
        Toast.makeText(this, "Downloading scanner module, please wait...", Toast.LENGTH_LONG).show()

        val options = GmsDocumentScannerOptions.Builder().build()
        val optionalModuleApi = GmsDocumentScanning.getClient(options)

        // Simplified: No progress listener, just install the module
        val moduleInstallRequest = ModuleInstallRequest.newBuilder()
            .addApi(optionalModuleApi)
            .build()

        ModuleInstall.getClient(this)
            .installModules(moduleInstallRequest)
            .addOnSuccessListener {
                Log.d(TAG, "✓ Module installation completed successfully")
                isModuleInstalling = false
                Toast.makeText(this, "Scanner ready", Toast.LENGTH_SHORT).show()

                // 설치 후 약간의 지연을 두고 스캐너 초기화
                Handler(mainLooper).postDelayed({
                    if (!isFinishing && !isDestroyed) {
                        initScannerAndLaunch()
                    }
                }, 500)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "✗ Module installation failed", e)
                Log.e(TAG, "Error type: ${e.javaClass.simpleName}")
                Log.e(TAG, "Error message: ${e.message}")
                isModuleInstalling = false

                // 설치 실패해도 스캐너 초기화 시도 (이미 설치되어 있을 수 있음)
                Log.d(TAG, "Attempting to initialize scanner despite install failure...")
                initScannerAndLaunch()
            }
    }

    private fun initScannerAndLaunch() {
        Log.d(TAG, "=== Initializing Scanner (attempt ${retryCount + 1}/$MAX_RETRIES) ===")

        // 🔹 이전 스캐너 인스턴스 정리
        scanner = null

        try {
            val options = GmsDocumentScannerOptions.Builder()
                .setGalleryImportAllowed(false)
                .setPageLimit(1)
                .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_BASE)
                .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
                .build()

            Log.d(TAG, "Scanner options configured:")
            Log.d(TAG, "  - Gallery import: false")
            Log.d(TAG, "  - Page limit: 1")
            Log.d(TAG, "  - Scanner mode: BASE")
            Log.d(TAG, "  - Result format: JPEG")

            // 🔹 새로운 스캐너 인스턴스 생성
            scanner = GmsDocumentScanning.getClient(options)
            Log.d(TAG, "✓ Scanner client created")

            // 짧은 지연 후 실행 (ML Kit 초기화 시간 확보)
            Handler(mainLooper).postDelayed({
                if (!isFinishing && !isDestroyed) {
                    launchScanner()
                }
            }, 300)

        } catch (e: Exception) {
            Log.e(TAG, "✗ Error creating scanner", e)
            Log.e(TAG, "Exception type: ${e.javaClass.simpleName}")
            Log.e(TAG, "Exception message: ${e.message}")
            e.printStackTrace()
            handleScannerError(e)
        }
    }

    private fun initScannerLauncher() {
        scannerLauncher = registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            Log.d(TAG, "=== Scanner Result Received ===")
            Log.d(TAG, "Result code: ${result.resultCode} (OK=${RESULT_OK}, CANCELED=${RESULT_CANCELED})")

            when (result.resultCode) {
                RESULT_OK -> {
                    Log.d(TAG, "✓ Scanning completed successfully")
                    val scanningResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
                    handleScanningResult(scanningResult)
                }
                RESULT_CANCELED -> {
                    Log.d(TAG, "Scanning was cancelled by user")
                    setResult(RESULT_CANCELED)
                    finish()
                }
                else -> {
                    Log.e(TAG, "✗ Unexpected result code: ${result.resultCode}")
                    Toast.makeText(this, "Scanning failed", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_CANCELED)
                    finish()
                }
            }
        }
    }

    private fun handleScanningResult(scanningResult: GmsDocumentScanningResult?) {
        Log.d(TAG, "=== Processing Scanning Result ===")

        if (scanningResult == null) {
            Log.e(TAG, "✗ Scanning result is null")
            Toast.makeText(this, "Failed to get scan result", Toast.LENGTH_SHORT).show()
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        val pages = scanningResult.getPages()
        Log.d(TAG, "Number of pages scanned: ${pages?.size ?: 0}")

        val page = pages?.firstOrNull()
        if (page != null) {
            val imageUri = page.getImageUri()
            Log.d(TAG, "✓ Image URI obtained: $imageUri")
            saveImageAndReturn(imageUri)
        } else {
            Log.e(TAG, "✗ No pages in scanning result")
            Toast.makeText(this, "No image captured", Toast.LENGTH_SHORT).show()
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    private fun launchScanner() {
        if (scanner == null) {
            Log.e(TAG, "✗ Scanner is null, cannot launch")
            handleScannerError(Exception("Scanner not initialized"))
            return
        }

        Log.d(TAG, "=== Launching Scanner UI ===")

        scanner!!.getStartScanIntent(this)
            .addOnSuccessListener { intentSender ->
                Log.d(TAG, "✓ Scanner intent obtained successfully")
                try {
                    scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                    retryCount = 0 // 성공하면 재시도 카운터 리셋
                    Log.d(TAG, "✓ Scanner UI launched")
                } catch (e: Exception) {
                    Log.e(TAG, "✗ Failed to launch scanner UI", e)
                    Log.e(TAG, "Exception type: ${e.javaClass.simpleName}")
                    handleScannerError(e)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "✗ Failed to get scanner intent", e)
                Log.e(TAG, "Error type: ${e.javaClass.simpleName}")
                Log.e(TAG, "Error message: ${e.message}")
                e.printStackTrace()
                handleScannerError(e)
            }
    }

    private fun handleScannerError(e: Exception) {
        Log.e(TAG, "=== Scanner Error Occurred ===")
        Log.e(TAG, "Error message: ${e.message}")
        Log.e(TAG, "Error type: ${e.javaClass.simpleName}")
        e.printStackTrace()

        val errorMessage = when {
            e.message?.contains("MlKitException", ignoreCase = true) == true ->
                "ML Kit module error"
            e.message?.contains("MODULE_NOT_AVAILABLE", ignoreCase = true) == true ->
                "Scanner module not downloaded"
            e.message?.contains("SERVICE_", ignoreCase = true) == true ->
                "Google Play Services error"
            e.message?.contains("NETWORK_ERROR", ignoreCase = true) == true ->
                "Network connection needed"
            e.message?.contains("not initialized", ignoreCase = true) == true ->
                "Scanner not initialized"
            else -> "Scanner error"
        }

        Log.e(TAG, "Categorized as: $errorMessage")

        // 🔹 재시도 로직
        if (retryCount < MAX_RETRIES) {
            retryCount++
            Log.d(TAG, "=== Retry Attempt $retryCount/$MAX_RETRIES ===")
            Toast.makeText(this, "Initializing scanner, please wait...", Toast.LENGTH_SHORT).show()

            // 재시도 전에 더 긴 대기 시간
            Handler(mainLooper).postDelayed({
                if (!isFinishing && !isDestroyed) {
                    initScannerAndLaunch()
                }
            }, 1000)
        } else {
            // 최대 재시도 횟수 초과
            Log.e(TAG, "✗ Maximum retry attempts reached")

            val userMessage = when {
                e.message?.contains("MlKitException", ignoreCase = true) == true ->
                    "ML Kit initialization failed. Please restart the app."
                e.message?.contains("SERVICE_", ignoreCase = true) == true ->
                    "Please update Google Play Services and try again."
                e.message?.contains("MODULE_NOT_AVAILABLE", ignoreCase = true) == true ->
                    "Scanner module unavailable. Please check your internet connection and try again."
                else -> "Scanner failed to start. Please try again."
            }

            Toast.makeText(this, userMessage, Toast.LENGTH_LONG).show()
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    private fun saveImageAndReturn(imageUri: Uri) {
        Log.d(TAG, "=== Saving Scanned Image ===")
        Log.d(TAG, "Source URI: $imageUri")

        try {
            val inputStream = contentResolver.openInputStream(imageUri)
            if (inputStream == null) {
                Log.e(TAG, "✗ Failed to open input stream for URI: $imageUri")
                Toast.makeText(this, "Failed to read scanned image", Toast.LENGTH_SHORT).show()
                setResult(RESULT_CANCELED)
                finish()
                return
            }

            // 🔹 내부 저장소 사용
            val file = File(filesDir, "scan_${System.currentTimeMillis()}.jpg")
            Log.d(TAG, "Destination path: ${file.absolutePath}")

            val outputStream = FileOutputStream(file)

            val bytesWritten = inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()

            Log.d(TAG, "✓ Image saved successfully")
            Log.d(TAG, "File path: ${file.absolutePath}")
            Log.d(TAG, "File size: ${file.length()} bytes")
            Log.d(TAG, "Bytes written: $bytesWritten")

            if (!file.exists() || file.length() == 0L) {
                Log.e(TAG, "✗ File validation failed")
                Log.e(TAG, "File exists: ${file.exists()}")
                Log.e(TAG, "File size: ${file.length()}")
                Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show()
                setResult(RESULT_CANCELED)
                finish()
                return
            }

            val intent = Intent()
            intent.putExtra("image_path", file.absolutePath)
            setResult(RESULT_OK, intent)

            Log.d(TAG, "=== Returning to caller with success ===")
            finish()

        } catch (e: Exception) {
            Log.e(TAG, "✗ Error saving image", e)
            Log.e(TAG, "Exception type: ${e.javaClass.simpleName}")
            Log.e(TAG, "Exception message: ${e.message}")
            e.printStackTrace()
            Toast.makeText(this, "Failed to save image: ${e.message}", Toast.LENGTH_LONG).show()
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scanner = null
        Log.d(TAG, "=== Activity destroyed ===")
    }

    override fun onStop() {
        super.onStop()
        // Activity가 백그라운드로 갈 때 스캐너 정리
        scanner = null
        Log.d(TAG, "Activity stopped (backgrounded)")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "Activity resumed")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "Activity paused")
    }
}