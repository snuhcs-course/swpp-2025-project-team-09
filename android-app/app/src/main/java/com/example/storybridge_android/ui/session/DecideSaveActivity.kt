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
import com.example.storybridge_android.R

class DecideSaveActivity : BaseActivity() {
    private lateinit var binding: ActivityDecideSaveBinding
    private lateinit var sessionId: String
    private val viewModel: DecideSaveActivityViewModel by viewModels {
        DecideSaveActivityViewModelFactory()
    }
    private var selectedAction: SaveAction? = null

    private enum class SaveAction {
        SAVE, DISCARD
    }

    private var decisionMade: Boolean = false

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
        binding.mainButton.isEnabled = false

        binding.btnSave.setOnClickListener {
            selectedAction = SaveAction.SAVE
            updateButtonState(binding.btnSave)
            showMainButton()
        }

        binding.btnDiscard.setOnClickListener {
            selectedAction = SaveAction.DISCARD
            updateButtonState(binding.btnDiscard)
            showMainButton()
        }

        binding.mainButton.setOnClickListener {
            when (selectedAction) {
                SaveAction.SAVE -> viewModel.saveSession()
                SaveAction.DISCARD -> viewModel.discardSession(sessionId)
                null -> return@setOnClickListener
            }
        }
    }

    private fun updateButtonState(selected: android.widget.Button) {
        listOf(binding.btnSave, binding.btnDiscard).forEach { it.isSelected = it == selected }
    }

    private fun observeViewModel() {
        lifecycleScope.launchWhenStarted {
            viewModel.state.collectLatest { state ->
                when (state) {
                    is DecideSaveUiState.Idle -> Unit

                    is DecideSaveUiState.Saved -> {
                        Toast.makeText(
                            this@DecideSaveActivity,
                            getString(R.string.info_session_saved),
                            Toast.LENGTH_SHORT
                        ).show()
                        showMainButton()
                    }

                    is DecideSaveUiState.Discarded -> {
                        Toast.makeText(
                            this@DecideSaveActivity,
                            getString(R.string.info_session_discarded),
                            Toast.LENGTH_SHORT
                        ).show()
                        showMainButton()
                    }

                    is DecideSaveUiState.Error -> {
                        Toast.makeText(
                            this@DecideSaveActivity,
                            getString(R.string.error_session_action_failed, state.message),
                            Toast.LENGTH_LONG
                        ).show()
                        decisionMade = false
                    }

                    is DecideSaveUiState.Loading -> {
                    }
                }
            }
        }
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
