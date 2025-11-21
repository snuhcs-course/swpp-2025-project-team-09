package com.example.storybridge_android.ui.session

import com.example.storybridge_android.R
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.Observer
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
        observeMainButton()
    }

    private fun observeSessionStats() {
        viewModel.sessionStats.observe(this, Observer { stats ->
            val summaryText = formatSessionSummary(stats)
            binding.sessionSummary.text = summaryText
            binding.sessionSummary.visibility = View.VISIBLE
        })
    }

    private fun observeMainButton() {
        viewModel.showMainButton.observe(this, Observer { show ->
            binding.mainButton.visibility = if (show) View.VISIBLE else View.INVISIBLE
        })
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

    private fun formatSessionSummary(stats: SessionStatsResponse): String {
        val timeText = formatTimeText(stats.total_time_spent)
        val pageText = formatPageText(stats.total_pages - 1)

        return getString(
            R.string.summary_text,
            stats.total_words_read,
            pageText,
            timeText
        )
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