package com.example.storybridge_android.ui.session

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storybridge_android.data.SessionRepository
import com.example.storybridge_android.network.SessionStatsResponse
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class FinishViewModel(private val repository: SessionRepository) : ViewModel() {

    val sessionStats = MutableLiveData<SessionStatsResponse>()
    val showMainButton = MutableLiveData<Boolean>()

    fun endSession(sessionId: String) {
        if (sessionId.isEmpty()) return

        viewModelScope.launch {
            repository.endSession(sessionId).fold(
                onSuccess = {
                    delay(500)
                    repository.getSessionStats(sessionId).fold(
                        onSuccess = { stats ->
                            sessionStats.postValue(stats)
                        },
                        onFailure = {
                            // 로그만 찍거나 무시 가능
                        }
                    )
                },
                onFailure = {
                    // 로그만 찍거나 무시 가능
                }
            )

            delay(3000)
            showMainButton.postValue(true)
        }
    }
}
