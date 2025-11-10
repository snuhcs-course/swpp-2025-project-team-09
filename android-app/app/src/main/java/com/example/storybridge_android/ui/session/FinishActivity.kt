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
            val minutes = stats.total_time_spent / 60
            binding.sessionSummary.text = "You read ${stats.total_words_read} words and ${stats.total_pages} pages for $minutes minutes. Amazing!"
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
