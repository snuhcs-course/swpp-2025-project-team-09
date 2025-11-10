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
import com.example.storybridge_android.ui.main.MainActivity

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
                setButtonSelected(binding.btnSave)
                handleSave()
            }
        }

        binding.btnDiscard.setOnClickListener {
            if (!decisionMade) {
                setButtonSelected(binding.btnDiscard)
                handleDiscard()
            }
        }

        binding.mainButton.setOnClickListener {
            navigateToMain()
        }
    }

    private fun setButtonSelected(selectedButton: View) {
        // Reset all buttons
        binding.btnSave.isSelected = false
        binding.btnDiscard.isSelected = false

        // Set selected button
        selectedButton.isSelected = true
    }

    private fun handleSave() {
        decisionMade = true
        Toast.makeText(this, "Session saved!", Toast.LENGTH_SHORT).show()
        showMainButton()
    }

    private fun handleDiscard() {
        decisionMade = true
        Log.d(TAG, "Attempting to discard session: $sessionId")

        lifecycleScope.launch {
            try {
                val result = repository.discardSession(sessionId)
                if (result.isSuccess) {
                    Log.d(TAG, "Session discarded successfully")
                    Toast.makeText(this@DecideSaveActivity, "Session discarded", Toast.LENGTH_SHORT).show()
                    showMainButton()
                } else {
                    val error = result.exceptionOrNull()
                    Log.e(TAG, "Failed to discard session: ${error?.message}", error)
                    Toast.makeText(this@DecideSaveActivity, "Failed to discard session: ${error?.message}", Toast.LENGTH_LONG).show()
                    decisionMade = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error discarding session", e)
                Toast.makeText(this@DecideSaveActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                decisionMade = false
            }
        }
    }

    private fun showMainButton() {
        binding.mainButton.visibility = View.VISIBLE
        binding.mainButton.isEnabled = true
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}