package com.example.storybridge_android.network

data class ProcessResponse(
    val book_title: String?,
    val image_url: String?,
    val audio_url: String?,
    val translation_txt: String?,
    //val bbox_results: JsonElement?,
    val created_at: String?
)
