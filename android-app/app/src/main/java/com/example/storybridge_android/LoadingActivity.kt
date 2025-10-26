package com.example.storybridge_android

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import com.example.storybridge_android.network.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max
import androidx.core.graphics.scale

class LoadingActivity : AppCompatActivity() {

    private lateinit var loadingBar: ProgressBar
    private val handler = Handler(Looper.getMainLooper())
    private var isPolling = true
    private lateinit var sessionId: String
    private var pageIndex: Int = 0
    private var lang: String = "en"
    private var imagePath: String? = null

    companion object {
        private const val TAG = "LoadingActivity"
        // Progress ranges
        private const val UPLOAD_PROGRESS_START = 0
        private const val UPLOAD_PROGRESS_END = 80
        private const val OCR_PROGRESS_START = 80
        private const val OCR_PROGRESS_END = 100
        private const val COMPRESSION_QUALITY = 80 // JPEG compression quality (0-100)
        private const val MAX_IMAGE_DIMENSION = 1920 // Maximum width/height for scaling
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading)

        Log.d(TAG, "=== LoadingActivity onCreate ===")

        loadingBar = findViewById(R.id.loadingBar)

        sessionId = intent.getStringExtra("session_id") ?: run {
            Log.e(TAG, "✗ No session_id provided")
            finish()
            return
        }
        pageIndex = intent.getIntExtra("page_index", 0)
        lang = intent.getStringExtra("lang") ?: "en"
        imagePath = intent.getStringExtra("image_path")

        Log.d(TAG, "Session ID: $sessionId")
        Log.d(TAG, "Page index: $pageIndex")
        Log.d(TAG, "Language: $lang")
        Log.d(TAG, "Image path: $imagePath")

        if (imagePath == null) {
            Log.e(TAG, "✗ No image path provided")
            finish()
            return
        }

        // Initialize progress bar
        loadingBar.progress = UPLOAD_PROGRESS_START

        uploadImage()
    }

    /**
     * Smoothly animates progress bar to a target value
     */
    private fun animateProgressTo(target: Int, durationMs: Long = 500) {
        val start = loadingBar.progress
        val diff = target - start
        if (diff <= 0) {
            loadingBar.progress = target
            return
        }

        val steps = 20
        val stepDelay = durationMs / steps
        val increment = diff.toFloat() / steps

        var currentStep = 0
        val progressRunnable = object : Runnable {
            override fun run() {
                if (currentStep < steps && isPolling) {
                    currentStep++
                    val newProgress = start + (increment * currentStep).toInt()
                    loadingBar.progress = newProgress.coerceAtMost(target)
                    handler.postDelayed(this, stepDelay)
                } else {
                    loadingBar.progress = target
                }
            }
        }
        handler.post(progressRunnable)
    }

    /**
     * Sets progress immediately
     */
    private fun setProgressTo(progress: Int) {
        handler.removeCallbacksAndMessages(null)
        loadingBar.progress = progress
    }

    /**
     * Decodes an image file and scales it down if necessary, maintaining aspect ratio.
     * The maximum width or height will be MAX_IMAGE_DIMENSION.
     */
    private fun decodeAndScaleImage(filePath: String): Bitmap? {
        val options = BitmapFactory.Options().apply {
            // First decode to get image dimensions
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(filePath, options)

        val imageWidth = options.outWidth
        val imageHeight = options.outHeight

        // Calculate inSampleSize to scale the image down
        var inSampleSize = 1
        if (max(imageWidth, imageHeight) > MAX_IMAGE_DIMENSION) {
            val scaleFactor = max(imageWidth, imageHeight).toFloat() / MAX_IMAGE_DIMENSION
            inSampleSize = scaleFactor.toInt()
        }

        Log.d(TAG, "Image Original Size: ${imageWidth}x${imageHeight}, InSampleSize: $inSampleSize")

        // Decode with inSampleSize set
        options.inJustDecodeBounds = false
        options.inSampleSize = inSampleSize

        val scaledBitmap = BitmapFactory.decodeFile(filePath, options)

        // Final scaling if inSampleSize was not enough or to hit the target exactly
        if (scaledBitmap != null && max(scaledBitmap.width, scaledBitmap.height) > MAX_IMAGE_DIMENSION) {
            val scaleRatio = MAX_IMAGE_DIMENSION.toFloat() / max(scaledBitmap.width, scaledBitmap.height)
            val newWidth = (scaledBitmap.width * scaleRatio).toInt()
            val newHeight = (scaledBitmap.height * scaleRatio).toInt()

            val finalBitmap = scaledBitmap.scale(newWidth, newHeight)
            if (finalBitmap != scaledBitmap) {
                scaledBitmap.recycle() // Free the intermediate bitmap
            }
            Log.d(TAG, "Image Final Size: ${finalBitmap.width}x${finalBitmap.height}")
            return finalBitmap
        }

        return scaledBitmap
    }

    private fun uploadImage() {
        Log.d(TAG, "=== Uploading Image (Scaled & Compressed) ===")

        val file = File(imagePath!!)
        if (!file.exists()) {
            Log.e(TAG, "✗ Image file does not exist: $imagePath")
            finish()
            return
        }

        Log.d(TAG, "Original File size: ${file.length()} bytes")

        // Start with upload progress
        animateProgressTo(UPLOAD_PROGRESS_END, 6000)

        // --- Scaling & Compression Logic ---
        val bitmap = decodeAndScaleImage(file.absolutePath)
        if (bitmap == null) {
            Log.e(TAG, "✗ Could not decode or scale bitmap from file path")
            finish()
            return
        }

        val outputStream = ByteArrayOutputStream()
        // Compress the scaled bitmap
        bitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESSION_QUALITY, outputStream)
        val byteArray = outputStream.toByteArray()

        Log.d(TAG, "Compressed byte array size: ${byteArray.size} bytes (Quality: $COMPRESSION_QUALITY%)")

        // Encode the compressed byte array to Base64
        val base64 = Base64.encodeToString(byteArray, Base64.DEFAULT)
        bitmap.recycle() // Release the bitmap memory immediately

        Log.d(TAG, "Base64 encoded, length: ${base64.length}")
        // --- End of Scaling & Compression Logic ---

        val req = UploadImageRequest(
            session_id = sessionId,
            page_index = pageIndex,
            lang = lang,
            image_base64 = base64
        )

        Log.d(TAG, "Making API call to /process/upload...")

        RetrofitClient.processApi.uploadImage(req)
            .enqueue(object : Callback<UploadImageResponse> {
                override fun onResponse(
                    call: Call<UploadImageResponse>,
                    response: Response<UploadImageResponse>
                ) {
                    Log.d(TAG, "=== Upload Response Received ===")
                    Log.d(TAG, "Response code: ${response.code()}")

                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body != null) {
                            pageIndex = body.page_index
                            Log.d(TAG, "✓ Upload successful")
                            Log.d(TAG, "Updated page_index: $pageIndex")
                            Log.d(TAG, "Status: ${body.status}")

                            // Move to OCR phase immediately
                            setProgressTo(OCR_PROGRESS_START)
                            handler.postDelayed({
                                pollStatus()
                            }, 100)
                        } else {
                            Log.e(TAG, "✗ Upload success but body is null")
                            finish()
                        }
                    } else {
                        Log.e(TAG, "✗ Upload failed: ${response.code()}")
                        try {
                            Log.e(TAG, "Error body: ${response.errorBody()?.string()}")
                        } catch (e: Exception) {
                            Log.e(TAG, "Could not read error body", e)
                        }
                        finish()
                    }
                }

                override fun onFailure(call: Call<UploadImageResponse>, t: Throwable) {
                    Log.e(TAG, "✗ Upload error: ${t.message}", t)
                    finish()
                }
            })
    }

    private fun pollStatus() {
        Log.d(TAG, "=== Starting OCR/Translation Status Polling ===")

        val api = RetrofitClient.processApi
        var runnable: Runnable? = null
        var pollCount = 0

        runnable = Runnable {
            if (!isPolling) {
                Log.d(TAG, "Polling stopped")
                return@Runnable
            }

            pollCount++
            Log.d(TAG, "OCR/Translation Poll attempt #$pollCount")

            api.checkOcrStatus(sessionId, pageIndex)
                .enqueue(object : Callback<CheckOcrResponse> {
                    override fun onResponse(
                        call: Call<CheckOcrResponse>,
                        response: Response<CheckOcrResponse>
                    ) {
                        if (!response.isSuccessful) {
                            Log.e(TAG, "OCR status check failed: ${response.code()}")
                            handler.postDelayed(runnable!!, 1500)
                            return
                        }

                        val body = response.body()
                        if (body != null) {
                            // Map server progress (0-100) to our allocated range (30-100)
                            val serverProgress = body.progress
                            val mappedProgress = OCR_PROGRESS_START +
                                    ((serverProgress * (OCR_PROGRESS_END - OCR_PROGRESS_START)) / 100)

                            Log.d(TAG, "OCR/Translation Status: ${body.status}, Server Progress: $serverProgress%, Mapped: $mappedProgress%")

                            // Smoothly animate to the server's reported progress
                            animateProgressTo(mappedProgress, 300)

                            if (body.status == "ready") {
                                Log.d(TAG, "✓ OCR and Translation ready! Moving to ReadingActivity...")
                                setProgressTo(OCR_PROGRESS_END)
                                isPolling = false
                                handler.postDelayed({
                                    navigateToReading()
                                }, 300)
                            } else {
                                Log.d(TAG, "OCR/Translation still processing, will retry in 1 second...")
                                handler.postDelayed(runnable!!, 1000)
                            }
                        } else {
                            Log.e(TAG, "OCR response body is null")
                            handler.postDelayed(runnable!!, 1500)
                        }
                    }

                    override fun onFailure(
                        call: Call<CheckOcrResponse>,
                        t: Throwable
                    ) {
                        Log.e(TAG, "OCR polling error: ${t.message}", t)
                        handler.postDelayed(runnable!!, 1500)
                    }
                })
        }

        handler.post(runnable)
    }

    private fun navigateToReading() {
        Log.d(TAG, "=== Navigating to ReadingActivity ===")
        Log.d(TAG, "Session ID: $sessionId")
        Log.d(TAG, "Page index: $pageIndex")

        val intent = Intent(this, ReadingActivity::class.java)
        intent.putExtra("session_id", sessionId)
        intent.putExtra("page_index", pageIndex)

        Log.d(TAG, "Starting ReadingActivity...")
        startActivity(intent)

        Log.d(TAG, "Finishing LoadingActivity")
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        isPolling = false
        handler.removeCallbacksAndMessages(null)
        Log.d(TAG, "=== Activity destroyed, all progress animations stopped ===")
    }
}