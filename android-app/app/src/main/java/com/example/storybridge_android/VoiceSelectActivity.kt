package com.example.storybridge_android

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
import com.example.storybridge_android.network.GetTTSFrontResponse
import com.example.storybridge_android.network.RetrofitClient
import com.example.storybridge_android.network.SelectVoiceRequest
import com.example.storybridge_android.network.SelectVoiceResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

class VoiceSelectActivity : AppCompatActivity() {

    private var sessionId: String? = null
    private var pageIndex: Int = 0

    private lateinit var manButton: Button
    private lateinit var womanButton: Button
    private lateinit var nextButton: Button

    private var maleTitleAudio: String? = null
    private var femaleTitleAudio: String? = null
    private var selectedVoice: String? = null

    private var mediaPlayer: MediaPlayer? = null

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
        pageIndex = intent.getIntExtra("page_index", 0)

        Log.d(TAG, "Received session_id: $sessionId")
        Log.d(TAG, "Received page_index: $pageIndex")

        if (sessionId == null) {
            Log.e(TAG, "✗ No session_id received! Cannot proceed.")
            finish()
            return
        }

        manButton = findViewById(R.id.manButton)
        womanButton = findViewById(R.id.womanButton)
        nextButton = findViewById(R.id.nextButton)

        // 오디오 로딩 전엔 비활성화
        manButton.isEnabled = false
        womanButton.isEnabled = false
        nextButton.isEnabled = false

        // 표지 제목 TTS 조회(단발 + 최대 2회 재시도)
        fetchFrontTitleTtsOnce()

        Log.d(TAG, "Setting up button listeners...")

        manButton.setOnClickListener {
            Log.d(TAG, "Man button clicked")
            val audio = maleTitleAudio
            if (audio.isNullOrEmpty()) {
                Toast.makeText(this, "남성 샘플이 아직 준비되지 않았습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            playBase64Audio(audio)
            AppSettings.setVoice(this, "male")
            selectedVoice = "male"
            nextButton.isEnabled = true
        }

        womanButton.setOnClickListener {
            Log.d(TAG, "Woman button clicked")
            val audio = femaleTitleAudio
            if (audio.isNullOrEmpty()) {
                Toast.makeText(this, "여성 샘플이 아직 준비되지 않았습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            playBase64Audio(audio)
            AppSettings.setVoice(this, "female")
            selectedVoice = "female"
            nextButton.isEnabled = true
        }

        nextButton.setOnClickListener {
            val voice = selectedVoice
            if (voice == null) {
                Toast.makeText(this, "성별을 선택하세요.", Toast.LENGTH_SHORT).show()
            } else {
                sendVoiceSelection(voice)
            }
        }

        Log.d(TAG, "Activity setup complete, waiting for user selection...")
    }

    /** /page/get_tts_front/ 단발 조회 (+최대 2회 재시도) */
    private fun fetchFrontTitleTtsOnce(retry: Int = 0) {
        val sid = sessionId ?: return
        Log.d(TAG, "GET /page/get_tts_front/ s=$sid, p=$pageIndex (retry=$retry)")
        RetrofitClient.pageApi.getTTSFront(sid, pageIndex)
            .enqueue(object : Callback<GetTTSFrontResponse> {
                override fun onResponse(
                    call: Call<GetTTSFrontResponse>,
                    response: Response<GetTTSFrontResponse>
                ) {
                    Log.d(TAG, "TTS front response code: ${response.code()}")
                    if (!response.isSuccessful) {
                        if (retry < 2) {
                            fetchFrontTitleTtsOnce(retry + 1)
                        } else {
                            Log.e(TAG, "get_tts_front failed after retries; finishing")
                            finish()
                        }
                        return
                    }
                    val items = response.body()?.audio_results.orEmpty()
                    val title = items.firstOrNull()

                    maleTitleAudio   = title?.audio_base64_list?.getOrNull(0)
                    femaleTitleAudio = title?.audio_base64_list?.getOrNull(1)

                    val maleReady = !maleTitleAudio.isNullOrEmpty()
                    val femaleReady = !femaleTitleAudio.isNullOrEmpty()

                    manButton.isEnabled = maleReady
                    womanButton.isEnabled = femaleReady

                    Log.d(TAG, "maleReady=$maleReady, femaleReady=$femaleReady")

                    if (!maleReady && !femaleReady) {
                        if (retry < 2) {
                            fetchFrontTitleTtsOnce(retry + 1)
                        } else {
                            Log.w(TAG, "No front TTS available; finishing")
                            finish()
                        }
                    }
                }

                override fun onFailure(call: Call<GetTTSFrontResponse>, t: Throwable) {
                    Log.e(TAG, "get_tts_front error: ${t.message}", t)
                    if (retry < 2) {
                        fetchFrontTitleTtsOnce(retry + 1)
                    } else {
                        finish()
                    }
                }
            })
    }

    /** base64 오디오를 임시 파일로 저장 후 MediaPlayer로 재생 */
    private fun playBase64Audio(b64: String) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer()

            val bytes = Base64.decode(b64, Base64.DEFAULT)
            val temp = File.createTempFile("front_tts_", ".mp3", cacheDir)
            temp.outputStream().use { it.write(bytes) }

            mediaPlayer!!.setDataSource(temp.absolutePath)
            mediaPlayer!!.setOnPreparedListener { it.start() }
            mediaPlayer!!.setOnCompletionListener {
                Log.d(TAG, "Audio completed")
            }
            mediaPlayer!!.setOnErrorListener { _, what, extra ->
                Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                true
            }
            mediaPlayer!!.prepareAsync()
        } catch (e: Exception) {
            Log.e(TAG, "playBase64Audio error", e)
            Toast.makeText(this, "오디오 재생 실패", Toast.LENGTH_SHORT).show()
        }
    }

    /** Next: 선호를 서버에 저장 후 다음 페이지 촬영으로 */
    private fun sendVoiceSelection(voice: String) {
        Log.d(TAG, "=== Sending Voice Selection ===")
        Log.d(TAG, "Voice: $voice")
        Log.d(TAG, "Session ID: $sessionId")

        val id = sessionId
        if (id == null) {
            Log.e(TAG, "✗ Session ID is null, cannot send voice selection")
            goToNextCapture()
            return
        }

        val request = SelectVoiceRequest(session_id = id, voice_style = voice)
        Log.d(TAG, "Making API call to /session/voice ...")

        RetrofitClient.sessionApi.selectVoice(request)
            .enqueue(object : Callback<SelectVoiceResponse> {
                override fun onResponse(
                    call: Call<SelectVoiceResponse>,
                    response: Response<SelectVoiceResponse>
                ) {
                    Log.d(TAG, "Voice selection response: ${response.code()}, success=${response.isSuccessful}")
                    goToNextCapture()
                }

                override fun onFailure(call: Call<SelectVoiceResponse>, t: Throwable) {
                    Log.e(TAG, "Voice selection API failed: ${t.message}", t)
                    goToNextCapture()
                }
            })
    }

    /** 다음 페이지 촬영으로 이동 (표지 다음이므로 page_index + 1) */
    private fun goToNextCapture() {
        Log.d(TAG, "=== Navigating to CameraSessionActivity (next page) ===")
        val intent = Intent(this, CameraSessionActivity::class.java)
        intent.putExtra("session_id", sessionId)
        intent.putExtra("page_index", pageIndex + 1)
        intent.putExtra("is_front", false)
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
        Log.d(TAG, "=== Activity destroyed ===")
    }
}
