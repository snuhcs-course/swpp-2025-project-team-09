package com.example.storybridge_android.ui.session.loading

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.viewModels
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.example.storybridge_android.R
import com.example.storybridge_android.ui.reading.ReadingActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.activity.OnBackPressedCallback
import com.example.storybridge_android.ui.common.BaseActivity
import com.example.storybridge_android.ui.session.voice.VoiceSelectActivity

class LoadingActivity : BaseActivity() {
    private lateinit var loadingBar: ProgressBar
    private val viewModel: LoadingViewModel by viewModels {
        LoadingViewModelFactory()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_loading)

        setupUI()
        observeProgress()

        val startedAt = intent.getStringExtra("started_at")
        if (startedAt != null) {
            handleResumeSession(startedAt)
        } else {
            handleNewSession()
        }

        setupBackPressHandler()
    }

    private fun setupUI() {
        loadingBar = findViewById(R.id.loadingBar)
        loadingBar.max = 100
    }

    private fun observeProgress() {
        lifecycleScope.launchWhenStarted {
            viewModel.progress.collectLatest { progress ->
                loadingBar.progress = progress.coerceIn(0, 100)
            }
        }
    }

    private fun handleResumeSession(startedAt: String) {
        lifecycleScope.launch {
            val deviceInfo = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
            viewModel.loadUserInfo(deviceInfo)
        }

        lifecycleScope.launchWhenStarted {
            viewModel.userInfo.collectLatest { response ->
                if (response == null || !response.isSuccessful) return@collectLatest
                val sessions = response.body() ?: return@collectLatest
                val match = sessions.find { it.started_at == startedAt }
                if (match != null) {
                    viewModel.reloadAllSession(match.started_at, this@LoadingActivity)
                } else {
                    showError(getString(R.string.error_session_not_found))
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.navigateToReading.collectLatest { session ->
                session?.let {
                    val realStartIndex = if (it.page_index == 0) 1 else it.page_index
                    navigateToReading(it.session_id, realStartIndex, it.total_pages)
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.error.collectLatest { msg ->
                msg?.let { showError(it) }
            }
        }
    }

    private fun handleNewSession() {
        val sessionId = intent.getStringExtra("session_id")
        val imagePath = intent.getStringExtra("image_path")
        val isCover = intent.getBooleanExtra("is_cover", false)
        val lang = intent.getStringExtra("lang") ?: "en"
        val pageIndex = intent.getIntExtra("page_index", 0)

        if (sessionId == null || imagePath == null) {
            showError(getString(R.string.error_invalid_session_or_image))
            return
        }

        lifecycleScope.launch {
            if (isCover) viewModel.uploadCover(sessionId, lang, imagePath)
            else viewModel.uploadImage(sessionId, pageIndex, lang, imagePath)
        }

        lifecycleScope.launchWhenStarted {
            viewModel.error.collectLatest { msg ->
                msg?.let { showError(it) }
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.navigateToVoice.collectLatest { result ->
                result?.let {
                    navigateToVoiceSelect(sessionId, it.title)
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.status.collectLatest {
                if (it == "ready") {
                    navigateToReading(sessionId, pageIndex, pageIndex + 1)
                }
            }
        }
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Toast.makeText(this@LoadingActivity, getString(R.string.exit_loading), Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun navigateToReading(sessionId: String, pageIndex: Int, totalPages: Int = pageIndex + 1) {
        // Check if this is a new session (no started_at) or existing session (has started_at)
        val isNewSession = intent.getStringExtra("started_at") == null

        val intent = Intent(this, ReadingActivity::class.java).apply {
            putExtra("session_id", sessionId)
            putExtra("page_index", pageIndex)
            putExtra("total_pages", totalPages)
            putExtra("is_new_session", isNewSession)
        }
        startActivity(intent)
        finish()
    }

    private fun navigateToVoiceSelect(
        sessionId: String,
        title: String
    ) {
        val intent = Intent(this, VoiceSelectActivity::class.java).apply {
            putExtra("session_id", sessionId)
            putExtra("book_title", title)
        }
        startActivity(intent)
        finish()
    }

    private fun showError(message: String) {
        Toast.makeText(
            this,
            getString(R.string.error_generic, message),
            Toast.LENGTH_LONG
        ).show()

        finish()
    }
}