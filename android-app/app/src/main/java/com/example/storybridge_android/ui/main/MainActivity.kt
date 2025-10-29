package com.example.storybridge_android.ui.main

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
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.storybridge_android.data.DefaultUserRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/*
class MainActivity : AppCompatActivity() {
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
        loadSessionCard()
    }

    private fun navigateToSettings() {
        startActivity(Intent(this, SettingActivity::class.java))
    }

    private fun navigateToStartSession() {
        startActivity(Intent(this, StartSessionActivity::class.java))
    }

    private fun navigateToLoadingSession() {
        startActivity(Intent(this, LoadingActivity::class.java))
    }

    private fun loadSessionCard() {
        val deviceInfo = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ANDROID_ID
        )
        val call = RetrofitClient.userApi.userInfo(deviceInfo)

        val container = findViewById<LinearLayout>(R.id.cardContainer)
        val emptyContainer = findViewById<LinearLayout>(R.id.emptyContainer)

        call.enqueue(object : Callback<UserInfoResponse> {
            override fun onResponse(
                call: Call<UserInfoResponse>,
                response: Response<UserInfoResponse>
            ) {
                if (response.isSuccessful) {
                    val data = response.body()

                    if (data == null || data.title.isNullOrEmpty() || data.started_at.isNullOrEmpty()) {
                        container.visibility = LinearLayout.GONE
                        emptyContainer.visibility = LinearLayout.VISIBLE
                        return
                    }

                    container.removeAllViews()
                    val sessionCard = SessionCard(this@MainActivity)
                    sessionCard.setBookTitle(data.title)
                    sessionCard.setBookProgress("Started: ${data.started_at}")
                    sessionCard.setOnNextClickListener { navigateToLoadingSession() }

                    if (!data.image_base64.isNullOrEmpty()) {
                        val imageBytes = Base64.decode(data.image_base64, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                        sessionCard.findViewById<ImageView>(R.id.bookImage).setImageBitmap(bitmap)
                    }

                    container.addView(sessionCard)
                    container.visibility = LinearLayout.VISIBLE
                    emptyContainer.visibility = LinearLayout.GONE

                } else {
                    Log.e("MainActivity", "Error: ${response.code()}")
                    container.visibility = LinearLayout.GONE
                    emptyContainer.visibility = LinearLayout.VISIBLE
                }
            }

            override fun onFailure(call: Call<UserInfoResponse>, t: Throwable) {
                Log.e("MainActivity", "API failed: ${t.message}")
                container.visibility = LinearLayout.GONE
                emptyContainer.visibility = LinearLayout.VISIBLE
            }
        })
    }


    /**
     * TopNavigationBar의 클릭 이벤트 연결
     */
    private fun setupTopNavigationBar() {
        val topNav = findViewById<TopNavigationBar>(R.id.topNavigationBar)
        topNav.setOnSettingsClickListener {
            navigateToSettings()
        }
    }

    /**
     * "Start New Reading" 버튼 초기화 및 클릭 이벤트 연결
     */
    private fun setupStartButton() {
        val startButton = findViewById<Button>(R.id.startNewReadingButton)
        startButton.setOnClickListener {
            navigateToStartSession()
        }
    }
}

 */

class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(DefaultUserRepository())
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
        val container = findViewById<LinearLayout>(R.id.cardContainer)
        val emptyContainer = findViewById<LinearLayout>(R.id.emptyContainer)

        lifecycleScope.launch {
            viewModel.userInfo.collectLatest { response ->
                if (response == null) return@collectLatest
                if (response.isSuccessful) {
                    val dataList = response.body()
                    val data = dataList?.firstOrNull()
                    if (data == null || data.title.isNullOrEmpty() || data.started_at.isNullOrEmpty()) {
                        container.visibility = LinearLayout.GONE
                        emptyContainer.visibility = LinearLayout.VISIBLE
                        return@collectLatest
                    }
                    container.removeAllViews()
                    val sessionCard = SessionCard(this@MainActivity)
                    sessionCard.setBookTitle(data.title)
                    sessionCard.setBookProgress("Started: ${data.started_at}")
                    sessionCard.setOnNextClickListener { navigateToLoadingSession() }

                    if (!data.image_base64.isNullOrEmpty()) {
                        val imageBytes = Base64.decode(data.image_base64, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                        sessionCard.findViewById<ImageView>(R.id.bookImage).setImageBitmap(bitmap)
                    }

                    container.addView(sessionCard)
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
        topNav.setOnSettingsClickListener { navigateToSettings() }
    }

    private fun setupStartButton() {
        val startButton = findViewById<Button>(R.id.startNewReadingButton)
        startButton.setOnClickListener { navigateToStartSession() }
    }

    private fun navigateToSettings() {
        startActivity(Intent(this, SettingActivity::class.java))
    }

    private fun navigateToStartSession() {
        startActivity(Intent(this, StartSessionActivity::class.java))
    }

    private fun navigateToLoadingSession() {
        startActivity(Intent(this, LoadingActivity::class.java))
    }
}