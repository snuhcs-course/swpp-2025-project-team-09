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
import com.example.storybridge_android.ui.common.BaseActivity
import com.example.storybridge_android.ui.main.MainActivity
import kotlinx.coroutines.flow.collectLatest

class DecideSaveActivity : BaseActivity() {

    private lateinit var binding: ActivityDecideSaveBinding
    private lateinit var sessionId: String

    private val viewModel: DecideSaveActivityViewModel by viewModels {
        DecideSaveActivityViewModelFactory()
    }

    private var decisionMade = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityDecideSaveBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionId = intent.getStringExtra("session_id") ?: ""

        initListener()
        observeViewModel()
    }

    private fun initListener() {
        binding.btnSave.setOnClickListener {
            if (!decisionMade) {
                decisionMade = true
                viewModel.saveSession()
            }
        }

        binding.btnDiscard.setOnClickListener {
            if (!decisionMade) {
                decisionMade = true
                viewModel.discardSession(sessionId)
            }
        }

        binding.mainButton.setOnClickListener { navigateToMain() }
    }

    private fun observeViewModel() {
        lifecycleScope.launchWhenStarted {
            viewModel.state.collectLatest { state ->
                when (state) {
                    is DecideSaveUiState.Idle -> Unit

                    is DecideSaveUiState.Saved -> {
                        Toast.makeText(this@DecideSaveActivity, "Session saved!", Toast.LENGTH_SHORT).show()
                        showMainButton()
                    }

                    is DecideSaveUiState.Discarded -> {
                        Toast.makeText(this@DecideSaveActivity, "Session discarded", Toast.LENGTH_SHORT).show()
                        showMainButton()
                    }

                    is DecideSaveUiState.Error -> {
                        Toast.makeText(this@DecideSaveActivity, "Failed: ${state.message}", Toast.LENGTH_LONG).show()
                        decisionMade = false
                    }

                    is DecideSaveUiState.Loading -> {
                    }
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
