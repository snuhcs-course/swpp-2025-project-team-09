package com.example.storybridge_android.ui.session.loading

import android.content.Intent
import android.os.Bundle
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.example.storybridge_android.ui.common.BaseActivity

class LoadingActivity : BaseActivity() {
    private lateinit var loadingBar: ProgressBar
    private lateinit var balloonOverlay: LoadingBalloonOverlayView
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
        balloonOverlay = findViewById(R.id.balloonOverlay)
    }

    private fun observeProgress() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.progress.collectLatest { progress ->
                    loadingBar.progress = progress.coerceIn(0, 100)
                }
            }
        }
    }

    private fun handleResumeSession(startedAt: String) {
        lifecycleScope.launch {
            viewModel.loadUserInfo(deviceInfo)
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
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
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.navigateToReading.collectLatest { session ->
                    session?.let {
                        val realStartIndex = if (it.page_index == 0) 1 else it.page_index
                        navigateToReading(it.session_id, realStartIndex, it.total_pages)
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.error.collectLatest { msg ->
                    msg?.let {
                        setResult(RESULT_CANCELED, Intent().apply {
                            putExtra("retake", true)
                        })
                        finish()
                    }
                }
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

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.error.collectLatest { msg ->
                    msg?.let {
                        setResult(RESULT_CANCELED, Intent().apply {
                            putExtra("retake", true)
                        })
                        finish()
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.status.collectLatest {
                    when (it) {
                        "ready" -> {
                            navigateToReading(sessionId, pageIndex, pageIndex + 1)
                        }
                        "cover_ready" -> {
                            setResult(RESULT_OK)
                            finish()
                        }
                    }
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

    private fun showError(message: String) {
        Toast.makeText(
            this,
            getString(R.string.error_generic, message),
            Toast.LENGTH_LONG
        ).show()

        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        balloonOverlay.stopAnimation()
    }
}