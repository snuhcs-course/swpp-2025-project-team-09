package com.example.storybridge_android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.storybridge_android.databinding.ActivityCameraBinding
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import java.io.File
import java.io.FileOutputStream

class CameraActivity : AppCompatActivity() {

    private lateinit var scanner: GmsDocumentScanner
    private lateinit var scannerLauncher: ActivityResultLauncher<IntentSenderRequest>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading)

        val options = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(false)
            .setPageLimit(1)
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_BASE)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
            .build()

        scanner = GmsDocumentScanning.getClient(options)

        scannerLauncher =
            registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    result.data?.let { intent ->
                        val scanningResult = GmsDocumentScanningResult.fromActivityResultIntent(intent)
                        scanningResult?.getPages()?.let { pages ->
                            if (pages.isNotEmpty()) {
                                val firstPage = pages[0]
                                saveImageAndProceed(firstPage.getImageUri())
                            }
                        }
                    }
                } else {
                    // If the user cancels, post the re-launch to the main thread's message queue.
                    // This ensures the previous scanner activity is fully closed before starting a new one.
                    Log.d(TAG, "Scanning cancelled or failed. Relaunching scanner.")
                    Handler(Looper.getMainLooper()).post {
                        launchScanner()
                    }
                }
            }

        // Launch the scanner for the first time.
        launchScanner()
    }

    private fun launchScanner() {
        scanner.getStartScanIntent(this)
            .addOnSuccessListener { intentSender ->
                scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            }
            .addOnFailureListener { e ->
                // If the scanner itself fails to start, go back to the main activity.
                Log.e(TAG, "Scanner failed to start: ${e.message}", e)
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
            }
    }

    private fun saveImageAndProceed(imageUri: android.net.Uri) {
        val inputStream = contentResolver.openInputStream(imageUri)
        val file = File(getExternalFilesDir(null), "scanned_page_${System.currentTimeMillis()}.jpg")
        val outputStream = FileOutputStream(file)

        inputStream?.copyTo(outputStream)
        inputStream?.close()
        outputStream.close()

        Log.d(TAG, "Scan successful: ${file.absolutePath}")
        val intent = Intent(this, LoadingActivity::class.java)
        startActivity(intent)
        finish()
    }

    companion object {
        private const val TAG = "CameraActivity"
    }
}
