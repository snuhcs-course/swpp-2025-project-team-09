package com.example.storybridge_android.ui.session

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.example.storybridge_android.data.SessionRepositoryImpl
import com.example.storybridge_android.databinding.ActivityFinishBinding
import com.example.storybridge_android.ui.main.MainActivity

class FinishActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFinishBinding
    private val viewModel: FinishViewModel by viewModels {
        FinishViewModelFactory(SessionRepositoryImpl())
    }
    private var isNewSession = true
    private lateinit var sessionId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityFinishBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionId = intent.getStringExtra("session_id") ?: ""
        isNewSession = intent.getBooleanExtra("is_new_session", true)

        viewModel.endSession(sessionId)

        viewModel.sessionStats.observe(this, Observer { stats ->
            val totalSeconds = stats.total_time_spent
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60

            val minuteText = if (minutes > 0) {
                "$minutes ${if (minutes == 1) "minute" else "minutes"}"
            } else ""
            val secondText = if (seconds > 0) {
                "$seconds ${if (seconds == 1) "second" else "seconds"}"
            } else ""
            val timeText = when {
                minuteText.isNotEmpty() && secondText.isNotEmpty() -> "$minuteText $secondText"
                minuteText.isNotEmpty() -> minuteText
                secondText.isNotEmpty() -> secondText
                else -> "0 seconds"
            }
            val pageCount = stats.total_pages - 1
            val pageText = if (pageCount == 1) "1 page" else "$pageCount pages"
            binding.sessionSummary.text = "You read ${stats.total_words_read} words and $pageText\nfor $timeText. Amazing!"
            binding.sessionSummary.visibility = View.VISIBLE
        })


        viewModel.showMainButton.observe(this, Observer { show ->
            binding.mainButton.visibility = if (show) View.VISIBLE else View.INVISIBLE
        })

        binding.mainButton.setOnClickListener {
            if (isNewSession) {
                // New session: go to DecideSaveActivity to choose save/discard
                val intent = Intent(this, DecideSaveActivity::class.java).apply {
                    putExtra("session_id", sessionId)
                }
                startActivity(intent)
            } else {
                // Existing session: go directly to MainActivity
                startActivity(Intent(this, MainActivity::class.java))
            }
            finish()
        }
    }
}
