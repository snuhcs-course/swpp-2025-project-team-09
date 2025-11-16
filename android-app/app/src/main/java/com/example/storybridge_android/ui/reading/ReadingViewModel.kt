package com.example.storybridge_android.ui.reading

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storybridge_android.data.PageRepository
import com.example.storybridge_android.network.GetImageResponse
import com.example.storybridge_android.network.GetOcrTranslationResponse
import com.example.storybridge_android.network.GetTtsResponse
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ReadingUiState(
    val image: GetImageResponse? = null,
    val ocr: GetOcrTranslationResponse? = null,
    val tts: GetTtsResponse? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class ReadingViewModel(private val repository: PageRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ReadingUiState())
    val uiState: StateFlow<ReadingUiState> = _uiState

    private var pollingJob: Job? = null
    private val pollInterval = 2000L

    private val _thumbnailList = MutableStateFlow<List<PageThumbnail>>(emptyList())
    val thumbnailList: StateFlow<List<PageThumbnail>> = _thumbnailList

    fun fetchPage(sessionId: String, pageIndex: Int) {
        _uiState.value = _uiState.value.copy(isLoading = true)
        fetchImage(sessionId, pageIndex)
        fetchOcr(sessionId, pageIndex)
        fetchTts(sessionId, pageIndex)
        startTtsPolling(sessionId, pageIndex)
    }

    private fun fetchImage(sessionId: String, pageIndex: Int) {
        viewModelScope.launch {
            repository.getImage(sessionId, pageIndex)
                .onSuccess { data ->
                    _uiState.value = _uiState.value.copy(image = data, isLoading = false)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(error = e.message, isLoading = false)
                }
        }
    }

    private fun fetchOcr(sessionId: String, pageIndex: Int) {
        viewModelScope.launch {
            repository.getOcrResults(sessionId, pageIndex)
                .onSuccess { data ->
                    _uiState.value = _uiState.value.copy(ocr = data)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(error = e.message)
                }
        }
    }

    private fun fetchTts(sessionId: String, pageIndex: Int) {
        viewModelScope.launch {
            repository.getTtsResults(sessionId, pageIndex)
                .onSuccess { data ->
                    _uiState.value = _uiState.value.copy(tts = data, isLoading = false)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(error = e.message, isLoading = false)
                }
        }
    }

    private fun startTtsPolling(sessionId: String, pageIndex: Int) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            repeat(60) { // 최대 60회 시도 (2분)
                repository.getTtsResults(sessionId, pageIndex)
                    .onSuccess { newData ->
                        _uiState.value = _uiState.value.copy(tts = newData)
                        Log.d("ReadingVM", "TTS polling: fetched ${newData.audio_results.size} items")

                        // 여기서는 BBox 개수와 비교하지 않고 그냥 계속 업데이트
                    }
                    .onFailure { e ->
                        Log.e("ReadingVM", "Polling error", e)
                    }

                delay(pollInterval)
            }
            Log.w("ReadingVM", "TTS polling finished (max attempts reached)")
        }
    }


    fun fetchThumbnail(sessionId: String, pageIndex: Int) {
        viewModelScope.launch {
            repository.getImage(sessionId, pageIndex)
                .onSuccess { res ->
                    val newItem = PageThumbnail(pageIndex, res.image_base64)
                    _thumbnailList.value = _thumbnailList.value + newItem
                }
                .onFailure {
                    val newItem = PageThumbnail(pageIndex, null)
                    _thumbnailList.value = _thumbnailList.value + newItem
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}
