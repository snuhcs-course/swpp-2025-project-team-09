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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_session)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        startSession()
    }

    private fun startSession() {
        val deviceInfo = android.provider.Settings.Secure.getString(
            contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        )
        val request = StartSessionRequest(user_id = deviceInfo)

        val call = RetrofitClient.sessionApi.startSession(request)
        call.enqueue(object : Callback<StartSessionResponse> {
            override fun onResponse(call: Call<StartSessionResponse>, response: Response<StartSessionResponse>) {
                if (response.isSuccessful) {
                    val session = response.body()
                    val sessionId = session?.session_id
                    if (sessionId != null) {
                        navigateToVoiceSelect(sessionId)
                    } else {
                        Log.e("StartSession", "Session ID is null")
                    }
                } else {
                    Log.e("StartSession", "Failed to start session: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<StartSessionResponse>, t: Throwable) {
                Log.e("StartSession", "Error: ${t.message}")
            }
        })
    }

    private fun navigateToVoiceSelect(sessionId: String) {
        val intent = Intent(this, VoiceSelectActivity::class.java)
        intent.putExtra("session_id", sessionId)
        startActivity(intent)
        finish()
    }
}
