package com.example.storybridge_android.ui.session.decide

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.storybridge_android.R
import com.example.storybridge_android.databinding.ActivityDecideSaveBinding
import com.example.storybridge_android.ui.common.BaseActivity
import com.example.storybridge_android.ui.main.MainActivity
import com.example.storybridge_android.ui.session.finish.FinishActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityDecideSaveBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionId = intent.getStringExtra("session_id") ?: ""

        initListener()
        observeViewModel()

        // Add back button handler - goes back to congratulations page
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateBackToFinish()
            }
        })
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

    private fun updateButtonState(selected: Button) {
        listOf(binding.btnSave, binding.btnDiscard).forEach { it.isSelected = it == selected }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collectLatest { state ->
                    when (state) {
                        is DecideSaveUiState.Idle -> Unit

                        is DecideSaveUiState.Saved -> {
                            Toast.makeText(
                                this@DecideSaveActivity,
                                getString(R.string.info_session_saved),
                                Toast.LENGTH_SHORT
                            ).show()
                            navigateToMain()
                        }

                        is DecideSaveUiState.Discarded -> {
                            Toast.makeText(
                                this@DecideSaveActivity,
                                getString(R.string.info_session_discarded),
                                Toast.LENGTH_SHORT
                            ).show()
                            navigateToMain()
                        }

                        is DecideSaveUiState.Error -> {
                            val errorMessage = getString(R.string.error_session_action_failed, state.message)
                            Toast.makeText(
                                this@DecideSaveActivity,
                                errorMessage,
                                Toast.LENGTH_LONG
                            ).show()
                            selectedAction = null
                            binding.btnSave.isSelected = false
                            binding.btnDiscard.isSelected = false
                            binding.mainButton.visibility = View.INVISIBLE
                            binding.mainButton.isEnabled = false
                        }

                        is DecideSaveUiState.Loading -> {
                        }
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

    private fun navigateBackToFinish() {
        val intent = Intent(this, FinishActivity::class.java).apply {
            putExtra("session_id", sessionId)
            putExtra("is_new_session", true)
        }
        startActivity(intent)
        finish()
    }
}