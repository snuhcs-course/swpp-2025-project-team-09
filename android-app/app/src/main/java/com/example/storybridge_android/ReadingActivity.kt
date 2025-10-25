package com.example.storybridge_android

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.storybridge_android.network.GetImageResponse
import com.example.storybridge_android.network.GetOcrTranslationResponse
import com.example.storybridge_android.network.GetTtsResponse
import com.example.storybridge_android.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream

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

    companion object {
        private const val TAG = "ReadingActivity"
    }

    // 🔹 각 bounding box별 오디오 데이터 저장
    private var audioResultsMap: Map<Int, List<String>> = emptyMap()
    private var currentPlayingIndex: Int = -1
    private var currentAudioIndex: Int = 0

    // 🔹 play button 참조 저장 (색상 변경용)
    private val playButtonsMap: MutableMap<Int, ImageButton> = mutableMapOf()

    // 🔹 OCR 결과 저장 (TTS 데이터 로드 후 버튼 추가를 위해)
    private var cachedBoundingBoxes: List<BoundingBox> = emptyList()

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

    // 기존 global play button은 숨김 처리
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

    private fun navigateToFinish() {
        val intent = Intent(this, FinishActivity::class.java)
        intent.putExtra("session_id", sessionId)
        startActivity(intent)
        finish()
    }

    private fun navigateToCamera() {
        val intent = Intent(this, CameraSessionActivity::class.java)
        intent.putExtra("session_id", sessionId)
        intent.putExtra("page_index", pageIndex + 1) // Next page
        startActivity(intent)
        finish()
    }

    private fun displayPage(base64Image: String?) {
        val pageImage = findViewById<ImageView>(R.id.pageImage)
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

        val pageImage = findViewById<ImageView>(R.id.pageImage)
        if (pageImage.drawable == null) {
            Log.e(TAG, "✗ Page image not loaded yet")
            return
        }

        // 기존 bounding box 및 play button 제거
        for (i in mainLayout.childCount - 1 downTo 0) {
            val child = mainLayout.getChildAt(i)
            if (child.tag == "bbox" || child.tag == "play_button") {
                mainLayout.removeViewAt(i)
            }
        }

        // playButtonsMap 초기화
        playButtonsMap.clear()

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

            // 🔹 텍스트 박스 생성 - WRAP_CONTENT로 변경하여 텍스트 크기에 맞춤
            val boxView = TextView(this).apply {
                text = box.text
                setBackgroundColor(getColor(R.color.black_50))
                setTextColor(getColor(R.color.white))
                textSize = 14f
                gravity = Gravity.START or Gravity.TOP
                setPadding(8, 8, 8, 8)
                tag = "bbox"
            }

            // 🔹 텍스트 크기를 측정하기 위해 먼저 measure 호출
            boxView.measure(
                View.MeasureSpec.makeMeasureSpec(rect.width().toInt(), View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )

            // 🔹 측정된 크기로 레이아웃 파라미터 설정
            val textWidth = boxView.measuredWidth
            val textHeight = boxView.measuredHeight

            Log.d(TAG, "Box ${box.index} - Original: ${rect.width().toInt()}x${rect.height().toInt()}, Text needs: ${textWidth}x${textHeight}")

            val params = ConstraintLayout.LayoutParams(textWidth, textHeight)
            params.startToStart = pageImage.id
            params.topToTop = pageImage.id
            boxView.layoutParams = params
            boxView.translationX = rect.left
            boxView.translationY = rect.top
            mainLayout.addView(boxView)

            // 🔹 play button 생성 (이 box에 오디오가 있는 경우에만)
            if (audioResultsMap.containsKey(box.index)) {
                Log.d(TAG, "Box ${box.index} has audio, creating play button")
            // 텍스트 박스의 실제 크기를 사용
                val textRect = RectF(rect.left, rect.top, rect.left + textWidth, rect.top + textHeight)
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
            alpha = 1.0f
            tag = "play_button"
            contentDescription = "Play audio for box $bboxIndex"
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setPadding(8, 8, 8, 8)
        }

        // 버튼 크기 설정 (예: 54dp x 54dp)
        val buttonSize = (54 * resources.displayMetrics.density).toInt()
        val params = ConstraintLayout.LayoutParams(buttonSize, buttonSize)
        params.startToStart = pageImageId
        params.topToTop = pageImageId

        playButton.layoutParams = params

        // 🔹 bottom-right 위치 계산
        playButton.translationX = rect.right - buttonSize/2 + 4
        playButton.translationY = rect.bottom - buttonSize/2 + 4

        // 🔹 Z-order를 높여서 다른 요소 위에 표시
        playButton.elevation = 8f

        // 클릭 리스너 설정
        playButton.setOnClickListener {
            Log.d(TAG, "Play button clicked for box $bboxIndex")
            playAudioForBox(bboxIndex)
        }

        mainLayout.addView(playButton)

        // 🔹 버튼 참조 저장
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
                updateButtonToPaused(bboxIndex) // Change to play icon but keep dark background
                Log.d(TAG, "Audio paused for box $bboxIndex at ${mediaPlayer?.currentPosition}")
            } else if (mediaPlayer != null) {
                mediaPlayer?.start()
                updateButtonToPlaying(bboxIndex) // Change to pause icon
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

        // 🔹 이전에 재생 중이던 버튼 상태 복원
        if (currentPlayingIndex != -1 && currentPlayingIndex != bboxIndex) {
            resetButtonState(currentPlayingIndex)
        }

        // 현재 재생 중인 것 중지
        mediaPlayer?.release()
        mediaPlayer = null

        currentPlayingIndex = bboxIndex
        currentAudioIndex = 0

        // 🔹 버튼 상태를 재생 중으로 변경
        updateButtonToPlaying(bboxIndex)

        playNextAudioInBox(audioList)
    }

    private fun playNextAudioInBox(audioList: List<String>) {
        if (currentAudioIndex >= audioList.size) {
            Log.d(TAG, "✓ All audio clips played for box $currentPlayingIndex")
            // 🔹 모든 오디오 재생 완료 - 버튼 상태 복원
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
                    // 🔹 에러 발생 시에도 버튼 상태 복원
                    resetButtonState(currentPlayingIndex)
                    currentPlayingIndex = -1
                    true
                }
                prepareAsync()
            }

        } catch (e: Exception) {
            Log.e(TAG, "✗ Error playing audio", e)
            e.printStackTrace()
            // 🔹 예외 발생 시 버튼 상태 복원
            resetButtonState(currentPlayingIndex)
            currentPlayingIndex = -1
        }
    }

    // 🎵 버튼을 재생 중 상태로 업데이트
    private fun updateButtonToPlaying(bboxIndex: Int) {
        playButtonsMap[bboxIndex]?.apply {
            setImageResource(android.R.drawable.ic_media_pause)
            setBackgroundResource(R.drawable.circle_dark)

            alpha = 1.0f
            Log.d(TAG, "✓ Button $bboxIndex set to playing state")
        }
    }
    // 🔹 버튼을 기본 상태로 복원
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

// -------------------
// 🔹 서버 통신
// -------------------

    private fun fetchPageData() {
        Log.d(TAG, "=== Fetching Page Data ===")
        fetchImage()
    }

    private fun fetchImage() {
        Log.d(TAG, "Fetching image...")
        pageApi.getImage(sessionId, pageIndex).enqueue(object : Callback<GetImageResponse> {
            override fun onResponse(call: Call<GetImageResponse>, response: Response<GetImageResponse>) {
                if (response.isSuccessful) {
                    Log.d(TAG, "✓ Image fetched successfully")
                    displayPage(response.body()?.image_base64)
                    fetchOcrResults()
                    fetchTtsResults()
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
        pageApi.getOcrResults(sessionId, pageIndex).enqueue(object : Callback<GetOcrTranslationResponse> {
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
                        findViewById<ImageView>(R.id.pageImage).post {
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
// 🔹 bbox_index를 키로 하는 맵 생성
                        audioResultsMap = audioList.associate { audioResult ->
                            Log.d(TAG, "Audio for bbox ${audioResult.bbox_index}: ${audioResult.audio_base64_list.size} clips")
                            audioResult.bbox_index to audioResult.audio_base64_list
                        }

// 🔹 OCR 결과가 이미 표시되었다면 play button 추가
                        if (cachedBoundingBoxes.isNotEmpty()) {
                            findViewById<ImageView>(R.id.pageImage).post {
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

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        playButtonsMap.clear()
        Log.d(TAG, "=== Activity destroyed ===")
    }
}