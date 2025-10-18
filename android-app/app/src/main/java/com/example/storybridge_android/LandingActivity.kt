package com.example.storybridge_android

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.storybridge_android.network.RetrofitClient
import com.example.storybridge_android.network.UserLoginResponse
import com.example.storybridge_android.network.UserRegisterResponse
import com.example.storybridge_android.network.UserLoginRequest
import com.example.storybridge_android.network.UserRegisterRequest
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LandingActivity : AppCompatActivity() {
    private lateinit var btnEnglish: Button
    private lateinit var btnVietnamese: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_landing_first)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.landing)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 첫 실행 시 등록 상태 확인 → 언어 선택 or 메인 이동
        Handler(Looper.getMainLooper()).postDelayed({
            checkIsRegistered()
        }, 1000)
    }

    private fun checkIsRegistered() {
        val deviceId = android.provider.Settings.Secure.getString(
            contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        )

        val api = RetrofitClient.userApi
        val loginReq = UserLoginRequest(device_info = deviceId)

        api.userLogin(loginReq).enqueue(object : Callback<UserLoginResponse> {
            override fun onResponse(
                call: Call<UserLoginResponse>,
                response: Response<UserLoginResponse>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    AppSettings.setFirstRunDone(this@LandingActivity)
                    navigateToMain()
                } else {
                    registerNewUser(deviceId)
                }
            }

            override fun onFailure(call: Call<UserLoginResponse>, t: Throwable) {
                registerNewUser(deviceId)
            }
        })
    }

    private fun registerNewUser(deviceId: String) {
        val api = RetrofitClient.userApi
        val registerReq = UserRegisterRequest(device_info = deviceId, language_preference = "en")

        api.userRegister(registerReq).enqueue(object : Callback<UserRegisterResponse> {
            override fun onResponse(
                call: Call<UserRegisterResponse>,
                response: Response<UserRegisterResponse>
            ) {
                AppSettings.setFirstRunDone(this@LandingActivity)
                showLanguageSelection()
            }

            override fun onFailure(call: Call<UserRegisterResponse>, t: Throwable) {
                t.printStackTrace()
            }
        })
    }

    private fun setLangPreference(lang: String) {
        AppSettings.setLanguage(this, lang)
    }

    private fun showLanguageSelection() {
        setContentView(R.layout.activity_landing_second)
        btnEnglish = findViewById(R.id.btnEnglish)
        btnVietnamese = findViewById(R.id.btnVietnamese)

        btnEnglish.setOnClickListener {
            setLangPreference("en")
            navigateToMain()
        }
        btnVietnamese.setOnClickListener {
            setLangPreference("vi")
            navigateToMain()
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
