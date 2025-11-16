package com.example.storybridge_android.repository

import com.example.storybridge_android.data.SessionRepositoryImpl
import com.example.storybridge_android.network.*
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class SessionRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: SessionRepositoryImpl

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()

        RetrofitClient.overrideBaseUrl(server.url("/").toString())
        repository = SessionRepositoryImpl()
    }

    @After
    fun teardown() {
        server.shutdown()
    }

    @Test
    fun startSession_returnsParsedResponse() = runBlocking {
        val json = """{"session_id":"s123","started_at":"2025-11-15T10:00:00","page_index":0}"""
        server.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val result = repository.startSession("user_123")

        assertTrue(result.isSuccess)
        val body = result.getOrNull()
        assertEquals("s123", body?.session_id)
    }

    @Test
    fun startSession_returnsFailureOnHttpError() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500))
        val result = repository.startSession("u1")
        assertTrue(result.isFailure)
    }

    @Test
    fun startSession_returnsFailureOnNullBody() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody(""))
        val result = repository.startSession("u1")
        assertTrue(result.isFailure)
    }


    @Test
    fun selectVoice_returnsParsedResponse() = runBlocking {
        val json = """{"session_id":"s123","voice_style":"nova"}"""
        server.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val result = repository.selectVoice("s123", "nova")

        assertTrue(result.isSuccess)
        assertEquals("nova", result.getOrNull()?.voice_style)
    }

    @Test
    fun selectVoice_returnsFailureOnHttpError() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(404))
        val result = repository.selectVoice("s1", "nova")
        assertTrue(result.isFailure)
    }

    @Test
    fun selectVoice_returnsFailureOnNullBody() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody(""))
        val result = repository.selectVoice("s1", "nova")
        assertTrue(result.isFailure)
    }


    @Test
    fun endSession_returnsParsedResponse() = runBlocking {
        val json = """{"session_id":"s123","ended_at":"2025-11-15T10:30:00","total_pages":3}"""
        server.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val result = repository.endSession("s123")

        assertTrue(result.isSuccess)
        assertEquals(3, result.getOrNull()?.total_pages)
    }

    @Test
    fun endSession_returnsFailureOnHttpError() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(400))
        val result = repository.endSession("s1")
        assertTrue(result.isFailure)
    }

    @Test
    fun endSession_returnsFailureOnNullBody() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody(""))
        val result = repository.endSession("s1")
        assertTrue(result.isFailure)
    }


    @Test
    fun getSessionStats_returnsParsedResponse() = runBlocking {
        val json = """
            {
                "session_id":"s123","user_id":"u001","isOngoing":false,
                "started_at":"2025-11-15T10:00:00","ended_at":"2025-11-15T10:30:00",
                "total_pages":3,"total_time_spent":600,"total_words_read":200
            }
        """
        server.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val result = repository.getSessionStats("s123")

        assertTrue(result.isSuccess)
        assertEquals("u001", result.getOrNull()?.user_id)
    }

    @Test
    fun getSessionStats_returnsFailureOnHttpError() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(403))
        val result = repository.getSessionStats("s1")
        assertTrue(result.isFailure)
    }

    @Test
    fun getSessionStats_returnsFailureOnNullBody() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody(""))
        val result = repository.getSessionStats("s1")
        assertTrue(result.isFailure)
    }

    @Test
    fun reloadAllSession_returnsParsedResponse() = runBlocking {
        val json = """
            {
                "session_id":"s123",
                "started_at":"2025-11-15T10:00:00",
                "pages":[
                    {
                        "page_index":0,"img_url":"img1.jpg",
                        "translation_text":"Hi","audio_url":"a1.mp3","ocr_results":[]
                    },
                    {
                        "page_index":1,"img_url":"img2.jpg",
                        "translation_text":"Hello","audio_url":"a2.mp3","ocr_results":[]
                    }
                ]
            }
        """
        server.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val result = repository.reloadAllSession("u001", "2025-11-15T10:00:00")

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull()?.pages?.size)
    }

    @Test
    fun reloadAllSession_returnsFailureOnHttpError() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(502))
        val result = repository.reloadAllSession("u1", "2025-01-01")
        assertTrue(result.isFailure)
    }

    @Test
    fun reloadAllSession_returnsFailureOnNullBody() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody(""))
        val result = repository.reloadAllSession("u1", "2025-01-01")
        assertTrue(result.isFailure)
    }


    @Test
    fun discardSession_returnsParsedResponse() = runBlocking {
        val json = """{"message":"Session discarded successfully"}"""
        server.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val result = repository.discardSession("s123")

        assertTrue(result.isSuccess)
        assertEquals("Session discarded successfully", result.getOrNull()?.message)
    }

    @Test
    fun discardSession_returnsFailureOnHttpError() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(400))
        val result = repository.discardSession("s1")
        assertTrue(result.isFailure)
    }

    @Test
    fun discardSession_returnsFailureOnNullBody() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody(""))
        val result = repository.discardSession("s1")
        assertTrue(result.isFailure)
    }

}
