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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.storybridge_android.network.GetImageResponse
import com.example.storybridge_android.network.GetOcrTranslationResponse
import com.example.storybridge_android.network.GetTtsResponse
import com.example.storybridge_android.network.RetrofitClient
import java.io.File
import java.io.FileOutputStream
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

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
        playButton.setOnClickListener { playAudio() }
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

    private fun updateAudio(base64Audio: String?) {
        val playButton = findViewById<ImageButton>(R.id.playButton)
        if (base64Audio.isNullOrEmpty()) {
            playButton.isEnabled = false
            return
        }
        val audioFile = File(cacheDir, "temp_audio.mp3")
        try {
            val audioBytes = Base64.decode(base64Audio, Base64.DEFAULT)
            FileOutputStream(audioFile).use { it.write(audioBytes) }
            playButton.isEnabled = true
        } catch (e: Exception) {
            e.printStackTrace()
            playButton.isEnabled = false
        }
    }

    private fun playAudio() {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer()
            val audioFile = File(cacheDir, "temp_audio.mp3")
            if (!audioFile.exists()) return

            mediaPlayer?.setDataSource(audioFile.absolutePath)
            mediaPlayer?.setOnPreparedListener { it.start() }
            mediaPlayer?.setOnCompletionListener { it.release() }
            mediaPlayer?.prepareAsync()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun fetchPageData() {
        fetchImage()
        fetchOcrResults()
        fetchTtsResults()
    }

    private fun fetchImage() {
        pageApi.getImage(sessionId, pageIndex).enqueue(object : Callback<GetImageResponse> {
            override fun onResponse(call: Call<GetImageResponse>, response: Response<GetImageResponse>) {
                if (response.isSuccessful) {
                    val imageBase64 = response.body()?.image_base64
                    displayPage(imageBase64)
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
                    val ocrList = response.body()?.ocr_results ?: return
                    val boxes = ocrList.mapNotNull {
                        it.bbox?.let { box ->
                            BoundingBox(box.x, box.y, box.width, box.height, it.translation_txt)
                        }
                    }
                    if (boxes.isNotEmpty()) {
                        findViewById<ImageView>(R.id.pageImage).post {
                            displayBB(boxes)
                        }
                    }
                }
            }

            override fun onFailure(call: Call<GetOcrTranslationResponse>, t: Throwable) {
                t.printStackTrace()
            }
        })
    }

    private fun fetchTtsResults() {
        pageApi.getTtsResults(sessionId, pageIndex).enqueue(object : Callback<GetTtsResponse> {
            override fun onResponse(call: Call<GetTtsResponse>, response: Response<GetTtsResponse>) {
                if (response.isSuccessful) {
                    val audioList = response.body()?.audio_results
                    if (!audioList.isNullOrEmpty()) {
                        val mergedAudio = audioList.first().audio_base64
                        updateAudio(mergedAudio)
                    }
                }
            }
            override fun onFailure(call: Call<GetTtsResponse>, t: Throwable) {
                t.printStackTrace()
            }
        })
    }
}