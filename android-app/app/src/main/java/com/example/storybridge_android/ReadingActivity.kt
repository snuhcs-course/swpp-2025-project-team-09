package com.example.storybridge_android

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ImageButton

class ReadingActivity : AppCompatActivity() {

    private lateinit var mainLayout: ConstraintLayout
    private lateinit var topUi: View
    private lateinit var bottomUi: View
    private var uiVisible = false
    private lateinit var overlay: ConstraintLayout
    private lateinit var dimBackground: View
    private var isOverlayVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_reading)

        mainLayout = findViewById(R.id.main)
        topUi = findViewById(R.id.topUi)
        bottomUi = findViewById(R.id.bottomUi)
        overlay = findViewById(R.id.sideOverlay)
        dimBackground = findViewById(R.id.dimBackground)
        val closeBtn: Button = findViewById(R.id.closeOverlayButton)
        val startButton = findViewById<Button>(R.id.startButton)
        val menuButton = findViewById<ImageButton>(R.id.menuButton) // 메뉴 버튼 추가!

        // 처음에는 UI 숨기기 (post 블록으로 height 보장)
        topUi.post { topUi.translationY = -topUi.height.toFloat() }
        bottomUi.post { bottomUi.translationY = bottomUi.height.toFloat() }

        ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 화면 터치 시 UI 토글
        mainLayout.setOnClickListener { toggleUi() }

        // Start 버튼 클릭 시 CameraActivity로 이동
        startButton.setOnClickListener {
            val intent = Intent(this, CameraActivity::class.java)
            startActivity(intent)
        }

        // 메뉴 버튼 클릭 시 오버레이 열기
        menuButton.setOnClickListener {
            toggleOverlay(true)
        }

        // 닫기 버튼 클릭 시
        closeBtn.setOnClickListener {
            toggleOverlay(false)
        }

        // 검은 배경 클릭 시 닫기
        dimBackground.setOnClickListener {
            toggleOverlay(false)
        }

        val finishButton = findViewById<Button>(R.id.finishButton)
        finishButton.setOnClickListener {
            val intent = Intent(this, FinishActivity::class.java)
            startActivity(intent)
            finish() // 필요하면 현재 액티비티 종료
        }
    }


    private fun navigateToFinish() {
        // TODO: FinishActivity로 이동하는 로직 구현
        // TODO: 상단 네비게이션에 버튼 존재
    }

    private fun navigateToCamera() {
        // TODO: CameraActivity로 이동하는 로직 구현
        // TODO: 하단 네비게이션에 버튼 존재
    }

    private fun navigateToReading() {
        // TODO: ReadingActivity로 이동하는 로직 구현
        // TODO: 왼쪽에서 나오는 오버레이에 페이지를 누르면 해당 페이지로 이동
    }

    private fun displayPage() {
        // TODO: 페이지 이미지 보여주기
    }

    private fun displayBB() {
        // TODO: 번역된 텍스트가 있는 bounding box, 오디오 버튼 보여주기
    }

    private fun toggleUI() {
        // TODO: 터치 여부에 따라서 상단, 하단 바 숨기거나 보여줌
    }

    private fun toggleOverlay() {
        // TODO: 좌측 오버레이 숨기거나 보여줌
    }

    private fun updateAudio() {
        // TODO: 서버로부터 TTS 파일을 받아서 오디오 버튼에 바인딩
    }

    private fun playAudio() {
        // TODO: 오디오 버튼을 누르면 재생
    }


    private fun toggleOverlay(show: Boolean) {
        if (show && !isOverlayVisible) {
            overlay.visibility = View.VISIBLE
            dimBackground.visibility = View.VISIBLE

            overlay.animate()
                .translationX(0f)
                .setDuration(300)
                .start()

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

    private fun addOverlayBox(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        text: String
    ) {
        // 텍스트 뷰 생성
        val boxView = TextView(this).apply {
            this.text = text
            setBackgroundColor(getColor(R.color.black_50)) // 반투명 검은색
            setTextColor(getColor(R.color.white))
            textSize = 16f
            gravity = Gravity.CENTER
            id = View.generateViewId()
        }

        // ConstraintLayout.LayoutParams 생성
        val params = ConstraintLayout.LayoutParams(width, height)
        // 부모 ConstraintLayout의 좌상단에 맞춤
        params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
        params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
        boxView.layoutParams = params

        // 좌표 이동
        boxView.translationX = x.toFloat()
        boxView.translationY = y.toFloat()

        // ConstraintLayout에 추가
        mainLayout.addView(boxView)
    }


    private fun toggleUi() {
        if (uiVisible) {
            // UI 숨기기
            topUi.animate().translationY(-topUi.height.toFloat()).setDuration(300).start()
            bottomUi.animate().translationY(bottomUi.height.toFloat()).setDuration(300).start()
        } else {
            // UI 등장
            topUi.animate().translationY(0f).setDuration(300).start()
            bottomUi.animate().translationY(0f).setDuration(300).start()
        }
        uiVisible = !uiVisible
    }


}