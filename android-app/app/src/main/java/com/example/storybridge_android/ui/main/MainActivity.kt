package com.example.storybridge_android.ui.main

import android.app.AlertDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.Settings
import android.util.Base64
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.storybridge_android.ui.session.LoadingActivity
import com.example.storybridge_android.R
import com.example.storybridge_android.ui.setting.SettingActivity
import com.example.storybridge_android.ui.session.StartSessionActivity
import com.example.storybridge_android.ui.common.SessionCard
import com.example.storybridge_android.ui.common.TopNavigationBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.storybridge_android.data.SessionRepositoryImpl
import com.example.storybridge_android.data.UserRepositoryImpl
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.*


class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(UserRepositoryImpl(), SessionRepositoryImpl())
    }

    private val settingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            recreate()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupTopNavigationBar()
        setupStartButton()
        observeUserInfo()
        loadUserInfo()
    }

    private fun loadUserInfo() {
        val deviceInfo = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        viewModel.loadUserInfo(deviceInfo)
    }

    private fun observeUserInfo() {
        val container = findViewById<com.google.android.flexbox.FlexboxLayout>(R.id.cardContainer)
        val emptyContainer = findViewById<LinearLayout>(R.id.emptyContainer)

        lifecycleScope.launch {
            viewModel.userInfo.collectLatest { response ->
                if (response == null) return@collectLatest
                if (response.isSuccessful) {
                    val sessions = response.body() ?: return@collectLatest
                    container.removeAllViews()

                    if (sessions.isEmpty()) {
                        container.visibility = LinearLayout.GONE
                        emptyContainer.visibility = LinearLayout.VISIBLE
                        return@collectLatest
                    }

                    for (data in sessions.reversed()) {
                        val sessionCard = SessionCard(this@MainActivity)
                        val title = data.translated_title ?: "NULL"
                        val maxLen = 12
                        val cropped =
                            if (title.length > maxLen) title.substring(0, maxLen) + "…" else title

                        sessionCard.setBookTitle(cropped)

                        val raw = data.started_at.take(10)
                        val parts = raw.split("-")
                        if (parts.size >= 3) {
                            val formatted = "${parts[1]}/${parts[2]}"
                            sessionCard.setBookProgress(formatted)
                        } else {
                            sessionCard.setBookProgress(data.started_at)
                        }

                        if (!data.image_base64.isNullOrEmpty()) {
                            val imageBytes = Base64.decode(data.image_base64, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                            sessionCard.findViewById<ImageView>(R.id.cardBookImage).setImageBitmap(bitmap)
                        }

                        sessionCard.setOnImageClickListener {
                            val intent = Intent(this@MainActivity, LoadingActivity::class.java)
                            intent.putExtra("started_at", data.started_at)
                            startActivity(intent)
                        }
                        sessionCard.setOnTrashClickListener {
                            AlertDialog.Builder(this@MainActivity)
                                .setTitle("삭제 확인")
                                .setMessage("정말로 삭제하시겠습니까?")
                                .setPositiveButton("삭제") { _, _ ->
                                    lifecycleScope.launch {
                                        val deviceInfo = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
                                        viewModel.discardSession(data.session_id, deviceInfo) // session_id 사
                                    }
                                }
                                .setNegativeButton("취소", null)
                                .show()
                        }
                        container.addView(sessionCard)
                    }

                    container.visibility = LinearLayout.VISIBLE
                    emptyContainer.visibility = LinearLayout.GONE
                } else {
                    container.visibility = LinearLayout.GONE
                    emptyContainer.visibility = LinearLayout.VISIBLE
                }
            }
        }
    }

    private fun setupTopNavigationBar() {
        val topNav = findViewById<TopNavigationBar>(R.id.topNavigationBar)
        topNav.setOnSettingsClickListener {
            val intent = Intent(this, SettingActivity::class.java)
            settingsLauncher.launch(intent)
        }
    }

    private fun setupStartButton() {
        val startButton = findViewById<Button>(R.id.startNewReadingButton)
        startButton.setOnClickListener { navigateToStartSession() }
    }

    private fun navigateToStartSession() {
        startActivity(Intent(this, StartSessionActivity::class.java))
    }

    private fun navigateToLoadingSession() {
        startActivity(Intent(this, LoadingActivity::class.java))
    }
}