package com.example.storybridge_android.ui.session

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFinishBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sessionId = intent.getStringExtra("session_id") ?: ""
        viewModel.endSession(sessionId)

        viewModel.sessionStats.observe(this, Observer { stats ->
            binding.sessionSummary.text = """
                Session: ${stats.session_id}
                Pages: ${stats.total_pages}
                Time: ${stats.total_time_spent}s
                Words: ${stats.total_words_read}
            """.trimIndent()
            binding.sessionSummary.visibility = View.VISIBLE
        })

        viewModel.showMainButton.observe(this, Observer { show ->
            binding.mainButton.visibility = if (show) View.VISIBLE else View.INVISIBLE
        })

        binding.mainButton.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}
