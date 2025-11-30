package com.example.storybridge_android.ui.session

import com.example.storybridge_android.R
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.Observer
import com.example.storybridge_android.data.BalloonColor
import com.example.storybridge_android.data.BalloonData
import com.example.storybridge_android.databinding.ActivityFinishBinding
import com.example.storybridge_android.network.SessionStatsResponse
import com.example.storybridge_android.ui.common.BaseActivity
import com.example.storybridge_android.ui.main.MainActivity

class FinishActivity : BaseActivity() {

    private lateinit var binding: ActivityFinishBinding
    private val viewModel: FinishViewModel by viewModels {
        FinishViewModelFactory()
    }
    private var isNewSession = true
    private lateinit var sessionId: String

    // Store texts in order (regardless of which balloon is popped)
    private val orderedTexts = mutableListOf<String>()
    private var poppedCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityFinishBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeSessionData()
        setupObservers()
        setupClickListeners()

        viewModel.endSession(sessionId)
    }

    private fun initializeSessionData() {
        sessionId = intent.getStringExtra("session_id") ?: ""
        isNewSession = intent.getBooleanExtra("is_new_session", true)
    }

    private fun setupObservers() {
        observeSessionStats()
        setupBalloonCallback()
    }

    private fun observeSessionStats() {
        viewModel.sessionStats.observe(this, Observer { stats ->
            setupBalloons(stats)
        })
    }

    private fun setupBalloonCallback() {
        binding.balloonView.onBalloonPopped = { lineIndex, text ->
            // Add the next text in order (regardless of which balloon was popped)
            if (poppedCount < orderedTexts.size) {
                val currentTexts = orderedTexts.take(poppedCount + 1)
                binding.balloonResultText.text = currentTexts.joinToString("\n")
                poppedCount++
            }
        }

        binding.balloonView.onAllBalloonsPopped = {
            binding.tapBalloonHint.visibility = View.GONE
            binding.amazingText.visibility = View.VISIBLE
            binding.mainButton.visibility = View.VISIBLE
        }
    }

    private fun setupBalloons(stats: SessionStatsResponse) {
        // Store texts in the order they should appear (do this immediately, not in post)
        orderedTexts.clear()
        orderedTexts.add(formatWordsLine(stats.total_words_read))
        orderedTexts.add(formatPagesLine(stats.total_pages - 1))
        orderedTexts.add(formatTimeLine(stats.total_time_spent))
        poppedCount = 0

        binding.balloonView.post {
            val viewWidth = binding.balloonView.width.toFloat()
            val viewHeight = binding.balloonView.height.toFloat()

            if (viewWidth == 0f || viewHeight == 0f) return@post

            // Calculate balloon size
            val balloonSize = viewWidth.coerceAtMost(viewHeight) / 2f
            val balloonWidth = balloonSize
            val balloonHeight = balloonSize * 1.2f // Vertical ratio of the balloon

            // Compute balloon positions (3 horizontally aligned)
            val spacing = viewWidth / 4f
            val centerY = viewHeight / 2f

            // Create balloon data
            val balloonDataList = listOf(
                BalloonData(
                    x = spacing,
                    y = centerY,
                    width = balloonWidth,
                    height = balloonHeight,
                    color = BalloonColor.RED,
                    lineIndex = 0,
                    text = formatWordsLine(stats.total_words_read)
                ),
                BalloonData(
                    x = spacing * 2f,
                    y = centerY,
                    width = balloonWidth,
                    height = balloonHeight,
                    color = BalloonColor.GREEN,
                    lineIndex = 1,
                    text = formatPagesLine(stats.total_pages - 1)
                ),
                BalloonData(
                    x = spacing * 3f,
                    y = centerY,
                    width = balloonWidth,
                    height = balloonHeight,
                    color = BalloonColor.BLUE,
                    lineIndex = 2,
                    text = formatTimeLine(stats.total_time_spent)
                )
            )

            binding.balloonView.setBalloons(balloonDataList)
        }
    }

    private fun formatWordsLine(wordCount: Int): String {
        val unit = if (wordCount == 1) {
            getString(R.string.unit_word)
        } else {
            getString(R.string.unit_words)
        }
        return getString(R.string.summary_line1, wordCount, unit)
    }

    private fun formatPagesLine(pageCount: Int): String {
        val unit = if (pageCount == 1) {
            getString(R.string.unit_page)
        } else {
            getString(R.string.unit_pages)
        }
        return getString(R.string.summary_line2, pageCount, unit)
    }

    private fun formatTimeLine(totalSeconds: Int): String {
        val timeText = formatTimeText(totalSeconds)
        return getString(R.string.summary_line3, timeText)
    }

    private fun setupClickListeners() {
        binding.mainButton.setOnClickListener {
            handleMainButtonClick()
        }
    }

    private fun handleMainButtonClick() {
        if (isNewSession) {
            navigateToDecideSave()
        } else {
            navigateToMain()
        }
        finish()
    }

    private fun navigateToDecideSave() {
        val intent = Intent(this, DecideSaveActivity::class.java).apply {
            putExtra("session_id", sessionId)
        }
        startActivity(intent)
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
    }

    private fun formatTimeText(totalSeconds: Int): String {
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60

        val minuteText = formatMinutes(minutes)
        val secondText = formatSeconds(seconds)

        return when {
            minuteText.isNotEmpty() && secondText.isNotEmpty() -> "$minuteText $secondText"
            minuteText.isNotEmpty() -> minuteText
            secondText.isNotEmpty() -> secondText
            else -> "0 ${getString(R.string.unit_seconds)}"
        }
    }

    private fun formatMinutes(minutes: Int): String {
        return when {
            minutes > 1 -> "$minutes ${getString(R.string.unit_minutes)}"
            minutes == 1 -> "$minutes ${getString(R.string.unit_minute)}"
            else -> ""
        }
    }

    private fun formatSeconds(seconds: Int): String {
        return when {
            seconds > 1 -> "$seconds ${getString(R.string.unit_seconds)}"
            seconds == 1 -> "$seconds ${getString(R.string.unit_second)}"
            else -> ""
        }
    }

    private fun formatPageText(pageCount: Int): String {
        return if (pageCount == 1) {
            "1 ${getString(R.string.unit_page)}"
        } else {
            "$pageCount ${getString(R.string.unit_pages)}"
        }
    }
}