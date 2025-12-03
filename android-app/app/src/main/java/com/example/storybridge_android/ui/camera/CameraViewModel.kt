package com.example.storybridge_android.ui.camera

import android.app.Application
import android.content.ContentResolver
import android.content.IntentSender
import android.net.Uri
import android.os.Handler
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

data class CameraUiState(
    val isInstalling: Boolean = false,
    val isReady: Boolean = false,
    val isScanning: Boolean = false,
    val imagePath: String? = null,
    val error: String? = null
)

class CameraViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState

    private var scanner: GmsDocumentScanner? = null
    private val context get() = getApplication<Application>().applicationContext

    fun checkGooglePlayServices(): Boolean {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(context)
        if (resultCode != ConnectionResult.SUCCESS) {
            val name = when (resultCode) {
                ConnectionResult.SERVICE_MISSING -> "SERVICE_MISSING"
                ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED -> "SERVICE_VERSION_UPDATE_REQUIRED"
                ConnectionResult.SERVICE_DISABLED -> "SERVICE_DISABLED"
                else -> "ERROR_$resultCode"
            }
            _uiState.value = _uiState.value.copy(error = "Google Play Services error: $name")
            return false
        }
        return true
    }

    fun checkModuleAndInitScanner() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isInstalling = true)
            val moduleInstallClient = ModuleInstall.getClient(context)
            val options = GmsDocumentScannerOptions.Builder().build()
            val optionalModuleApi = GmsDocumentScanning.getClient(options)

            moduleInstallClient.areModulesAvailable(optionalModuleApi)
                .addOnSuccessListener { response ->
                    if (response.areModulesAvailable()) {
                        initScanner()
                    } else {
                        installModule(optionalModuleApi)
                    }
                }
                .addOnFailureListener {
                    initScanner()
                }
        }
    }

    private fun installModule(optionalModuleApi: GmsDocumentScanner) {
        _uiState.value = _uiState.value.copy(isInstalling = true)
        val request = ModuleInstallRequest.newBuilder()
            .addApi(optionalModuleApi)
            .build()

        ModuleInstall.getClient(context)
            .installModules(request)
            .addOnSuccessListener {
                Handler(context.mainLooper).postDelayed({
                    initScanner()
                }, 500)
            }
            .addOnFailureListener {
                _uiState.value = _uiState.value.copy(error = "Module install failed: ${it.message}")
            }
    }

    private fun initScanner() {
        try {
            val options = GmsDocumentScannerOptions.Builder()
                .setGalleryImportAllowed(false)
                .setPageLimit(1)
                .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_BASE)
                .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
                .build()
            scanner = GmsDocumentScanning.getClient(options)
            _uiState.value = _uiState.value.copy(isInstalling = false, isReady = true)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(error = "Scanner init failed: ${e.message}")
        }
    }

    fun prepareScannerIntent(activity: android.app.Activity, onReady: (IntentSender) -> Unit, onError: (String) -> Unit) {
        val s = scanner ?: return onError("Scanner not initialized")
        s.getStartScanIntent(activity)
            .addOnSuccessListener { intentSender -> onReady(intentSender) }
            .addOnFailureListener { e -> onError("Failed to get scanner intent: ${e.message}") }
    }

    fun handleScanningResult(scanningResult: GmsDocumentScanningResult?, contentResolver: ContentResolver) {
        if (scanningResult == null) {
            _uiState.value = _uiState.value.copy(error = "Scanning result is null")
            return
        }

        val page = scanningResult.pages?.firstOrNull()
        if (page == null) {
            _uiState.value = _uiState.value.copy(error = "No page scanned")
            return
        }

        saveImage(page.imageUri, contentResolver)
    }

    private fun saveImage(uri: Uri, contentResolver: ContentResolver) {
        try {
            val file = File(context.filesDir, "scan_${System.currentTimeMillis()}.jpg")

            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            } ?: throw Exception("Cannot open input stream")

            _uiState.value = _uiState.value.copy(imagePath = file.absolutePath)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(error = "Save failed: ${e.message}")
        }
    }

    fun consumeReadyFlag() {
        _uiState.value = _uiState.value.copy(isReady = false)
    }

}
