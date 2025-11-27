package com.example.storybridge_android.repository

import com.example.storybridge_android.data.UserRepositoryImpl
import com.example.storybridge_android.network.UserLoginRequest
import com.example.storybridge_android.network.UserRegisterRequest
import com.example.storybridge_android.network.RetrofitClient
import com.example.storybridge_android.network.UserLangRequest
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class UserRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: UserRepositoryImpl

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()

        RetrofitClient.overrideBaseUrl(server.url("/").toString())
        repository = UserRepositoryImpl()
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

    @Test
    fun userLang_returnsParsedResponse() = runBlocking {
        val mockJson = """
        {
            "user_id": "u123",
            "language_preference": "en",
            "updated_at": "2025-11-14T10:00:00"
        }
    """.trimIndent()
        server.enqueue(MockResponse().setResponseCode(200).setBody(mockJson))

        val response = repository.userLang(UserLangRequest("device_123", "en"))

        assertTrue(response.isSuccessful)
        val body = response.body()
        assertEquals("u123", body?.user_id)
        assertEquals("en", body?.language_preference)
        assertEquals("2025-11-14T10:00:00", body?.updated_at)
    }

    @Test
    fun getUserInfo_returnsParsedList() = runBlocking {
        val mockJson = """
        [
            {
                "user_id": "u123",
                "session_id": "s1",
                "title": "book1",
                "translated_title": "Book Title",
                "image_base64": "base64image",
                "started_at": "2025-11-14T09:00:00"
            },
            {
                "user_id": "u123",
                "session_id": "s2",
                "title": "book2",
                "translated_title": "Another Book",
                "image_base64": "base64image2",
                "started_at": "2025-11-14T10:00:00"
            }
        ]
    """.trimIndent()
        server.enqueue(MockResponse().setResponseCode(200).setBody(mockJson))

        val response = repository.getUserInfo("device_123")

        assertTrue(response.isSuccessful)
        val list = response.body()
        assertEquals(2, list?.size)
        assertEquals("s1", list?.get(0)?.session_id)
        assertEquals("book2", list?.get(1)?.title)
    }
}
