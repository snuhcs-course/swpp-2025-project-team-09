package com.example.storybridge_android.ui.session

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.viewModels
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.storybridge_android.R
import com.example.storybridge_android.data.SessionRepositoryImpl
import com.example.storybridge_android.ui.camera.CameraSessionActivity
import kotlinx.coroutines.flow.collectLatest

class StartSessionActivity : AppCompatActivity() {

    private val viewModel: StartSessionViewModel by viewModels {
        StartSessionViewModelFactory(SessionRepositoryImpl())
    }

    companion object {
        private const val TAG = "StartSessionActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_start_session)

        val startButton = findViewById<Button>(R.id.startSessionButton)
        startButton.setOnClickListener {
            startNewSession()
        }

        observeViewModel()
    }

    private fun startNewSession() {
        val deviceInfo = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        viewModel.startSession(deviceInfo)
    }

    private fun observeViewModel() {
        lifecycleScope.launchWhenStarted {
            viewModel.state.collectLatest { state ->
                when (state) {
                    is StartSessionUiState.Idle -> {}
                    is StartSessionUiState.Loading -> Log.d(TAG, "Session starting...")
                    is StartSessionUiState.Success -> {
                        Log.d(TAG, "Session started: ${state.sessionId}")
                        navigateToCameraForCover(state.sessionId)
                    }
                    is StartSessionUiState.Error -> {
                        Toast.makeText(this@StartSessionActivity, state.message, Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            }
        }
    }

    private fun navigateToCameraForCover(sessionId: String) {
        val intent = Intent(this, CameraSessionActivity::class.java)
        intent.putExtra("session_id", sessionId)
        intent.putExtra("page_index", 0)
        intent.putExtra("is_cover", true)
        startActivity(intent)
        finish()
    }
}
