package com.example.storybridge_android

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.storybridge_android.network.RetrofitClient
import com.example.storybridge_android.network.StartSessionRequest
import com.example.storybridge_android.network.StartSessionResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class StartSessionActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "StartSessionActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "=== StartSessionActivity onCreate ===")

        setContentView(R.layout.activity_start_session)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        startSession()
    }

    private fun startSession() {
        Log.d(TAG, "=== Starting Session ===")

        val deviceInfo = android.provider.Settings.Secure.getString(
            contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        )

        Log.d(TAG, "Device ID: $deviceInfo")

        val request = StartSessionRequest(user_id = deviceInfo)
        Log.d(TAG, "Request created: user_id=$deviceInfo")

        val call = RetrofitClient.sessionApi.startSession(request)
        Log.d(TAG, "Making API call to /session/start...")

        call.enqueue(object : Callback<StartSessionResponse> {
            override fun onResponse(call: Call<StartSessionResponse>, response: Response<StartSessionResponse>) {
                Log.d(TAG, "=== API Response Received ===")
                Log.d(TAG, "Response code: ${response.code()}")
                Log.d(TAG, "Response message: ${response.message()}")
                Log.d(TAG, "Is successful: ${response.isSuccessful}")

                if (response.isSuccessful) {
                    val session = response.body()
                    Log.d(TAG, "Response body: $session")

                    val sessionId = session?.session_id
                    Log.d(TAG, "Session ID: $sessionId")

                    if (sessionId != null) {
                        Log.d(TAG, "✓ Session created successfully: $sessionId")
                        Log.d(TAG, "Navigating to VoiceSelectActivity...")
                        navigateToVoiceSelect(sessionId)
                    } else {
                        Log.e(TAG, "✗ Session ID is null in response body")
                        Log.e(TAG, "Full response body: $session")
                        finish()
                    }
                } else {
                    Log.e(TAG, "✗ Failed to start session")
                    Log.e(TAG, "Response code: ${response.code()}")
                    Log.e(TAG, "Response message: ${response.message()}")

                    try {
                        val errorBody = response.errorBody()?.string()
                        Log.e(TAG, "Error body: $errorBody")
                    } catch (e: Exception) {
                        Log.e(TAG, "Could not read error body", e)
                    }
                    finish()
                }
            }

            override fun onFailure(call: Call<StartSessionResponse>, t: Throwable) {
                Log.e(TAG, "=== API Call Failed ===")
                Log.e(TAG, "Error type: ${t.javaClass.simpleName}")
                Log.e(TAG, "Error message: ${t.message}")
                Log.e(TAG, "Stack trace:", t)
                t.printStackTrace()
                finish()
            }
        })

        Log.d(TAG, "API call enqueued, waiting for response...")
    }

    private fun navigateToVoiceSelect(sessionId: String) {
        Log.d(TAG, "=== Navigating to VoiceSelectActivity ===")
        Log.d(TAG, "Session ID to pass: $sessionId")

        val intent = Intent(this, VoiceSelectActivity::class.java)
        intent.putExtra("session_id", sessionId)

        Log.d(TAG, "Starting VoiceSelectActivity...")
        startActivity(intent)

        Log.d(TAG, "Finishing StartSessionActivity")
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "=== Activity destroyed ===")
    }
}