package com.example.storybridge_android.ui.session

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Button
import android.widget.Toast
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
import java.io.File
import java.io.FileOutputStream

const val MALE_VOICE = "onyx"
const val FEMALE_VOICE = "shimmer"

class VoiceSelectActivity : AppCompatActivity() {

    private var sessionId: String? = null
    private var maleTts: String? = null
    private var femaleTts: String? = null
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
        maleTts = intent.getStringExtra("male_tts")
        femaleTts = intent.getStringExtra("female_tts")

        Log.d(TAG, "Received male_tts length: ${maleTts?.length}")
        Log.d(TAG, "Received female_tts length: ${femaleTts?.length}")

        if (sessionId == null) {
            Log.e(TAG, "✗ No session_id received! Cannot proceed.")
            Toast.makeText(this, "Session error. Please try again.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Check if TTS data is available
        if (maleTts.isNullOrEmpty() && femaleTts.isNullOrEmpty()) {
            Log.w(TAG, "⚠ Both TTS previews are empty")
            Toast.makeText(
                this,
                "Voice previews not available, but you can still select a voice",
                Toast.LENGTH_LONG
            ).show()
        }

        val manButton = findViewById<Button>(R.id.manButton)
        val womanButton = findViewById<Button>(R.id.womanButton)
        val nextButton = findViewById<Button>(R.id.nextButton)

        // Initial state : disabled
        nextButton.isEnabled = false

        Log.d(TAG, "Setting up button listeners...")

        manButton.setOnClickListener {
            Log.d(TAG, "Man button clicked")
            AppSettings.setVoice(this, MALE_VOICE)
            sendVoiceSelection(MALE_VOICE)
            playTts(maleTts, MALE_VOICE)
            nextButton.isEnabled = true
        }

        womanButton.setOnClickListener {
            Log.d(TAG, "Woman button clicked")
            AppSettings.setVoice(this, FEMALE_VOICE)
            sendVoiceSelection(FEMALE_VOICE)
            playTts(femaleTts, FEMALE_VOICE)
            nextButton.isEnabled = true
        }

        nextButton.setOnClickListener {
            Log.d(TAG, "Next button clicked")
            goToCamera()
        }

        Log.d(TAG, "Activity setup complete, waiting for user selection...")
    }

    private fun playTts(ttsBase64: String?, voice: String) {
        if (ttsBase64.isNullOrEmpty()) {
            Log.e(TAG, "✗ No TTS data available for $voice")
            Toast.makeText(
                this,
                "Voice preview not available for $voice",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        try {
            // Base64 → ByteArray 변환
            val audioBytes = Base64.decode(ttsBase64, Base64.DEFAULT)

            if (audioBytes.isEmpty()) {
                Log.e(TAG, "✗ Decoded audio bytes are empty")
                Toast.makeText(this, "Invalid audio data", Toast.LENGTH_SHORT).show()
                return
            }

            val tempFile = File.createTempFile("tts_preview", ".mp3", cacheDir)
            FileOutputStream(tempFile).use { it.write(audioBytes) }

            val mediaPlayer = MediaPlayer()
            mediaPlayer.setDataSource(tempFile.absolutePath)
            mediaPlayer.prepare()
            mediaPlayer.start()

            Log.d(TAG, "▶ Playing $voice voice preview...")

            mediaPlayer.setOnCompletionListener {
                Log.d(TAG, "✓ $voice voice playback finished")
                mediaPlayer.release()
                tempFile.delete() // Clean up temp file
            }

            mediaPlayer.setOnErrorListener { mp, what, extra ->
                Log.e(TAG, "✗ MediaPlayer error: what=$what, extra=$extra")
                Toast.makeText(this, "Error playing audio", Toast.LENGTH_SHORT).show()
                mp.release()
                tempFile.delete()
                true
            }

        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "✗ Invalid Base64 encoding: ${e.message}", e)
            Toast.makeText(this, "Invalid audio format", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "✗ Error playing TTS audio: ${e.message}", e)
            Toast.makeText(this, "Failed to play audio preview", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendVoiceSelection(voice: String) {
        Log.d(TAG, "=== Sending Voice Selection ===")
        Log.d(TAG, "Voice: $voice")
        Log.d(TAG, "Session ID: $sessionId")

        val id = sessionId
        if (id == null) {
            Log.e(TAG, "✗ Session ID is null, cannot send voice selection")
            Toast.makeText(this, "Session error", Toast.LENGTH_SHORT).show()
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
                        Toast.makeText(
                            this@VoiceSelectActivity,
                            "Failed to save voice preference",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<SelectVoiceResponse>, t: Throwable) {
                    Log.e(TAG, "=== Voice Selection API Failed ===")
                    Log.e(TAG, "Error: ${t.message}", t)
                    Log.d(TAG, "Continuing to camera despite error...")
                    Toast.makeText(
                        this@VoiceSelectActivity,
                        "Network error, but continuing...",
                        Toast.LENGTH_SHORT
                    ).show()
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