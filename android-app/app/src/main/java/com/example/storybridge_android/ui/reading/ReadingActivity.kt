package com.example.storybridge_android.ui.reading

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.storybridge_android.R
import com.example.storybridge_android.network.GetImageResponse
import com.example.storybridge_android.network.GetOcrTranslationResponse
import com.example.storybridge_android.network.GetTtsResponse
import com.example.storybridge_android.network.RetrofitClient
import com.example.storybridge_android.ui.camera.CameraSessionActivity
import com.example.storybridge_android.ui.session.FinishActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min

class ReadingActivity : AppCompatActivity() {

    private lateinit var sessionId: String
    private var pageIndex: Int = 0
    private val pageApi = RetrofitClient.pageApi
    private lateinit var mainLayout: ConstraintLayout
    private lateinit var topUi: View
    private lateinit var bottomUi: View
    private var uiVisible = false
    private lateinit var overlay: ConstraintLayout
    private lateinit var dimBackground: View
    private var isOverlayVisible = false
    private var mediaPlayer: MediaPlayer? = null
    private var pageBitmap: Bitmap? = null
    private lateinit var pageImage: ImageView
    private val handler = Handler(Looper.getMainLooper())
    private var isTtsPolling = false
    private val TTS_POLL_INTERVAL = 2000L

    companion object {
        private const val TAG = "ReadingActivity"
    }

    // Audio data storage per bounding box
    private var audioResultsMap: Map<Int, List<String>> = emptyMap()
    private var currentPlayingIndex: Int = -1
    private var currentAudioIndex: Int = 0

    // Play button references for state changes
    private val playButtonsMap: MutableMap<Int, ImageButton> = mutableMapOf()

    // Bounding box references for dragging
    private val boundingBoxViewsMap: MutableMap<Int, TextView> = mutableMapOf()

    // Cached OCR results for adding buttons after TTS load
    private var cachedBoundingBoxes: List<BoundingBox> = emptyList()

    // Touch and drag constant
    private val TOUCH_SLOP = 10f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_reading)

        Log.d(TAG, "=== ReadingActivity onCreate ===")

        initViews()
        initUiState()
        initListeners()

        sessionId = intent.getStringExtra("session_id") ?: ""
        pageIndex = intent.getIntExtra("page_index", 0)

        Log.d(TAG, "Session ID: $sessionId")
        Log.d(TAG, "Page index: $pageIndex")

        fetchPageData()
    }

    private fun initViews() {
        mainLayout = findViewById(R.id.main)
        topUi = findViewById(R.id.topUi)
        bottomUi = findViewById(R.id.bottomUi)
        overlay = findViewById(R.id.sideOverlay)
        dimBackground = findViewById(R.id.dimBackground)
        pageImage = findViewById(R.id.pageImage)

        // Hide global play button
        findViewById<ImageButton>(R.id.playButton).visibility = View.GONE
    }

    private fun initUiState() {
        topUi.post { topUi.translationY = -topUi.height.toFloat() }
        bottomUi.post { bottomUi.translationY = bottomUi.height.toFloat() }
    }

    private fun initListeners() {
        val startButton = findViewById<Button>(R.id.startButton)
        val menuButton = findViewById<ImageButton>(R.id.menuButton)
        val closeButton = findViewById<Button>(R.id.closeOverlayButton)
        val finishButton = findViewById<Button>(R.id.finishButton)

        mainLayout.setOnClickListener { toggleUI() }

        startButton.setOnClickListener { navigateToCamera() }
        menuButton.setOnClickListener { toggleOverlay(true) }
        closeButton.setOnClickListener { toggleOverlay(false) }
        dimBackground.setOnClickListener { toggleOverlay(false) }
        finishButton.setOnClickListener { navigateToFinish() }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupBoundingBoxTouchListener(boxView: TextView, boxIndex: Int) {
        var boxLastTouchX = 0f
        var boxLastTouchY = 0f
        var boxIsDragging = false

        boxView.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    boxLastTouchX = event.rawX
                    boxLastTouchY = event.rawY
                    boxIsDragging = false
                    return@setOnTouchListener true
                }

                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - boxLastTouchX
                    val deltaY = event.rawY - boxLastTouchY

                    if (!boxIsDragging && (Math.abs(deltaX) > TOUCH_SLOP || Math.abs(deltaY) > TOUCH_SLOP)) {
                        boxIsDragging = true
                    }

                    if (boxIsDragging) {
                        boxView.translationX += deltaX
                        boxView.translationY += deltaY

                        playButtonsMap[boxIndex]?.let { button ->
                            button.translationX += deltaX
                            button.translationY += deltaY
                        }

                        boxLastTouchX = event.rawX
                        boxLastTouchY = event.rawY
                    }
                    return@setOnTouchListener true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    boxIsDragging = false
                    return@setOnTouchListener true
                }

                else -> return@setOnTouchListener false
            }
        }
    }

    private fun navigateToFinish() {
        val intent = Intent(this, FinishActivity::class.java)
        intent.putExtra("session_id", sessionId)
        startActivity(intent)
        finish()
    }

    private fun navigateToCamera() {
        val intent = Intent(this, CameraSessionActivity::class.java)
        intent.putExtra("session_id", sessionId)
        intent.putExtra("page_index", pageIndex + 1)
        startActivity(intent)
        finish()
    }

    private fun displayPage(base64Image: String?) {
        try {
            val decodedBytes = Base64.decode(base64Image, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            pageBitmap = bitmap
            pageImage.setImageBitmap(bitmap)
            Log.d(TAG, "✓ Page image displayed")
        } catch (e: Exception) {
            Log.e(TAG, "✗ Error displaying page image", e)
            e.printStackTrace()
        }
    }

    data class BoundingBox(
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int,
        val text: String,
        val index: Int
    )

    private fun displayBB(bboxes: List<BoundingBox>) {
        Log.d(TAG, "=== Displaying Bounding Boxes ===")
        Log.d(TAG, "Number of boxes: ${bboxes.size}")

        if (pageImage.drawable == null) {
            Log.e(TAG, "✗ Page image not loaded yet")
            return
        }

        // Remove existing boxes and buttons
        for (i in mainLayout.childCount - 1 downTo 0) {
            val child = mainLayout.getChildAt(i)
            if (child.tag == "bbox" || child.tag == "play_button") {
                mainLayout.removeViewAt(i)
            }
        }

        playButtonsMap.clear()
        boundingBoxViewsMap.clear()

        val imageMatrix = pageImage.imageMatrix

        for (box in bboxes) {
            Log.d(TAG, "Processing box ${box.index}: '${box.text.take(20)}...'")

            val rect = RectF(
                box.x.toFloat(),
                box.y.toFloat(),
                (box.x + box.width).toFloat(),
                (box.y + box.height).toFloat()
            )
            imageMatrix.mapRect(rect)

            val boxView = TextView(this).apply {
                text = box.text
                setBackgroundColor(getColor(R.color.black_50))
                setTextColor(getColor(R.color.white))
                gravity = Gravity.START or Gravity.TOP
                setPadding(8, 8, 8, 8)
                tag = "bbox"
            }

            val baseTextSize = 14f
            boxView.textSize = baseTextSize
            boxView.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            val measuredWidth = boxView.measuredWidth.toFloat()
            val measuredHeight = boxView.measuredHeight.toFloat()

            val desiredWidth = rect.width()
            val desiredHeight = rect.height()

            val widthRatio = desiredWidth / measuredWidth
            val heightRatio = desiredHeight / measuredHeight
            val scaleRatio = min(widthRatio, heightRatio)

            val newTextSize = (baseTextSize * scaleRatio).coerceIn(16f, 30f)
            boxView.textSize = newTextSize

            boxView.measure(
                View.MeasureSpec.makeMeasureSpec(desiredWidth.toInt(), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )

            val finalTextWidth = desiredWidth
            val finalTextHeight = boxView.measuredHeight.toFloat()

            val params = ConstraintLayout.LayoutParams(finalTextWidth.toInt(), finalTextHeight.toInt())
            params.startToStart = pageImage.id
            params.topToTop = pageImage.id
            boxView.layoutParams = params
            boxView.translationX = rect.left
            boxView.translationY = rect.top
            mainLayout.addView(boxView)

            boundingBoxViewsMap[box.index] = boxView

            setupBoundingBoxTouchListener(boxView, box.index)

            // Create play button if audio exists for this box
            if (audioResultsMap.containsKey(box.index)) {
                Log.d(TAG, "Box ${box.index} has audio, creating play button")
                val textRect = RectF(
                    rect.left,
                    rect.top,
                    rect.left + finalTextWidth,
                    rect.top + finalTextHeight
                )
                createPlayButton(box.index, textRect, pageImage.id)
            } else {
                Log.d(TAG, "Box ${box.index} has NO audio")
            }
        }

        Log.d(TAG, "✓ Displayed ${bboxes.size} boxes and ${playButtonsMap.size} play buttons")
    }

    private fun createPlayButton(bboxIndex: Int, rect: RectF, pageImageId: Int) {
        Log.d(TAG, "Creating play button for box $bboxIndex at position (${rect.right}, ${rect.bottom})")

        val playButton = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_media_play)
            setBackgroundResource(R.drawable.circle_dark)
            alpha = 0.9f
            tag = "play_button"
            contentDescription = "Play audio for box $bboxIndex"
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setPadding(8, 8, 8, 8)
        }

        val buttonSize = (54 * resources.displayMetrics.density).toInt()
        val params = ConstraintLayout.LayoutParams(buttonSize, buttonSize)
        params.startToStart = pageImageId
        params.topToTop = pageImageId

        playButton.layoutParams = params

        playButton.translationX = rect.right - buttonSize/2 + 4
        playButton.translationY = rect.bottom - buttonSize/2 + 4

        playButton.elevation = 8f

        playButton.setOnClickListener {
            Log.d(TAG, "Play button clicked for box $bboxIndex")
            playAudioForBox(bboxIndex)
        }

        mainLayout.addView(playButton)

        playButtonsMap[bboxIndex] = playButton

        Log.d(TAG, "✓ Play button created for box $bboxIndex")
    }

    private fun updateButtonToPaused(bboxIndex: Int) {
        playButtonsMap[bboxIndex]?.apply {
            setImageResource(android.R.drawable.ic_media_play)
            setBackgroundResource(R.drawable.circle_dark)
            alpha = 1.0f
            Log.d(TAG, "✓ Button $bboxIndex set to paused state")
        }
    }

    private fun playAudioForBox(bboxIndex: Int) {
        Log.d(TAG, "=== Playing Audio for Box $bboxIndex ===")

        if (currentPlayingIndex == bboxIndex) {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
                updateButtonToPaused(bboxIndex)
                Log.d(TAG, "Audio paused for box $bboxIndex at ${mediaPlayer?.currentPosition}")
            } else if (mediaPlayer != null) {
                mediaPlayer?.start()
                updateButtonToPlaying(bboxIndex)
                Log.d(TAG, "Audio resumed for box $bboxIndex")
            }
            return
        }

        val audioList = audioResultsMap[bboxIndex]
        if (audioList == null) {
            Log.e(TAG, "✗ No audio found for box $bboxIndex")
            return
        }

        Log.d(TAG, "Audio list size: ${audioList.size}")

        // Reset previous button state
        if (currentPlayingIndex != -1 && currentPlayingIndex != bboxIndex) {
            resetButtonState(currentPlayingIndex)
        }

        mediaPlayer?.release()
        mediaPlayer = null

        currentPlayingIndex = bboxIndex
        currentAudioIndex = 0

        updateButtonToPlaying(bboxIndex)

        playNextAudioInBox(audioList)
    }

    private fun playNextAudioInBox(audioList: List<String>) {
        if (currentAudioIndex >= audioList.size) {
            Log.d(TAG, "✓ All audio clips played for box $currentPlayingIndex")
            resetButtonState(currentPlayingIndex)
            currentAudioIndex = 0
            currentPlayingIndex = -1
            return
        }

        Log.d(TAG, "Playing audio clip ${currentAudioIndex + 1}/${audioList.size} for box $currentPlayingIndex")

        val base64Audio = audioList[currentAudioIndex]
        val audioFile = File(cacheDir, "temp_audio_${currentPlayingIndex}_$currentAudioIndex.mp3")

        try {
            val audioBytes = Base64.decode(base64Audio, Base64.DEFAULT)
            FileOutputStream(audioFile).use { it.write(audioBytes) }

            Log.d(TAG, "Audio file saved: ${audioFile.absolutePath}, size: ${audioFile.length()} bytes")

            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(audioFile.absolutePath)
                setOnPreparedListener {
                    Log.d(TAG, "MediaPlayer prepared, starting playback")
                    start()
                }
                setOnCompletionListener {
                    Log.d(TAG, "Audio clip completed")
                    currentAudioIndex++
                    playNextAudioInBox(audioList)
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    resetButtonState(currentPlayingIndex)
                    currentPlayingIndex = -1
                    true
                }
                prepareAsync()
            }

        } catch (e: Exception) {
            Log.e(TAG, "✗ Error playing audio", e)
            e.printStackTrace()
            resetButtonState(currentPlayingIndex)
            currentPlayingIndex = -1
        }
    }

    private fun updateButtonToPlaying(bboxIndex: Int) {
        playButtonsMap[bboxIndex]?.apply {
            setImageResource(android.R.drawable.ic_media_pause)
            setBackgroundResource(R.drawable.circle_dark)
            alpha = 1.0f
            Log.d(TAG, "✓ Button $bboxIndex set to playing state")
        }
    }

    private fun resetButtonState(bboxIndex: Int) {
        playButtonsMap[bboxIndex]?.apply {
            setImageResource(android.R.drawable.ic_media_play)
            setBackgroundResource(R.drawable.circle_dark)
            alpha = 0.9f
            Log.d(TAG, "✓ Button $bboxIndex reset to default state")
        }
    }

    private fun toggleUI() {
        if (uiVisible) {
            topUi.animate().translationY(-topUi.height.toFloat()).setDuration(300).start()
            bottomUi.animate().translationY(bottomUi.height.toFloat()).setDuration(300).start()
        } else {
            topUi.animate().translationY(0f).setDuration(300).start()
            bottomUi.animate().translationY(0f).setDuration(300).start()
        }
        uiVisible = !uiVisible
    }

    private fun toggleOverlay(show: Boolean) {
        if (show && !isOverlayVisible) {
            overlay.visibility = View.VISIBLE
            dimBackground.visibility = View.VISIBLE
            overlay.animate().translationX(0f).setDuration(300).start()
            isOverlayVisible = true
        } else if (!show && isOverlayVisible) {
            overlay.animate()
                .translationX(-overlay.width.toFloat())
                .setDuration(300)
                .withEndAction {
                    overlay.visibility = View.GONE
                    dimBackground.visibility = View.GONE
                }
                .start()
            isOverlayVisible = false
        }
    }

    // Server communication

    private fun fetchPageData() {
        Log.d(TAG, "=== Fetching Page Data ===")
        fetchImage()
        fetchOcrResults()
        fetchTtsResults()
        startTtsPolling()
    }

    private fun fetchImage() {
        Log.d(TAG, "Fetching image...")
        pageApi.getImage(sessionId, pageIndex).enqueue(object : Callback<GetImageResponse> {
            override fun onResponse(call: Call<GetImageResponse>, response: Response<GetImageResponse>) {
                if (response.isSuccessful) {
                    Log.d(TAG, "✓ Image fetched successfully")
                    displayPage(response.body()?.image_base64)
                } else {
                    Log.e(TAG, "✗ Image fetch failed: ${response.code()}")
                }
            }
            override fun onFailure(call: Call<GetImageResponse>, t: Throwable) {
                Log.e(TAG, "✗ Image fetch error", t)
                t.printStackTrace()
            }
        })
    }

    private fun fetchOcrResults() {
        Log.d(TAG, "Fetching OCR results...")
        pageApi.getOcrResults(sessionId, pageIndex).enqueue(object :
            Callback<GetOcrTranslationResponse> {
            override fun onResponse(call: Call<GetOcrTranslationResponse>, response: Response<GetOcrTranslationResponse>) {
                if (response.isSuccessful) {
                    Log.d(TAG, "✓ OCR results fetched successfully")
                    val ocrList = response.body()?.ocr_results
                    Log.d(TAG, "OCR results count: ${ocrList?.size ?: 0}")

                    val boxes = ocrList?.mapIndexed { index, ocrBox ->
                        ocrBox.bbox.let { box ->
                            BoundingBox(box.x, box.y, box.width, box.height, ocrBox.translation_txt, index)
                        }
                    } ?: emptyList()

                    cachedBoundingBoxes = boxes

                    if (boxes.isNotEmpty()) {
                        pageImage.post {
                            displayBB(boxes)
                        }
                    } else {
                        Log.w(TAG, "No bounding boxes to display")
                    }
                } else {
                    Log.e(TAG, "✗ OCR fetch failed: ${response.code()}")
                }
            }
            override fun onFailure(call: Call<GetOcrTranslationResponse>, t: Throwable) {
                Log.e(TAG, "✗ OCR fetch error", t)
            }
        })
    }

    private fun fetchTtsResults() {
        Log.d(TAG, "Fetching TTS results...")
        pageApi.getTtsResults(sessionId, pageIndex).enqueue(object : Callback<GetTtsResponse> {
            override fun onResponse(call: Call<GetTtsResponse>, response: Response<GetTtsResponse>) {
                if (response.isSuccessful) {
                    Log.d(TAG, "✓ TTS results fetched successfully")
                    val audioList = response.body()?.audio_results
                    Log.d(TAG, "TTS results count: ${audioList?.size ?: 0}")

                    if (!audioList.isNullOrEmpty()) {
                        // Create map with bbox_index as key
                        audioResultsMap = audioList.associate { audioResult ->
                            Log.d(TAG, "Audio for bbox ${audioResult.bbox_index}: ${audioResult.audio_base64_list.size} clips")
                            audioResult.bbox_index to audioResult.audio_base64_list
                        }

                        // If OCR results already displayed, add play buttons
                        if (cachedBoundingBoxes.isNotEmpty()) {
                            pageImage.post {
                                Log.d(TAG, "Re-displaying bounding boxes with audio buttons")
                                displayBB(cachedBoundingBoxes)
                            }
                        }
                    } else {
                        Log.w(TAG, "No TTS results available")
                    }
                } else {
                    Log.e(TAG, "✗ TTS fetch failed: ${response.code()}")
                }
            }
            override fun onFailure(call: Call<GetTtsResponse>, t: Throwable) {
                Log.e(TAG, "✗ TTS fetch error", t)
            }
        })
    }

    private fun startTtsPolling() {
        if (isTtsPolling) {
            Log.d(TAG, "TTS polling already active")
            return
        }

        isTtsPolling = true
        Log.d(TAG, "=== Starting TTS Polling ===")
        pollTtsStatus()
    }

    private fun pollTtsStatus() {
        if (!isTtsPolling) {
            Log.d(TAG, "TTS polling stopped")
            return
        }

        Log.d(TAG, "Polling TTS status...")

        pageApi.getTtsResults(sessionId, pageIndex).enqueue(object : Callback<GetTtsResponse> {
            override fun onResponse(call: Call<GetTtsResponse>, response: Response<GetTtsResponse>) {
                if (response.isSuccessful) {
                    val audioList = response.body()?.audio_results
                    Log.d(TAG, "TTS poll: ${audioList?.size ?: 0} boxes have audio")

                    if (!audioList.isNullOrEmpty()) {
                        val oldSize = audioResultsMap.size

                        // Update audio map
                        audioResultsMap = audioList.associate { audioResult ->
                            audioResult.bbox_index to audioResult.audio_base64_list
                        }

                        val newSize = audioResultsMap.size

                        // If new audio appeared, update UI
                        if (newSize > oldSize) {
                            Log.d(TAG, "New audio detected! Updating buttons ($oldSize → $newSize)")

                            if (cachedBoundingBoxes.isNotEmpty()) {
                                pageImage.post {
                                    displayBB(cachedBoundingBoxes)
                                }
                            }
                        }

                        // Check if all boxes have audio
                        val totalBoxes = cachedBoundingBoxes.size
                        if (newSize >= totalBoxes) {
                            Log.d(TAG, "✓ All TTS complete! Stopping polling")
                            isTtsPolling = false
                            return
                        }
                    }

                    // Continue polling
                    handler.postDelayed({ pollTtsStatus() }, TTS_POLL_INTERVAL)
                } else {
                    Log.e(TAG, "✗ TTS poll failed: ${response.code()}")
                    handler.postDelayed({ pollTtsStatus() }, TTS_POLL_INTERVAL)
                }
            }

            override fun onFailure(call: Call<GetTtsResponse>, t: Throwable) {
                Log.e(TAG, "✗ TTS poll error", t)
                handler.postDelayed({ pollTtsStatus() }, TTS_POLL_INTERVAL)
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        isTtsPolling = false
        handler.removeCallbacksAndMessages(null)
        mediaPlayer?.release()
        playButtonsMap.clear()
        boundingBoxViewsMap.clear()
        Log.d(TAG, "=== Activity destroyed ===")
    }
}