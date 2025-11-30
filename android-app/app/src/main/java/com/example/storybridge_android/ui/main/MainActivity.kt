package com.example.storybridge_android.ui.main

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.Settings
import android.util.Base64
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.storybridge_android.R
import com.example.storybridge_android.network.UserInfoResponse
import com.example.storybridge_android.ui.common.BaseActivity
import com.example.storybridge_android.ui.common.SessionCard
import com.example.storybridge_android.ui.common.TopNavigationBar
import com.example.storybridge_android.ui.session.loading.LoadingActivity
import com.example.storybridge_android.ui.session.start.StartSessionActivity
import com.example.storybridge_android.ui.setting.SettingActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : BaseActivity() {
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory()
    }

    private lateinit var exitPanel: View
    private lateinit var exitConfirmBtn: Button
    private lateinit var exitCancelBtn: Button

    private lateinit var discardPanel: View
    private lateinit var discardConfirmBtn: Button
    private lateinit var discardCancelBtn: Button
    private var selectedSessionId: String? = null

    private val settingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            recreate()
        }
    }

    private lateinit var cardContainer: com.google.android.flexbox.FlexboxLayout
    private lateinit var emptyContainer: LinearLayout

    private val colorPalette = listOf(
        R.color.yellow_light,
        R.color.blue_light,
        R.color.purple_light,
        R.color.teal_light,
        R.color.red_light
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        applyWindowInsets()
        initializeViews()
        initExitPanel()
        initDiscardPanel()
        setupTopNavigationBar()
        setupStartButton()
        setupBackPressHandler()
        observeUserInfo()
    }

    override fun onResume() {
        super.onResume()
        loadUserInfo()
    }

    private fun initializeViews() {
        cardContainer = findViewById(R.id.cardContainer)
        emptyContainer = findViewById(R.id.emptyContainer)
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun initExitPanel() {
        exitPanel = findViewById(R.id.exitPanelInclude)
        exitConfirmBtn = findViewById(R.id.exitConfirmBtn)
        exitCancelBtn = findViewById(R.id.exitCancelBtn)

        exitConfirmBtn.setOnClickListener {
            finish()
        }

        exitCancelBtn.setOnClickListener {
            exitPanel.visibility = View.GONE
        }
    }

    private fun initDiscardPanel() {
        discardPanel = findViewById(R.id.discardPanelInclude)
        discardConfirmBtn = findViewById(R.id.discardConfirmBtn)
        discardCancelBtn = findViewById(R.id.discardCancelBtn)

        discardConfirmBtn.setOnClickListener {
            selectedSessionId?.let { sessionId ->
                lifecycleScope.launch {
                    val deviceInfo = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
                    viewModel.discardSession(sessionId, deviceInfo)
                }
            }
            discardPanel.visibility = View.GONE
            selectedSessionId = null
        }

        discardCancelBtn.setOnClickListener {
            discardPanel.visibility = View.GONE
            selectedSessionId = null
        }
    }

    private fun loadUserInfo() {
        val deviceInfo = getAndroidId()
        viewModel.loadUserInfo(deviceInfo)
    }

    private fun observeUserInfo() {
        lifecycleScope.launch {
            viewModel.userInfo.collectLatest { response ->
                handleUserInfoResponse(response)
            }
        }
    }

    private fun handleUserInfoResponse(response: retrofit2.Response<List<UserInfoResponse>>?) {
        if (response == null || !response.isSuccessful) {
            showEmptyState()
            return
        }

        val sessions = response.body()
        if (sessions.isNullOrEmpty()) {
            showEmptyState()
            return
        }

        displaySessions(sessions)
    }

    private fun showEmptyState() {
        cardContainer.visibility = LinearLayout.GONE
        emptyContainer.visibility = LinearLayout.VISIBLE
    }

    private fun displaySessions(sessions: List<UserInfoResponse>) {
        cardContainer.removeAllViews()

        val reversedSessions = sessions.reversed()
        reversedSessions.forEachIndexed { index, sessionData ->

            val title = sessionData.translated_title?.trim()?.lowercase()
            val image = sessionData.image_base64?.trim()

            if ((title.isNullOrEmpty() || title == "null") && image.isNullOrEmpty()) {
                return@forEachIndexed
            }

            val sessionCard = createSessionCard(sessionData, index, reversedSessions.size)
            cardContainer.addView(sessionCard)
        }

        cardContainer.visibility = LinearLayout.VISIBLE
        emptyContainer.visibility = LinearLayout.GONE
    }

    private fun createSessionCard(
        sessionData: UserInfoResponse,
        index: Int,
        totalCount: Int
    ): SessionCard {
        val sessionCard = SessionCard(this)

        applyCardBackground(sessionCard, index, totalCount)
        setupCardContent(sessionCard, sessionData)
        setupCardListeners(sessionCard, sessionData)

        return sessionCard
    }

    private fun applyCardBackground(
        sessionCard: SessionCard,
        index: Int,
        totalCount: Int
    ) {
        val root = sessionCard.findViewById<ConstraintLayout>(R.id.sessionCardRoot)
        val logicalIndex = totalCount - 1 - index
        val colorRes = colorPalette[logicalIndex % colorPalette.size]
        val colorInt = ContextCompat.getColor(this, colorRes)

        val drawable = AppCompatResources.getDrawable(this, R.drawable.card_background)!!.mutate()
        drawable.setTint(colorInt)
        root.background = drawable
    }

    private fun setupCardContent(sessionCard: SessionCard, sessionData: UserInfoResponse) {
        setupCardTitle(sessionCard, sessionData.translated_title)
        setupCardDate(sessionCard, sessionData.started_at)
        setupCardImage(sessionCard, sessionData.image_base64)
    }

    private fun setupCardTitle(sessionCard: SessionCard, title: String?) {
        val displayTitle = title ?: "NULL"
        val maxLength = 12
        val croppedTitle = if (displayTitle.length > maxLength) {
            displayTitle.substring(0, maxLength) + "…"
        } else {
            displayTitle
        }
        sessionCard.setBookTitle(croppedTitle)
    }

    private fun setupCardDate(sessionCard: SessionCard, startedAt: String) {
        val formattedDate = formatDate(startedAt)
        sessionCard.setBookProgress(formattedDate)
    }

    private fun formatDate(dateString: String): String {
        val raw = dateString.take(10)
        val parts = raw.split("-")
        return if (parts.size >= 3) {
            "${parts[1]}/${parts[2]}"
        } else {
            dateString
        }
    }

    private fun setupCardImage(sessionCard: SessionCard, imageBase64: String?) {
        if (imageBase64.isNullOrEmpty()) return

        val bitmap = decodeBase64Image(imageBase64)
        bitmap?.let {
            sessionCard.findViewById<ImageView>(R.id.cardBookImage).setImageBitmap(it)
        }
    }

    private fun decodeBase64Image(base64String: String): Bitmap? {
        return try {
            val imageBytes = Base64.decode(base64String, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        } catch (e: Exception) {
            null
        }
    }

    private fun setupCardListeners(sessionCard: SessionCard, sessionData: UserInfoResponse) {
        setupImageClickListener(sessionCard, sessionData.started_at)
        setupTrashClickListener(sessionCard, sessionData.session_id)
    }

    private fun setupImageClickListener(sessionCard: SessionCard, startedAt: String) {
        sessionCard.setOnImageClickListener {
            navigateToLoadingSession(startedAt)
        }
    }

    private fun setupTrashClickListener(sessionCard: SessionCard, sessionId: String) {
        sessionCard.setOnTrashClickListener {
            selectedSessionId = sessionId
            discardPanel.visibility = View.VISIBLE
        }
    }

    private fun setupTopNavigationBar() {
        val topNav = findViewById<TopNavigationBar>(R.id.topNavigationBar)
        topNav.setOnSettingsClickListener {
            navigateToSettings()
        }
    }

    private fun setupStartButton() {
        val startButton = findViewById<Button>(R.id.startNewReadingButton)
        startButton.setOnClickListener {
            navigateToStartSession()
        }
    }

    private fun navigateToSettings() {
        val intent = Intent(this, SettingActivity::class.java)
        settingsLauncher.launch(intent)
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                exitPanel.visibility = View.VISIBLE
            }
        })
    }

    private fun navigateToStartSession() {
        startActivity(Intent(this, StartSessionActivity::class.java))
    }

    private fun navigateToLoadingSession(startedAt: String) {
        val intent = Intent(this, LoadingActivity::class.java).apply {
            putExtra("started_at", startedAt)
        }
        startActivity(intent)
    }

    @SuppressLint("HardwareIds")
    private fun getAndroidId(): String {
        return Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
    }
}