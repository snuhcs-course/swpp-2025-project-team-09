package com.example.storybridge_android.ui.session

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.storybridge_android.ui.setting.AppSettings
import com.example.storybridge_android.R
import com.example.storybridge_android.network.RetrofitClient
import com.example.storybridge_android.network.SelectVoiceRequest
import com.example.storybridge_android.network.SelectVoiceResponse
import com.example.storybridge_android.ui.camera.CameraSessionActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class VoiceSelectActivity : AppCompatActivity() {

    private var sessionId: String? = null

    companion object {
        private const val TAG = "VoiceSelectActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "=== VoiceSelectActivity onCreate ===")

        enableEdgeToEdge()
        setContentView(R.layout.activity_voice_select)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        sessionId = intent.getStringExtra("session_id")
        Log.d(TAG, "Received session_id: $sessionId")

        if (sessionId == null) {
            Log.e(TAG, "✗ No session_id received! Cannot proceed.")
            finish()
            return
        }

        val manButton = findViewById<Button>(R.id.manButton)
        val womanButton = findViewById<Button>(R.id.womanButton)

        Log.d(TAG, "Setting up button listeners...")

        manButton.setOnClickListener {
            Log.d(TAG, "Man button clicked")
            AppSettings.setVoice(this, "male")
            sendVoiceSelection("male")
        }

        womanButton.setOnClickListener {
            Log.d(TAG, "Woman button clicked")
            AppSettings.setVoice(this, "female")
            sendVoiceSelection("female")
        }

        Log.d(TAG, "Activity setup complete, waiting for user selection...")
    }

    private fun sendVoiceSelection(voice: String) {
        Log.d(TAG, "=== Sending Voice Selection ===")
        Log.d(TAG, "Voice: $voice")
        Log.d(TAG, "Session ID: $sessionId")

        val id = sessionId
        if (id == null) {
            Log.e(TAG, "✗ Session ID is null, cannot send voice selection")
            goToCamera()
            return
        }

        val request = SelectVoiceRequest(session_id = id, voice_style = voice)
        Log.d(TAG, "Making API call to /session/voice...")

        RetrofitClient.sessionApi.selectVoice(request)
            .enqueue(object : Callback<SelectVoiceResponse> {
                override fun onResponse(
                    call: Call<SelectVoiceResponse>,
                    response: Response<SelectVoiceResponse>
                ) {
                    Log.d(TAG, "=== Voice Selection API Response ===")
                    Log.d(TAG, "Response code: ${response.code()}")
                    Log.d(TAG, "Is successful: ${response.isSuccessful}")

                    if (response.isSuccessful) {
                        Log.d(TAG, "✓ Voice selection successful")
                        val body = response.body()
                        Log.d(TAG, "Response body: $body")
                    } else {
                        Log.w(TAG, "Voice selection API failed, but continuing anyway")
                        Log.w(TAG, "Response code: ${response.code()}")
                    }

                    goToCamera()
                }

                override fun onFailure(call: Call<SelectVoiceResponse>, t: Throwable) {
                    Log.e(TAG, "=== Voice Selection API Failed ===")
                    Log.e(TAG, "Error: ${t.message}", t)
                    Log.d(TAG, "Continuing to camera despite error...")
                    goToCamera()
                }
            })
    }

    private fun goToCamera() {
        Log.d(TAG, "=== Navigating to CameraSessionActivity ===")
        Log.d(TAG, "Session ID to pass: $sessionId")

        val intent = Intent(this, CameraSessionActivity::class.java)
        intent.putExtra("session_id", sessionId)

        Log.d(TAG, "Starting CameraSessionActivity...")
        startActivity(intent)

        Log.d(TAG, "Finishing VoiceSelectActivity")
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "=== Activity destroyed ===")
    }
}