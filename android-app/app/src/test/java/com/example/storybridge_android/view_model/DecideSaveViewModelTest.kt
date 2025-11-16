package com.example.storybridge_android.ui.session

import com.example.storybridge_android.data.SessionRepository
import com.example.storybridge_android.network.DiscardSessionResponse
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
class DecideSaveViewModelTest {

    private lateinit var repo: SessionRepository
    private lateinit var viewModel: DecideSaveViewModel
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        repo = mockk()
        viewModel = DecideSaveViewModel(repo)
    }

    @Test
    fun initial_state_is_idle() = runTest {
        assertEquals(DecideSaveUiState.Idle, viewModel.state.value)
    }

    @Test
    fun save_sets_state_to_success() = runTest {
        viewModel.save()
        assertEquals(DecideSaveUiState.SaveSuccess, viewModel.state.value)
    }

    @Test
    fun discard_success_updates_state() = runTest {
        val res = DiscardSessionResponse("s1")
        coEvery { repo.discardSession("s1") } returns Result.success(res)

        viewModel.discard("s1")
        advanceUntilIdle()

        assertEquals(DecideSaveUiState.DiscardSuccess, viewModel.state.value)
    }

    @Test
    fun discard_failure_updates_state_with_error() = runTest {
        coEvery { repo.discardSession("s1") } returns Result.failure(Exception("fail"))

        viewModel.discard("s1")
        advanceUntilIdle()

        assertEquals(
            DecideSaveUiState.DiscardError("fail"),
            viewModel.state.value
        )
    }

}
