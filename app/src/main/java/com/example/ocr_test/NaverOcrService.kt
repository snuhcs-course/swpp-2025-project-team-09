package com.example.ocr_test

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.Call
import okhttp3.ResponseBody

interface NaverOcrService {
    @POST("custom/v1/46306/243a02e5c2b49022608921358ba314dcfaef7566fa05290fd581a0e24c0662a9/general")
    fun requestOcr(
        @Header("X-OCR-SECRET") secretKey: String,
        @Body request: OcrRequest
    ): Call<ResponseBody>
}


