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
import com.example.storybridge_android.ui.BottomNav
import com.example.storybridge_android.ui.LeftOverlay
import com.example.storybridge_android.ui.TopNav

class ReadingActivity : AppCompatActivity() {

    private lateinit var mainLayout: ConstraintLayout
    private lateinit var topNav: TopNav
    private lateinit var bottomNav: BottomNav
    private var uiVisible = false
    private lateinit var leftOverlay: LeftOverlay
    private lateinit var dimBackground: View
    private var isOverlayVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_reading)

        mainLayout = findViewById(R.id.main)
        topNav = findViewById(R.id.topNav)
        bottomNav = findViewById(R.id.bottomNav)
        leftOverlay = findViewById(R.id.leftOverlay)
        dimBackground = findViewById(R.id.dimBackground)

        ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initializeNavPosision()
        mainLayout.setOnClickListener { toggleUi() }

        connectTopNav()
        connectBottomNav()
        connectLeftOverlay()

        // 검은 배경 클릭 시 닫기
        dimBackground.setOnClickListener {
            toggleOverlay(false)
        }
    }

    private fun connectTopNav() {
        topNav.setOnMenuButtonClickListener {
            // 메뉴 버튼이 클릭되었을 때의 동작
            toggleOverlay(true)
        }
        topNav.setOnFinishButtonClickListener {
            navigateToFinish()
        }
    }

    private fun connectBottomNav() {
        // TODO: 이전 intent로 정보 받아오기 필요
        val hasPrev = intent.getBooleanExtra("hasPrev", false)
        val hasNext = intent.getBooleanExtra("hasNext", false)

        // 받아온 정보로 BottomNav의 상태(버튼 가시성)를 설정합니다.
        bottomNav.configure(hasPrevious = hasPrev, hasNext = hasNext)

        // 이전 페이지 버튼 리스너 (버튼이 보일 때만 동작)
        bottomNav.setOnPrevButtonClickListener {
            // TODO: 이전 페이지로 이동하는 로직 구현 (예: 새로운 ReadingActivity 시작)
        }

        // 'Capture New Page' 버튼 리스너
        bottomNav.setOnCaptureButtonClickListener {
            navigateToCamera()
        }

        // 다음 페이지 버튼 리스너 (버튼이 보일 때만 동작)
        bottomNav.setOnNextButtonClickListener {
            // TODO: 다음 페이지로 이동하는 로직 구현 (예: 새로운 ReadingActivity 시작)
        }
    }

    private fun connectLeftOverlay() {
        leftOverlay.setOnCloseButtonClickListener {
            toggleOverlay(false)
        }

        // TODO: 실제 페이지 수와 데이터를 가져와서 설정해야 합니다.
    }


    private fun navigateToFinish() {
        val intent = Intent(this, FinishActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToCamera() {
        val intent = Intent(this, CameraActivity::class.java)
        startActivity(intent)
        finish()
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

    private fun updateAudio() {
        // TODO: 서버로부터 TTS 파일을 받아서 오디오 버튼에 바인딩
    }

    private fun playAudio() {
        // TODO: 오디오 버튼을 누르면 재생
    }


    private fun initializeNavPosision() {
        topNav.post { topNav.translationY = -topNav.height.toFloat() }
        bottomNav.post { bottomNav.translationY = bottomNav.height.toFloat() }

        leftOverlay.post {
            leftOverlay.translationX = -leftOverlay.width.toFloat()
            leftOverlay.visibility = View.GONE
        }
        dimBackground.visibility = View.GONE
    }

    private fun toggleOverlay(show: Boolean) {
        if (show && !isOverlayVisible) {
            leftOverlay.visibility = View.VISIBLE
            dimBackground.visibility = View.VISIBLE

            leftOverlay.animate()
                .translationX(0f)
                .setDuration(300)
                .start()

            isOverlayVisible = true
        } else if (!show && isOverlayVisible) {
            leftOverlay.animate()
                .translationX(-leftOverlay.width.toFloat())
                .setDuration(300)
                .withEndAction {
                    leftOverlay.visibility = View.GONE
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
            topNav.animate().translationY(-topNav.height.toFloat()).setDuration(300).start()
            bottomNav.animate().translationY(bottomNav.height.toFloat()).setDuration(300).start()
        } else {
            // UI 등장
            topNav.animate().translationY(0f).setDuration(300).start()
            bottomNav.animate().translationY(0f).setDuration(300).start()
        }
        uiVisible = !uiVisible
    }


}