package com.example.ocr_test

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.widget.TextView

class MainActivity : AppCompatActivity() {

    private val secretKey = "sk" // Replace this to your secret key

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        testOcrFromDrawable()
    }

    private fun testOcrFromDrawable() {
        // drawable to File
        val file = drawableToFile(this, R.drawable.test_img, "test_img.png")
        // To Base64
        val base64Image = fileToBase64(file)

        val request = OcrRequest(
            images = listOf(
                OcrImage(
                    format = "png",
                    name = "test_img",
                    data = base64Image
                )
            )
        )
        // API call
        val call = RetrofitClient.instance.requestOcr(
            secretKey = secretKey,
            request = request
        )

        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    val result = response.body()?.string()
                    println("OCR Result: $result")

                    result?.let {
                        val extractedText = parseInferText(it)

                        runOnUiThread {
                            val textView = findViewById<TextView>(R.id.ocrResultText)
                            textView.text = extractedText
                        }
                    }
                } else {
                    println("Error: ${response.code()} / ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                println("Failed: ${t.message}")
            }
        })
    }

}
