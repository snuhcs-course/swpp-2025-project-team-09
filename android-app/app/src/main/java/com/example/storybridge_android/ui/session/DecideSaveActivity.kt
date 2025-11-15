package com.example.storybridge_android.ui.session

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.storybridge_android.databinding.ActivityDecideSaveBinding
import com.example.storybridge_android.ui.main.MainActivity
import kotlinx.coroutines.flow.collectLatest

class DecideSaveActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDecideSaveBinding
    private lateinit var sessionId: String

    private val viewModel: DecideSaveViewModel by viewModels {
        DecideSaveViewModelFactory()
    }

    private var decisionMade = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityDecideSaveBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionId = intent.getStringExtra("session_id") ?: ""

        if (sessionId.isEmpty()) {
            Toast.makeText(this, "No session_id", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupListeners()
        observeState()
    }

    private fun setupListeners() {
        binding.btnSave.setOnClickListener {
            if (!decisionMade) {
                setButtonSelected(binding.btnSave)
                decisionMade = true
                viewModel.save()
                disableButtons()
            }
        }

        binding.btnDiscard.setOnClickListener {
            if (!decisionMade) {
                setButtonSelected(binding.btnDiscard)
                decisionMade = true
                viewModel.discard(sessionId)
                disableButtons()
            }
        }

        binding.mainButton.setOnClickListener {
            navigateToMain()
        }
    }

    private fun observeState() {
        lifecycleScope.launchWhenStarted {
            viewModel.state.collectLatest { state ->
                when (state) {

                    is DecideSaveUiState.SaveSuccess -> {
                        Toast.makeText(this@DecideSaveActivity, "Session saved!", Toast.LENGTH_SHORT).show()
                        showMainButton()
                    }

                    is DecideSaveUiState.DiscardSuccess -> {
                        Toast.makeText(this@DecideSaveActivity, "Session discarded", Toast.LENGTH_SHORT).show()
                        showMainButton()
                    }

                    is DecideSaveUiState.DiscardError -> {
                        Toast.makeText(this@DecideSaveActivity, "Failed: ${state.msg}", Toast.LENGTH_LONG).show()

                        decisionMade = false
                        enableButtons()
                        binding.btnSave.isSelected = false
                        binding.btnDiscard.isSelected = false
                    }

                    else -> {}
                }
            }
        }
    }

    private fun setButtonSelected(selected: View) {
        binding.btnSave.isSelected = false
        binding.btnDiscard.isSelected = false
        selected.isSelected = true
    }

    private fun disableButtons() {
        binding.btnSave.isEnabled = false
        binding.btnDiscard.isEnabled = false
    }

    private fun enableButtons() {
        binding.btnSave.isEnabled = true
        binding.btnDiscard.isEnabled = true
    }

    private fun showMainButton() {
        binding.mainButton.visibility = View.VISIBLE
        binding.mainButton.isEnabled = true
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
