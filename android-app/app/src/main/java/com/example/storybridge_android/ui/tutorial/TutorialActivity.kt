package com.example.storybridge_android.ui.tutorial

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.storybridge_android.R
import com.example.storybridge_android.ui.main.MainActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class TutorialPage(
    val imageRes: Int,
    val description: String
)

class TutorialActivity : AppCompatActivity() {

    private val viewModel: TutorialViewModel by viewModels { TutorialViewModelFactory() }

    private lateinit var nextBtn: Button
    private lateinit var imageView: ImageView
    private lateinit var descView: TextView
    private lateinit var indicatorLayout: LinearLayout
    private lateinit var tutorialList: List<TutorialPage>
    private var currentIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tutorial)

        imageView = findViewById(R.id.tutorialImage)
        descView = findViewById(R.id.instructionText)
        nextBtn = findViewById(R.id.nextButton)
        indicatorLayout = findViewById(R.id.indicatorLayout)

        tutorialList = listOf(
            TutorialPage(R.drawable.tutorial_1, getString(R.string.tutorial_1)),
            TutorialPage(R.drawable.tutorial_2, getString(R.string.tutorial_2)),
            TutorialPage(R.drawable.tutorial_3, getString(R.string.tutorial_3)),
            TutorialPage(R.drawable.tutorial_4, getString(R.string.tutorial_4)),
            TutorialPage(R.drawable.tutorial_5, getString(R.string.tutorial_5)),
            TutorialPage(R.drawable.tutorial_6, getString(R.string.tutorial_6)),
            TutorialPage(R.drawable.tutorial_7, getString(R.string.tutorial_7)),
            TutorialPage(R.drawable.tutorial_8, getString(R.string.tutorial_8))
        )

        updateUI(0)

        lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                when (state) {
                    is TutorialUiState.Loading -> {
                        nextBtn.isEnabled = false
                        nextBtn.text = getString(R.string.tutorial_loading)
                    }
                    is TutorialUiState.NavigateMain -> {
                        startActivity(Intent(this@TutorialActivity, MainActivity::class.java))
                        finish()
                    }
                    is TutorialUiState.Error -> {
                        nextBtn.isEnabled = true
                        getString(R.string.tutorial_start)
                        Toast.makeText(this@TutorialActivity, state.message, Toast.LENGTH_SHORT).show()
                    }
                    else -> {}
                }
            }
        }

        nextBtn.setOnClickListener {
            if (currentIndex < tutorialList.size - 1) {
                currentIndex++
                updateUI(currentIndex)
            } else {
                val deviceId = fetchDeviceId()
                viewModel.startApp(deviceId)
            }
        }
    }

    private fun updateUI(index: Int) {
        val page = tutorialList[index]
        imageView.setImageResource(page.imageRes)
        descView.text = page.description
        nextBtn.text = if (index < tutorialList.size - 1)
            getString(R.string.next)
        else
            getString(R.string.tutorial_start)

        for (i in 0 until indicatorLayout.childCount) {
            val dot = indicatorLayout.getChildAt(i)
            val drawableRes = if (i == index) R.drawable.circle_dark else R.drawable.circle_gray
            dot.setBackgroundResource(drawableRes)
        }
    }

    private fun fetchDeviceId(): String {
        return Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown_id"
    }
}
