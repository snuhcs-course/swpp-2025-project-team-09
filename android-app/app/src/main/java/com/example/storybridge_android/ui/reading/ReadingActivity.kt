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
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.storybridge_android.R
import com.example.storybridge_android.data.PageRepositoryImpl
import com.example.storybridge_android.network.GetImageResponse
import com.example.storybridge_android.network.GetOcrTranslationResponse
import com.example.storybridge_android.network.GetTtsResponse
import com.example.storybridge_android.ui.camera.CameraSessionActivity
import com.example.storybridge_android.ui.session.FinishActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog

class ReadingActivity : AppCompatActivity() {

    private lateinit var sessionId: String
    private var pageIndex = 0
    private var totalPages = 0
    private lateinit var mainLayout: ConstraintLayout
    private lateinit var pageImage: ImageView
    private lateinit var topUi: View
    private lateinit var bottomUi: View
    private lateinit var overlay: ConstraintLayout
    private lateinit var dimBackground: View
    private lateinit var thumbnailRecyclerView: RecyclerView

    private var uiVisible = false
    private var isOverlayVisible = false
    private val handler = Handler(Looper.getMainLooper())
    private var mediaPlayer: MediaPlayer? = null
    private var pageBitmap: Bitmap? = null
    private var isTtsPolling = false

    private val viewModel: ReadingViewModel by viewModels {
        ReadingViewModelFactory(PageRepositoryImpl())
    }

    companion object {
        private const val TAG = "ReadingActivity"
        private const val TTS_POLL_INTERVAL = 2000L
        private const val TOUCH_SLOP = 10f
    }

    private var audioResultsMap: Map<Int, List<String>> = emptyMap()
    private var currentPlayingIndex: Int = -1
    private var currentAudioIndex: Int = 0
    private val playButtonsMap: MutableMap<Int, ImageButton> = mutableMapOf()
    private val boundingBoxViewsMap: MutableMap<Int, TextView> = mutableMapOf()
    private var cachedBoundingBoxes: List<BoundingBox> = emptyList()

    private lateinit var thumbnailAdapter: ThumbnailAdapter
    private val thumbnailList = mutableListOf<PageThumbnail>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_reading)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                AlertDialog.Builder(this@ReadingActivity)
                    .setTitle("나가기")
                    .setMessage("읽기 화면을 종료할까요?")
                    .setPositiveButton("종료") { _, _ ->
                        finish()
                    }
                    .setNegativeButton("취소", null)
                    .show()
            }
        })
        initViews()
        initUiState()
        initListeners()

        sessionId = intent.getStringExtra("session_id") ?: ""
        pageIndex = intent.getIntExtra("page_index", 0)
        totalPages = intent.getIntExtra("total_pages", pageIndex + 1)

        observeViewModel()
        fetchPage()
        fetchAllThumbnails()
    }

    private fun initViews() {
        mainLayout = findViewById(R.id.main)
        pageImage = findViewById(R.id.pageImage)
        topUi = findViewById(R.id.topUi)
        bottomUi = findViewById(R.id.bottomUi)
        overlay = findViewById(R.id.sideOverlay)
        dimBackground = findViewById(R.id.dimBackground)
        thumbnailRecyclerView = findViewById(R.id.thumbnailRecyclerView)

        findViewById<ImageButton>(R.id.playButton).visibility = View.GONE

        thumbnailAdapter = ThumbnailAdapter { onThumbnailClick(it) }
        thumbnailRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ReadingActivity)
            adapter = thumbnailAdapter
        }
    }

    private fun initUiState() {
        topUi.post { topUi.translationY = -topUi.height.toFloat() }
        bottomUi.post { bottomUi.translationY = bottomUi.height.toFloat() }
    }

    private fun initListeners() {
        findViewById<Button>(R.id.startButton).setOnClickListener { navigateToCamera() }
        findViewById<ImageButton>(R.id.menuButton).setOnClickListener { toggleOverlay(true) }
        findViewById<Button>(R.id.closeOverlayButton).setOnClickListener { toggleOverlay(false) }
        findViewById<Button>(R.id.finishButton).setOnClickListener { navigateToFinish() }
        findViewById<View>(R.id.dimBackground).setOnClickListener { toggleOverlay(false) }

        findViewById<Button>(R.id.prevButton).setOnClickListener {
            if (pageIndex > 0) loadPage(pageIndex - 1)
            else Toast.makeText(this, "This is the first page", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.nextButton).setOnClickListener {
            if (pageIndex + 1 >= totalPages)
                Toast.makeText(this, "This is the last page", Toast.LENGTH_SHORT).show()
            else loadPage(pageIndex + 1)
        }

        mainLayout.setOnClickListener { toggleUI() }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                state.image?.let { displayPage(it) }
                state.ocr?.let { handleOcr(it) }
                state.tts?.let { handleTts(it) }
                state.error?.let { Toast.makeText(this@ReadingActivity, it, Toast.LENGTH_SHORT).show() }
            }
        }
    }

    private fun fetchPage() {
        viewModel.fetchPage(sessionId, pageIndex)
    }

    private fun displayPage(data: GetImageResponse) {
        try {
            val bytes = Base64.decode(data.image_base64, Base64.DEFAULT)
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            pageBitmap = bmp
            pageImage.setImageBitmap(bmp)
        } catch (e: Exception) {
            Log.e(TAG, "Image decode failed", e)
        }
    }

    private fun handleOcr(data: GetOcrTranslationResponse) {
        val boxes = data.ocr_results.mapIndexed { i, ocrBox ->
            val box = ocrBox.bbox
            BoundingBox(box.x, box.y, box.width, box.height, ocrBox.translation_txt, i)
        }
        cachedBoundingBoxes = boxes
        if (boxes.isNotEmpty()) pageImage.post { displayBB(boxes) }
    }

    private fun handleTts(data: GetTtsResponse) {
        audioResultsMap = data.audio_results.associate { it.bbox_index to it.audio_base64_list }
        if (cachedBoundingBoxes.isNotEmpty()) pageImage.post { displayBB(cachedBoundingBoxes) }
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

    @SuppressLint("ClickableViewAccessibility")
    private fun setupBoundingBoxTouchListener(boxView: TextView, boxIndex: Int) {
        var lastX = 0f
        var lastY = 0f
        var dragging = false
        boxView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastX = event.rawX
                    lastY = event.rawY
                    dragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - lastX
                    val dy = event.rawY - lastY
                    if (!dragging && (kotlin.math.abs(dx) > TOUCH_SLOP || kotlin.math.abs(dy) > TOUCH_SLOP))
                        dragging = true
                    if (dragging) {
                        boxView.translationX += dx
                        boxView.translationY += dy
                        playButtonsMap[boxIndex]?.apply {
                            translationX += dx
                            translationY += dy
                        }
                        lastX = event.rawX
                        lastY = event.rawY
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    dragging = false; true
                }
                else -> false
            }
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

            val baseTextSize = 24f
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

            val newTextSize = (baseTextSize * scaleRatio).coerceIn(24f, 36f)
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

    private fun playAudioForBox(bboxIndex: Int) {
        if (currentPlayingIndex == bboxIndex) {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
                updateButtonToPaused(bboxIndex)
            } else {
                mediaPlayer?.start()
                updateButtonToPlaying(bboxIndex)
            }
            return
        }

        val audioList = audioResultsMap[bboxIndex] ?: return
        if (currentPlayingIndex != -1) resetButtonState(currentPlayingIndex)
        mediaPlayer?.release()
        mediaPlayer = null
        currentPlayingIndex = bboxIndex
        currentAudioIndex = 0
        updateButtonToPlaying(bboxIndex)
        playNextAudio(audioList)
    }

    private fun playNextAudio(list: List<String>) {
        if (currentAudioIndex >= list.size) {
            resetButtonState(currentPlayingIndex)
            currentPlayingIndex = -1
            return
        }

        val base64Audio = list[currentAudioIndex]
        val audioFile = File(cacheDir, "temp_audio_${currentPlayingIndex}_$currentAudioIndex.mp3")
        try {
            val bytes = Base64.decode(base64Audio, Base64.DEFAULT)
            FileOutputStream(audioFile).use { it.write(bytes) }
            mediaPlayer = MediaPlayer().apply {
                setDataSource(audioFile.absolutePath)
                setOnPreparedListener { start() }
                setOnCompletionListener {
                    currentAudioIndex++
                    playNextAudio(list)
                }
                setOnErrorListener { _, _, _ ->
                    resetButtonState(currentPlayingIndex); true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Audio error", e)
            resetButtonState(currentPlayingIndex)
        }
    }

    private fun updateButtonToPlaying(bboxIndex: Int) {
        playButtonsMap[bboxIndex]?.apply {
            setImageResource(android.R.drawable.ic_media_pause)
        }
    }

    private fun updateButtonToPaused(bboxIndex: Int) {
        playButtonsMap[bboxIndex]?.apply {
            setImageResource(android.R.drawable.ic_media_play)
        }
    }

    private fun resetButtonState(bboxIndex: Int) {
        playButtonsMap[bboxIndex]?.apply {
            setImageResource(android.R.drawable.ic_media_play)
            alpha = 0.9f
        }
    }

    private fun onThumbnailClick(index: Int) {
        if (index != pageIndex) loadPage(index)
        else Toast.makeText(this, "This is the current page", Toast.LENGTH_SHORT).show()
    }

    private fun loadPage(newIndex: Int) {
        pageIndex = newIndex
        audioResultsMap = emptyMap()
        cachedBoundingBoxes = emptyList()
        pageImage.setImageDrawable(null)
        playButtonsMap.clear()
        boundingBoxViewsMap.clear()
        fetchPage()
    }

    private fun fetchAllThumbnails() {
        // ViewModel에 썸네일 요청
        for (i in 0 until totalPages) {
            viewModel.fetchThumbnail(sessionId, i)
        }

        // Flow 수집 → RecyclerView 갱신
        lifecycleScope.launch {
            viewModel.thumbnailList.collectLatest { list ->
                val sortedList = list.sortedBy { it.pageIndex }
                thumbnailAdapter.submitList(sortedList)
            }
        }
    }

    private fun navigateToFinish() {
        startActivity(Intent(this, FinishActivity::class.java).putExtra("session_id", sessionId))
        finish()
    }

    private fun navigateToCamera() {
        val intent = Intent(this, CameraSessionActivity::class.java).apply {
            putExtra("session_id", sessionId)
            putExtra("page_index", totalPages)  // 새 페이지 인덱스
        }
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        mediaPlayer?.release()
        mediaPlayer = null
        playButtonsMap.clear()
        boundingBoxViewsMap.clear()
    }
}
