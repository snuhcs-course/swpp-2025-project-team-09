package com.example.storybridge_android

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.storybridge_android.network.RetrofitClient
import com.example.storybridge_android.network.SelectVoiceRequest
import com.example.storybridge_android.network.SelectVoiceResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class VoiceSelectActivity : AppCompatActivity() {

    private var sessionId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_voice_select)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        sessionId = intent.getStringExtra("session_id")

        val manButton = findViewById<Button>(R.id.manButton)
        val womanButton = findViewById<Button>(R.id.womanButton)

        manButton.setOnClickListener {
            AppSettings.setVoice(this, "male")
            sendVoiceSelection("male")
        }

        womanButton.setOnClickListener {
            AppSettings.setVoice(this, "female")
            sendVoiceSelection("female")
        }
    }

    private fun sendVoiceSelection(voice: String) {
        val id = sessionId ?: return
        val request = SelectVoiceRequest(session_id = id, voice_style = voice)

        RetrofitClient.sessionApi.selectVoice(request)
            .enqueue(object : Callback<SelectVoiceResponse> {
                override fun onResponse(
                    call: Call<SelectVoiceResponse>,
                    response: Response<SelectVoiceResponse>
                ) {
                    goToCamera()
                }

                override fun onFailure(call: Call<SelectVoiceResponse>, t: Throwable) {
                    goToCamera()
                }
            })
    }

    private fun goToCamera() {
        val intent = Intent(this, CameraSessionActivity::class.java)
        intent.putExtra("session_id", sessionId)
        startActivity(intent)
        finish()
    }
}
