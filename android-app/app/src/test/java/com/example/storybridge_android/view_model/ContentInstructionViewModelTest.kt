package com.example.storybridge_android.view_model

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.example.storybridge_android.data.SessionRepository
import com.example.storybridge_android.ui.session.instruction.ContentInstructionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.never
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ContentInstructionViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    private val fakeSessionId = "test_session_123"
    private lateinit var viewModel: ContentInstructionViewModel

    @Mock
    private lateinit var mockSessionRepo: SessionRepository

    private lateinit var closeable: AutoCloseable

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        closeable = MockitoAnnotations.openMocks(this)

        viewModel = ContentInstructionViewModel(mockSessionRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        closeable.close()
    }

    @Test
    fun onStartClicked_withValidSessionId_emitsTrue() = runTest {
        val observer = mock<Observer<Boolean>>()
        viewModel.navigateToCamera.observeForever(observer)

        viewModel.setSessionId(fakeSessionId)

        viewModel.onStartClicked()

        verify(observer).onChanged(true)

        viewModel.navigateToCamera.removeObserver(observer)
    }

    @Test
    fun onStartClicked_withValidSessionId_updatesLiveDataValue() = runTest {
        viewModel.setSessionId(fakeSessionId)

        viewModel.onStartClicked()

        assertTrue(viewModel.navigateToCamera.value ?: false)
    }

    @Test
    fun onStartClicked_withNullSessionId_emitsFalseAndDoesNotNavigate() = runTest {
        val observer = mock<Observer<Boolean>>()
        viewModel.navigateToCamera.observeForever(observer)

        viewModel.onStartClicked()

        verify(observer).onChanged(false)
        verify(observer, never()).onChanged(true)

        assertFalse(viewModel.navigateToCamera.value ?: true)

        viewModel.navigateToCamera.removeObserver(observer)
    }

    @Test
    fun setSessionId_updatesInternalState() = runTest {
        viewModel.setSessionId(fakeSessionId)

        viewModel.onStartClicked()

        assertTrue(viewModel.navigateToCamera.value ?: false)
    }
}
