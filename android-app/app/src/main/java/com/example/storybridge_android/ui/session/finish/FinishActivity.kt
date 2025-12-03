package com.example.storybridge_android.ui.session.finish

import com.example.storybridge_android.R
import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.Observer
import com.example.storybridge_android.data.BalloonColor
import com.example.storybridge_android.data.CongratulationBalloon
import com.example.storybridge_android.databinding.ActivityFinishBinding
import com.example.storybridge_android.network.SessionStatsResponse
import com.example.storybridge_android.ui.common.BaseActivity
import com.example.storybridge_android.ui.main.MainActivity
import com.example.storybridge_android.ui.session.decide.DecideSaveActivity
import com.example.storybridge_android.ui.reading.ReadingActivity
import android.view.View

class FinishActivity : BaseActivity() {

    private lateinit var binding: ActivityFinishBinding
    private val viewModel: FinishViewModel by viewModels {
        FinishViewModelFactory()
    }
    private var isNewSession = true
    private lateinit var sessionId: String
    private var totalPages = 0
    private var currentPageIndex = 0

    // Store texts in order (regardless of which balloon is popped)
    private val orderedTexts = mutableListOf<String>()
    private var poppedCount = 0
    private var pickedWordsLoaded = false
    private var allBalloonsPopped = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityFinishBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeSessionData()
        setupObservers()
        setupClickListeners()
        setupFlipCardListeners()
        setupBackPressHandler()

        viewModel.endSession(sessionId)
        viewModel.pickWords(sessionId)
    }

    private fun initializeSessionData() {
        sessionId = intent.getStringExtra("session_id") ?: ""
        isNewSession = intent.getBooleanExtra("is_new_session", true)
        totalPages = intent.getIntExtra("total_pages", 0)
        currentPageIndex = intent.getIntExtra("page_index", 0)
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Go back to reading activity at the page user was on
                navigateBackToReading()
            }
        })
    }

    private fun setupObservers() {
        observeSessionStats()
        setupBalloonCallback()
        viewModel.pickedWords.observe(this) { items ->
            if (items.size >= 3) {
                binding.card1.setData(items[0].word, items[0].meaning_ko)
                binding.card2.setData(items[1].word, items[1].meaning_ko)
                binding.card3.setData(items[2].word, items[2].meaning_ko)
                pickedWordsLoaded = true
                if (allBalloonsPopped) {
                    binding.learnedWordsContainer.visibility = View.VISIBLE
                    binding.learnedWordsTitle.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun observeSessionStats() {
        viewModel.sessionStats.observe(this, Observer { stats ->
            // Store total pages from stats if not provided
            if (totalPages == 0) {
                totalPages = stats.total_pages
            }
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
            allBalloonsPopped = true
            binding.tapBalloonHint.visibility = View.GONE
            binding.amazingText.visibility = View.GONE
            binding.mainButton.visibility = View.GONE

            if (pickedWordsLoaded) {
                binding.learnedWordsContainer.visibility = View.VISIBLE
                binding.learnedWordsTitle.visibility = View.VISIBLE
            }
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
                CongratulationBalloon(
                    x = spacing,
                    y = centerY,
                    width = balloonWidth,
                    height = balloonHeight,
                    color = BalloonColor.RED,
                    lineIndex = 0,
                    text = formatWordsLine(stats.total_words_read)
                ),
                CongratulationBalloon(
                    x = spacing * 2f,
                    y = centerY,
                    width = balloonWidth,
                    height = balloonHeight,
                    color = BalloonColor.GREEN,
                    lineIndex = 1,
                    text = formatPagesLine(stats.total_pages - 1)
                ),
                CongratulationBalloon(
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

    private fun navigateBackToReading() {
        val intent = Intent(this, ReadingActivity::class.java).apply {
            putExtra("session_id", sessionId)
            putExtra("page_index", currentPageIndex) // Go back to the page user was on
            putExtra("total_pages", totalPages)
            putExtra("is_new_session", isNewSession)
        }
        startActivity(intent)
        finish()
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

    private fun setupFlipCardListeners() {
        val card1 = binding.card1
        val card2 = binding.card2
        val card3 = binding.card3

        var card1Flipped = false
        var card2Flipped = false
        var card3Flipped = false

        fun checkAllFlipped() {
            if (card1Flipped && card2Flipped && card3Flipped) {
                binding.amazingText.visibility = View.VISIBLE
                binding.mainButton.visibility = View.VISIBLE
            }
        }

        card1.onFlipped = {
            if (!card1Flipped) {
                card1Flipped = true
                checkAllFlipped()
            }
        }
        card2.onFlipped = {
            if (!card2Flipped) {
                card2Flipped = true
                checkAllFlipped()
            }
        }
        card3.onFlipped = {
            if (!card3Flipped) {
                card3Flipped = true
                checkAllFlipped()
            }
        }
    }

}