package com.example.storybridge_android

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Base64
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

    // 🔹 순차 재생용
    private var audioQueue: List<String> = emptyList()
    private var currentAudioIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_reading)
        initViews()
        initUiState()
        initListeners()

        sessionId = intent.getStringExtra("session_id") ?: ""
        pageIndex = intent.getIntExtra("page_index", 0)
        fetchPageData()
    }

    private fun initViews() {
        mainLayout = findViewById(R.id.main)
        topUi = findViewById(R.id.topUi)
        bottomUi = findViewById(R.id.bottomUi)
        overlay = findViewById(R.id.sideOverlay)
        dimBackground = findViewById(R.id.dimBackground)
        findViewById<ImageButton>(R.id.playButton).isEnabled = false
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
        val playButton = findViewById<ImageButton>(R.id.playButton)

        mainLayout.setOnClickListener { toggleUI() }
        startButton.setOnClickListener { navigateToCamera() }
        menuButton.setOnClickListener { toggleOverlay(true) }
        closeButton.setOnClickListener { toggleOverlay(false) }
        dimBackground.setOnClickListener { toggleOverlay(false) }
        finishButton.setOnClickListener { navigateToFinish() }
        // playButton 클릭 리스너는 updateAudioSequential에서 설정
    }

    private fun navigateToFinish() {
        val intent = Intent(this, FinishActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToCamera() {
        val intent = Intent(this, CameraSessionActivity::class.java)
        intent.putExtra("session_id", sessionId)
        startActivity(intent)
        finish()
    }

    private fun navigateToReading(pageIndex: Int? = null) {
        val intent = Intent(this, ReadingActivity::class.java)
        pageIndex?.let { intent.putExtra("page_index", it) }
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
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    data class BoundingBox(
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int,
        val text: String
    )

    private fun displayBB(bboxes: List<BoundingBox>) {
        val pageImage = findViewById<ImageView>(R.id.pageImage)
        if (pageImage.drawable == null) return

        for (i in mainLayout.childCount - 1 downTo 0) {
            val child = mainLayout.getChildAt(i)
            if (child.tag == "bbox") mainLayout.removeViewAt(i)
        }

        val imageMatrix = pageImage.imageMatrix
        for (box in bboxes) {
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
                textSize = 14f
                gravity = Gravity.CENTER
                tag = "bbox"
            }

            val params = ConstraintLayout.LayoutParams(rect.width().toInt(), rect.height().toInt())
            params.startToStart = pageImage.id
            params.topToTop = pageImage.id
            boxView.layoutParams = params
            boxView.translationX = rect.left
            boxView.translationY = rect.top
            mainLayout.addView(boxView)
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
    // 🔹 오디오 순차 재생
    // -------------------

    private fun updateAudioSequential(audioBase64List: List<String>) {
        if (audioBase64List.isEmpty()) {
            disablePlayButton()
            return
        }

        audioQueue = audioBase64List
        currentAudioIndex = 0

        val playButton = findViewById<ImageButton>(R.id.playButton)
        playButton.isEnabled = true
        playButton.setOnClickListener {
            playNextAudio()
        }
    }

    private fun playNextAudio() {
        if (currentAudioIndex >= audioQueue.size) {
            currentAudioIndex = 0
            return
        }

        val base64Audio = audioQueue[currentAudioIndex]
        val audioFile = File(cacheDir, "temp_audio_$currentAudioIndex.mp3")
        try {
            val audioBytes = Base64.decode(base64Audio, Base64.DEFAULT)
            FileOutputStream(audioFile).use { it.write(audioBytes) }

            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(audioFile.absolutePath)
                setOnPreparedListener { start() }
                setOnCompletionListener {
                    currentAudioIndex++
                    playNextAudio()
                }
                prepareAsync()
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun disablePlayButton() {
        val playButton = findViewById<ImageButton>(R.id.playButton)
        playButton.isEnabled = false
        playButton.setOnClickListener(null)
    }

    // -------------------
    // 🔹 서버 통신
    // -------------------

    private fun fetchPageData() {
        fetchImage()
    }

    private fun fetchImage() {
        pageApi.getImage(sessionId, pageIndex).enqueue(object : Callback<GetImageResponse> {
            override fun onResponse(call: Call<GetImageResponse>, response: Response<GetImageResponse>) {
                if (response.isSuccessful) {
                    displayPage(response.body()?.image_base64)
                    fetchOcrResults()
                    fetchTtsResults()
                }
            }
            override fun onFailure(call: Call<GetImageResponse>, t: Throwable) {
                t.printStackTrace()
            }
        })
    }

    private fun fetchOcrResults() {
        pageApi.getOcrResults(sessionId, pageIndex).enqueue(object : Callback<GetOcrTranslationResponse> {
            override fun onResponse(call: Call<GetOcrTranslationResponse>, response: Response<GetOcrTranslationResponse>) {
                if (response.isSuccessful) {
                    val ocrList = response.body()?.ocr_results
                    val boxes = ocrList?.mapNotNull {
                        it.bbox?.let { box ->
                            BoundingBox(box.x, box.y, box.width, box.height, it.translation_txt)
                        }
                    } ?: emptyList()
                    if (boxes.isNotEmpty()) {
                        findViewById<ImageView>(R.id.pageImage).post {
                            displayBB(boxes)
                        }
                    }
                }
            }
            override fun onFailure(call: Call<GetOcrTranslationResponse>, t: Throwable) {}
        })
    }

    private fun fetchTtsResults() {
        pageApi.getTtsResults(sessionId, pageIndex).enqueue(object : Callback<GetTtsResponse> {
            override fun onResponse(call: Call<GetTtsResponse>, response: Response<GetTtsResponse>) {
                if (response.isSuccessful) {
                    val audioList = response.body()?.audio_results
                    if (!audioList.isNullOrEmpty()) {
                        val fullAudioList = audioList.flatMap { it.audio_base64_list }
                        if (fullAudioList.isNotEmpty()) {
                            updateAudioSequential(fullAudioList)
                        } else {
                            disablePlayButton()
                        }
                    } else {
                        disablePlayButton()
                    }
                } else {
                    disablePlayButton()
                }
            }
            override fun onFailure(call: Call<GetTtsResponse>, t: Throwable) {
                disablePlayButton()
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
    }
}
