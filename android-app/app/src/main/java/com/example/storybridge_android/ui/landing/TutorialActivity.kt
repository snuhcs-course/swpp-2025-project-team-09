package com.example.storybridge_android.ui.landing

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.storybridge_android.R
import com.example.storybridge_android.ui.common.BaseActivity
import com.example.storybridge_android.ui.main.MainActivity
import com.example.storybridge_android.ui.setting.AppSettings

class TutorialActivity : BaseActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var indicatorLayout: LinearLayout
    private lateinit var nextButton: Button
    private lateinit var startButton: Button

    private lateinit var exitPanel: View
    private lateinit var exitConfirmBtn: Button
    private lateinit var exitCancelBtn: Button

    private val tutorialList: List<TutorialPage> by lazy { createTutorialData() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupSystemUI()
        setContentView(R.layout.activity_tutorial)
        initViews()
        setupWindowInsets()
        setupViewPager()
        setupButtons()
        setupExitLogic()
    }

    private fun setupSystemUI() {
        enableEdgeToEdge()
        val selectedLang = intent.getStringExtra("lang") ?: AppSettings.getLanguage(this)
        AppSettings.setLanguage(this, selectedLang)
    }

    private fun setupWindowInsets() {
        val contentLayout = findViewById<View>(R.id.contentLayout)
        ViewCompat.setOnApplyWindowInsetsListener(contentLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun initViews() {
        viewPager = findViewById(R.id.viewPager)
        indicatorLayout = findViewById(R.id.indicatorLayout)
        nextButton = findViewById(R.id.nextButton)
        startButton = findViewById(R.id.startButton)

        exitPanel = findViewById(R.id.exitPanelInclude)
        exitConfirmBtn = findViewById(R.id.exitConfirmBtn)
        exitCancelBtn = findViewById(R.id.exitCancelBtn)
    }

    private fun createTutorialData(): List<TutorialPage> {
        return listOf(
            TutorialPage(R.drawable.tutorial_1, getString(R.string.tutorial_1)),
            TutorialPage(R.drawable.tutorial_2, getString(R.string.tutorial_2)),
            TutorialPage(R.drawable.tutorial_3, getString(R.string.tutorial_3)),
            TutorialPage(R.drawable.tutorial_4, getString(R.string.tutorial_4)),
            TutorialPage(R.drawable.tutorial_5, getString(R.string.tutorial_5)),
            TutorialPage(R.drawable.tutorial_6, getString(R.string.tutorial_6)),
            TutorialPage(R.drawable.tutorial_7, getString(R.string.tutorial_7)),
            TutorialPage(R.drawable.tutorial_8, getString(R.string.tutorial_8)),
            TutorialPage(R.drawable.tutorial_9, getString(R.string.tutorial_9)),
            TutorialPage(R.drawable.tutorial_10, getString(R.string.tutorial_10))
        )
    }

    private fun setupViewPager() {
        val adapter = TutorialPagerAdapter(tutorialList)
        viewPager.adapter = adapter

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateUiForPosition(position)
            }
        })

        updateUiForPosition(0)
    }

    private fun updateUiForPosition(position: Int) {
        updateIndicators(position)
        updateButtonVisibility(position)
    }

    private fun updateIndicators(position: Int) {
        for (i in 0 until indicatorLayout.childCount) {
            val dot = indicatorLayout.getChildAt(i)
            val drawableRes = if (i == position) R.drawable.circle_dark else R.drawable.circle_gray
            dot.setBackgroundResource(drawableRes)
        }
    }

    private fun updateButtonVisibility(position: Int) {
        val isLastPage = position == tutorialList.size - 1
        nextButton.visibility = if (isLastPage) View.GONE else View.VISIBLE
        startButton.visibility = if (isLastPage) View.VISIBLE else View.GONE
    }

    private fun setupButtons() {
        nextButton.setOnClickListener {
            val currentItem = viewPager.currentItem
            if (currentItem < tutorialList.size - 1) {
                viewPager.currentItem = currentItem + 1
            }
        }

        startButton.setOnClickListener {
            navigateToMain()
        }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun setupExitLogic() {
        exitConfirmBtn.setOnClickListener { finish() }
        exitCancelBtn.setOnClickListener { exitPanel.visibility = View.GONE }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                exitPanel.visibility = View.VISIBLE
            }
        })
    }
}

data class TutorialPage(
    val imageRes: Int,
    val description: String
)