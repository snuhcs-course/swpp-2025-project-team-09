package com.example.storybridge_android.ui.session

import android.content.Intent
import android.os.Bundle
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.storybridge_android.R
import com.example.storybridge_android.data.ProcessRepositoryImpl
import com.example.storybridge_android.ui.reading.ReadingActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LoadingActivity : AppCompatActivity() {

    private lateinit var loadingBar: ProgressBar

    private val viewModel: LoadingViewModel by viewModels {
        LoadingViewModelFactory(ProcessRepositoryImpl())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading)

        loadingBar = findViewById(R.id.loadingBar)
        loadingBar.max = 100

        val sessionId = intent.getStringExtra("session_id")
        val imagePath = intent.getStringExtra("image_path")
        val isCover = intent.getBooleanExtra("is_cover", false)
        val lang = intent.getStringExtra("lang") ?: "en"
        val pageIndex = intent.getIntExtra("page_index", 0)

        if (sessionId == null || imagePath == null) {
            showError("Invalid session or image path")
            return
        }

        lifecycleScope.launch {
            if (isCover) viewModel.uploadCover(sessionId, lang, imagePath)
            else viewModel.uploadImage(sessionId, pageIndex, lang, imagePath)
        }

        lifecycleScope.launchWhenStarted {
            viewModel.progress.collectLatest { progress ->
                loadingBar.progress = progress.coerceIn(0, 100)
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.error.collectLatest { msg ->
                msg?.let { showError(it) }
            }
        }
        
        lifecycleScope.launchWhenStarted {
            viewModel.navigateToVoice.collectLatest { result ->
                result?.let {
                    navigateToVoiceSelect(sessionId, it.title, it.maleTts, it.femaleTts)
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.status.collectLatest {
                if (it == "ready") {
                    navigateToReading(sessionId, pageIndex)
                }
            }
        }
    }

    private fun navigateToReading(sessionId: String, pageIndex: Int) {
        val intent = Intent(this, ReadingActivity::class.java).apply {
            putExtra("session_id", sessionId)
            putExtra("page_index", pageIndex)
        }
        startActivity(intent)
        finish()
    }

    private fun navigateToVoiceSelect(sessionId: String, title: String, maleTts: String?, femaleTts: String?) {
        val intent = Intent(this, VoiceSelectActivity::class.java).apply {
            putExtra("session_id", sessionId)
            putExtra("book_title", title)
            putExtra("male_tts", maleTts)
            putExtra("female_tts", femaleTts)
        }
        startActivity(intent)
        finish()
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        finish()
    }
}
