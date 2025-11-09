package com.example.storybridge_android.repository

import com.example.storybridge_android.data.DefaultUserRepository
import com.example.storybridge_android.network.UserLoginRequest
import com.example.storybridge_android.network.UserRegisterRequest
import com.example.storybridge_android.network.RetrofitClient
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class UserRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: DefaultUserRepository

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()

        RetrofitClient.overrideBaseUrl(server.url("/").toString())
        repository = DefaultUserRepository()
    }

    @After
    fun teardown() {
        server.shutdown()
    }

    @Test
    fun login_returnsParsedResponse() = runBlocking {
        val mockJson = """{"user_id":"u001","language_preference":"en"}"""
        server.enqueue(MockResponse().setResponseCode(200).setBody(mockJson))

        val response = repository.login(UserLoginRequest("device_123"))

        assertTrue(response.isSuccessful)
        assertEquals("u001", response.body()?.user_id)
        assertEquals("en", response.body()?.language_preference)
    }

    @Test
    fun register_returnsParsedResponse() = runBlocking {
        val mockJson = """{"user_id":"u999","language_preference":"en"}"""
        server.enqueue(MockResponse().setResponseCode(200).setBody(mockJson))

        val response = repository.register(UserRegisterRequest("device_999", "zh"))

        assertTrue(response.isSuccessful)
        assertEquals("u999", response.body()?.user_id)
    }
}
