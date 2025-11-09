package com.example.storybridge_android.ui.session

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.storybridge_android.data.SessionRepositoryImpl
import com.example.storybridge_android.databinding.ActivityDecideSaveBinding
import kotlinx.coroutines.launch
import androidx.activity.enableEdgeToEdge

class DecideSaveActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDecideSaveBinding
    private lateinit var sessionId: String
    private val repository = SessionRepositoryImpl()
    private var decisionMade = false

    companion object {
        private const val TAG = "DecideSaveActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityDecideSaveBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionId = intent.getStringExtra("session_id") ?: ""

        if (sessionId.isEmpty()) {
            Log.e(TAG, "No session_id provided")
            Toast.makeText(this, "Error: No session ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnSave.setOnClickListener {
            if (!decisionMade) {
                handleSave()
            }
        }

        binding.btnDiscard.setOnClickListener {
            if (!decisionMade) {
                handleDiscard()
            }
        }

        binding.mainButton.setOnClickListener {
            navigateToFinish()
        }
    }

    private fun handleSave() {
        decisionMade = true
        Toast.makeText(this, "Session saved!", Toast.LENGTH_SHORT).show()
        showMainButton()
    }

    private fun handleDiscard() {
        decisionMade = true

        lifecycleScope.launch {
            try {
                val result = repository.discardSession(sessionId)
                if (result.isSuccess) {
                    Toast.makeText(this@DecideSaveActivity, "Session discarded", Toast.LENGTH_SHORT).show()
                    showMainButton()
                } else {
                    Toast.makeText(this@DecideSaveActivity, "Failed to discard session", Toast.LENGTH_SHORT).show()
                    decisionMade = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error discarding session", e)
                Toast.makeText(this@DecideSaveActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                decisionMade = false
            }
        }
    }

    private fun showMainButton() {
        binding.mainButton.visibility = View.VISIBLE
        binding.mainButton.isEnabled = true
    }

    private fun navigateToFinish() {
        val intent = Intent(this, FinishActivity::class.java).apply {
            putExtra("session_id", sessionId)
        }
        startActivity(intent)
        finish()
    }
}