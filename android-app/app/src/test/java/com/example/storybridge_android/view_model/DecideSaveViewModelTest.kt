package com.example.storybridge_android.ui.session

import com.example.storybridge_android.data.SessionRepository
import com.example.storybridge_android.network.DiscardSessionResponse
import com.example.storybridge_android.ui.session.decide.DecideSaveActivityViewModel
import com.example.storybridge_android.ui.session.decide.DecideSaveUiState
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DecideSaveActivityViewModelTest {

    private lateinit var repo: SessionRepository
    private lateinit var viewModel: DecideSaveActivityViewModel
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        repo = mockk()
        viewModel = DecideSaveActivityViewModel(repo)
    }

    @Test
    fun initial_state_is_idle() = runTest {
        assertEquals(DecideSaveUiState.Idle, viewModel.state.value)
    }

    @Test
    fun save_sets_state_to_saved() = runTest {
        viewModel.saveSession()
        assertEquals(DecideSaveUiState.Saved, viewModel.state.value)
    }

    @Test
    fun discard_success_updates_state() = runTest {
        val res = DiscardSessionResponse("s1")
        coEvery { repo.discardSession("s1") } returns Result.success(res)

        viewModel.discardSession("s1")
        advanceUntilIdle()

        assertEquals(DecideSaveUiState.Discarded, viewModel.state.value)
    }

    @Test
    fun discard_failure_updates_state_with_error() = runTest {
        coEvery { repo.discardSession("s1") } returns Result.failure(Exception("fail"))

        viewModel.discardSession("s1")
        advanceUntilIdle()

        assertEquals(
            DecideSaveUiState.Error("fail"),
            viewModel.state.value
        )
    }
}
