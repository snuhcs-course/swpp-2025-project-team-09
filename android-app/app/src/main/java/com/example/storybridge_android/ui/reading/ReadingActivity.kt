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
import androidx.activity.result.contract.ActivityResultContracts
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
import com.example.storybridge_android.ui.common.TopNav
import com.example.storybridge_android.ui.common.BottomNav
import com.example.storybridge_android.ui.common.LeftOverlay
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import kotlin.math.max


class ReadingActivity : AppCompatActivity() {

    private lateinit var sessionId: String
    private var pageIndex = 0
    private var totalPages = 0
    private var isNewSession = true
    private lateinit var mainLayout: ConstraintLayout
    private lateinit var pageImage: ImageView
    private lateinit var topUi: TopNav
    private lateinit var bottomUi: BottomNav
    private lateinit var leftPanel: LeftOverlay
    private lateinit var dimBackground: View
    private lateinit var thumbnailRecyclerView: RecyclerView

    private var uiVisible = false
    private var isOverlayVisible = false
    private val handler = Handler(Looper.getMainLooper())
    private var mediaPlayer: MediaPlayer? = null
    private var pageBitmap: Bitmap? = null

    private val viewModel: ReadingViewModel by viewModels {
        ReadingViewModelFactory(PageRepositoryImpl())
    }

    companion object {
        private const val TAG = "ReadingActivity"
        private const val TOUCH_SLOP = 10f
    }

    private var audioResultsMap: Map<Int, List<String>> = emptyMap()
    private var currentPlayingIndex: Int = -1
    private var currentAudioIndex: Int = 0
    private val playButtonsMap: MutableMap<Int, ImageButton> = mutableMapOf()
    private val boundingBoxViewsMap: MutableMap<Int, TextView> = mutableMapOf()
    private var cachedBoundingBoxes: List<BoundingBox> = emptyList()
    private val savedBoxTranslations: MutableMap<Int, Pair<Float, Float>> = mutableMapOf()
    private val MIN_WIDTH = 500

    private lateinit var thumbnailAdapter: ThumbnailAdapter

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val newPageAdded = result.data?.getBooleanExtra("page_added", false) ?: false
            if (newPageAdded) {
                totalPages++
                fetchAllThumbnails()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_reading)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                AlertDialog.Builder(this@ReadingActivity)
                    .setTitle(getString(R.string.exit_dialog_title))
                    .setMessage(getString(R.string.exit_dialog_message))
                    .setPositiveButton(getString(R.string.exit_dialog_confirm)) { _, _ ->
                        navigateToFinish()
                    }
                    .setNegativeButton(getString(R.string.exit_dialog_cancel), null)
                    .show()
            }
        })

        initViews()
        initUiState()
        initListeners()

        sessionId = intent.getStringExtra("session_id") ?: ""
        pageIndex = intent.getIntExtra("page_index", 0)
        totalPages = intent.getIntExtra("total_pages", pageIndex + 1)
        isNewSession = intent.getBooleanExtra("is_new_session", true)

        // Update page status on BottomNav
        updateBottomNavStatus()

        observeViewModel()
        fetchPage()
        fetchAllThumbnails()
    }

    private fun initViews() {
        mainLayout = findViewById(R.id.main)
        pageImage = findViewById(R.id.pageImage)
        topUi = findViewById(R.id.topUi)
        bottomUi = findViewById(R.id.bottomUi)
        leftPanel = findViewById(R.id.leftPanel)
        dimBackground = findViewById(R.id.dimBackground)

        // Get RecyclerView from LeftOverlay component
        thumbnailRecyclerView = leftPanel.thumbnailRecyclerView

        thumbnailAdapter = ThumbnailAdapter { onThumbnailClick(it) }
        thumbnailRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ReadingActivity)
            adapter = thumbnailAdapter
        }
    }

    private fun initUiState() {
        topUi.post { topUi.translationY = -topUi.height.toFloat() }
        bottomUi.post { bottomUi.translationY = bottomUi.height.toFloat() }

        // Hide left panel initially
        leftPanel.visibility = View.GONE
        leftPanel.post { leftPanel.translationX = -leftPanel.width.toFloat() }
    }

    private fun initListeners() {
        // TopNav listeners
        topUi.setOnMenuButtonClickListener { toggleOverlay(true) }
        topUi.setOnFinishButtonClickListener { navigateToFinish() }

        // BottomNav listeners
        bottomUi.setOnPrevButtonClickListener {
            loadPage(pageIndex - 1)
        }
        bottomUi.setOnCaptureButtonClickListener { navigateToCamera() }

        bottomUi.setOnNextButtonClickListener {
            loadPage(pageIndex + 1)
        }

        // Dim background listener - closes overlay when clicking outside
        dimBackground.setOnClickListener { toggleOverlay(false) }

        // Main layout listener for toggling UI
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

        // 썸네일 리스트 Flow 수집 (한 번만 실행)
        lifecycleScope.launch {
            viewModel.thumbnailList.collectLatest { list ->
                val sortedList = list.sortedBy { it.pageIndex }
                thumbnailAdapter.submitList(sortedList)
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

    data class BoundingBox(
        val x: Int, val y: Int, val width: Int, val height: Int,
        val text: String, val index: Int
    )

    private fun handleOcr(data: GetOcrTranslationResponse) {
        val boxes = data.ocr_results.mapIndexed { i, ocrBox ->
            val box = ocrBox.bbox
            val adjustedWidth = max(box.width, MIN_WIDTH)
            BoundingBox(box.x, box.y, adjustedWidth, box.height, ocrBox.translation_txt, i)
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
            leftPanel.visibility = View.VISIBLE
            dimBackground.visibility = View.VISIBLE
            leftPanel.animate().translationX(0f).setDuration(300).start()
            isOverlayVisible = true
        } else if (!show && isOverlayVisible) {
            leftPanel.animate()
                .translationX(-leftPanel.width.toFloat())
                .setDuration(300)
                .withEndAction {
                    leftPanel.visibility = View.GONE
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
                    // Save the final position when user releases
                    if (dragging) {
                        savedBoxTranslations[boxIndex] = Pair(boxView.translationX, boxView.translationY)
                    }
                    dragging = false; true
                }
                else -> false
            }
        }
    }

    private fun displayBB(bboxes: List<BoundingBox>) {
        for (i in mainLayout.childCount - 1 downTo 0) {
            val child = mainLayout.getChildAt(i)
            if (child.tag == "bbox" || child.tag == "play_button") mainLayout.removeViewAt(i)
        }

        playButtonsMap.clear()
        boundingBoxViewsMap.clear()

        for (box in bboxes) {
            val rect = RectF(
                box.x.toFloat(),
                box.y.toFloat(),
                (box.x + box.width).toFloat(),
                (box.y + box.height).toFloat()
            )

            val boxView = TextView(this).apply {
                text = box.text
                setBackgroundResource(R.drawable.bbox_background)
                setTextAppearance(R.style.BBoxText)
                setLineSpacing(0f, 0.8f)  // 줄 간격 설정 (multiplier = 0.8)
                gravity = Gravity.START or Gravity.TOP
                setPadding(24, 16, 24, 16)
                tag = "bbox"
            }

            val params = ConstraintLayout.LayoutParams(
                rect.width().toInt(),  // 너비는 서버 데이터 고정
                ConstraintLayout.LayoutParams.WRAP_CONTENT  // 높이는 텍스트에 맞춤
            )
            params.startToStart = pageImage.id
            params.topToTop = pageImage.id
            boxView.layoutParams = params

            boxView.translationX = rect.left
            boxView.translationY = rect.top

            mainLayout.addView(boxView)
            boundingBoxViewsMap[box.index] = boxView

            setupBoundingBoxTouchListener(boxView, box.index)

            // Restore saved position if it exists
            savedBoxTranslations[box.index]?.let { (savedX, savedY) ->
                boxView.translationX = savedX
                boxView.translationY = savedY
            }

            if (audioResultsMap.containsKey(box.index)) {
                createPlayButton(box.index, rect)
            }
        }
    }

    private fun createPlayButton(bboxIndex: Int, rect: RectF) {
        val playButton = ImageButton(this, null, android.R.attr.borderlessButtonStyle).apply {
            setImageResource(R.drawable.ic_headphone)
            background = null
            scaleType = ImageView.ScaleType.FIT_CENTER
            tag = "play_button"
            setPadding(0, 0, 0, 0)
        }
        val size = (40 * resources.displayMetrics.density).toInt()
        val params = ConstraintLayout.LayoutParams(size, size)
        params.startToStart = pageImage.id
        params.topToTop = pageImage.id
        playButton.layoutParams = params
        val boxView = boundingBoxViewsMap[bboxIndex]
        val boxTranslationX = boxView?.translationX ?: rect.left
        val boxTranslationY = boxView?.translationY ?: rect.top
        playButton.translationX = boxTranslationX + rect.width() - size / 2
        playButton.translationY = boxTranslationY + rect.height() - size / 2
        playButton.elevation = 8f
        playButton.setOnClickListener { playAudioForBox(bboxIndex) }
        mainLayout.addView(playButton)
        playButtonsMap[bboxIndex] = playButton
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
            setImageResource(R.drawable.ic_pause)
        }
    }

    private fun updateButtonToPaused(bboxIndex: Int) {
        playButtonsMap[bboxIndex]?.apply {
            setImageResource(R.drawable.ic_headphone)
        }
    }

    private fun resetButtonState(bboxIndex: Int) {
        playButtonsMap[bboxIndex]?.apply {
            setImageResource(R.drawable.ic_headphone)
        }
    }

    private fun onThumbnailClick(index: Int) {
        if (index != pageIndex) loadPage(index)
        else Toast.makeText(this, "This is the current page", Toast.LENGTH_SHORT).show()
    }

    private fun loadPage(newIndex: Int) {
        // Stop any playing audio and reset state
        mediaPlayer?.release()
        mediaPlayer = null
        currentPlayingIndex = -1
        currentAudioIndex = 0
        // Clear page data
        pageIndex = newIndex
        audioResultsMap = emptyMap()
        cachedBoundingBoxes = emptyList()
        pageImage.setImageDrawable(null)
        playButtonsMap.clear()
        boundingBoxViewsMap.clear()
        savedBoxTranslations.clear()
        updateBottomNavStatus()
        // Fetch new page
        fetchPage()
    }

    private fun updateBottomNavStatus() {
        bottomUi.updatePageStatus(pageIndex, totalPages)
    }

    private fun fetchAllThumbnails() {
        // ViewModel에 썸네일 요청 (커버 페이지 제외, 1부터 시작)
        for (i in 1 until totalPages) {
            viewModel.fetchThumbnail(sessionId, i)
        }
        // Flow 수집은 observeViewModel()에서 한 번만 실행됨
    }

    private fun navigateToFinish() {
        val intent = Intent(this, FinishActivity::class.java).apply {
            putExtra("session_id", sessionId)
            putExtra("is_new_session", isNewSession)
        }
        startActivity(intent)
        finish()
    }

    private fun navigateToCamera() {
        val intent = Intent(this, CameraSessionActivity::class.java).apply {
            putExtra("session_id", sessionId)
            putExtra("page_index", totalPages)  // 새 페이지 인덱스
        }
        cameraLauncher.launch(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        mediaPlayer?.release()
        mediaPlayer = null
        playButtonsMap.clear()
        boundingBoxViewsMap.clear()
        savedBoxTranslations.clear()
    }
}
