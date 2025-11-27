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
    val showMainButton = MutableLiveData<Boolean>(false)

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
                            // log
                        }
                    )
                },
                onFailure = {
                    // log
                }
            )

            delay(3000)
            showMainButton.postValue(true)
        }
    }
}