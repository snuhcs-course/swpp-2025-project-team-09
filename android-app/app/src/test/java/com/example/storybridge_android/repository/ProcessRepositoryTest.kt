package com.example.storybridge_android.repository

import com.example.storybridge_android.data.ProcessRepositoryImpl
import com.example.storybridge_android.network.*
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class ProcessRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: ProcessRepositoryImpl

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()

        RetrofitClient.overrideBaseUrl(server.url("/").toString())
        repository = ProcessRepositoryImpl()
    }

    @After
    fun teardown() {
        server.shutdown()
    }

    private fun dummyUploadReq() = UploadImageRequest("s123", 0, "en", "base64string")

    @Test
    fun uploadImage_returnsParsedResponse() = runBlocking {
        val json = """{"session_id":"s123","page_index":0,"status":"pending","submitted_at":"2025-11-15T10:00:00"}"""
        server.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val result = repository.uploadImage(dummyUploadReq())

        assertTrue(result.isSuccess)
        assertEquals("s123", result.getOrNull()?.session_id)
    }

    @Test
    fun uploadImage_returnsFailureOnHttpError() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(400))
        val result = repository.uploadImage(dummyUploadReq())
        assertTrue(result.isFailure)
    }

    @Test
    fun uploadImage_returnsFailureOnNullBody() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody(""))
        val result = repository.uploadImage(dummyUploadReq())
        assertTrue(result.isFailure)
    }

    @Test
    fun uploadCoverImage_returnsParsedResponse() = runBlocking {
        val json = """
            {
                "session_id":"s123","page_index":0,"status":"ready","submitted_at":"2025-11-15T10:00:00",
                "title":"title","translated_title":"translated","tts_male":"m.mp3","tts_female":"f.mp3"
            }
        """
        server.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val result = repository.uploadCoverImage(dummyUploadReq())

        assertTrue(result.isSuccess)
        assertEquals("title", result.getOrNull()?.title)
    }

    @Test
    fun uploadCoverImage_returnsFailureOnHttpError() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500))
        val result = repository.uploadCoverImage(dummyUploadReq())
        assertTrue(result.isFailure)
    }

    @Test
    fun uploadCoverImage_returnsFailureOnNullBody() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody(""))
        val result = repository.uploadCoverImage(dummyUploadReq())
        assertTrue(result.isFailure)
    }

    @Test
    fun checkOcrStatus_returnsParsedResponse() = runBlocking {
        val json = """
            {
                "session_id":"s123","page_index":0,"status":"ready",
                "progress":100,"submitted_at":"2025-11-15T10:00:00",
                "processed_at":"2025-11-15T10:01:00"
            }
        """
        server.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val result = repository.checkOcrStatus("s123", 0)

        assertTrue(result.isSuccess)
        assertEquals("ready", result.getOrNull()?.status)
    }

    @Test
    fun checkOcrStatus_returnsFailureOnHttpError() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(403))
        val result = repository.checkOcrStatus("s123", 0)
        assertTrue(result.isFailure)
    }

    @Test
    fun checkOcrStatus_returnsFailureOnNullBody() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody(""))
        val result = repository.checkOcrStatus("s123", 0)
        assertTrue(result.isFailure)
    }

    @Test
    fun checkTtsStatus_returnsParsedResponse() = runBlocking {
        val json = """
            {
                "session_id":"s123","page_index":0,"status":"ready",
                "progress":100,"bb_status":[
                    {"bbox_index":0,"status":"ready","has_audio":true},
                    {"bbox_index":1,"status":"ready","has_audio":true}
                ]
            }
        """
        server.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val result = repository.checkTtsStatus("s123", 0)

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull()?.bb_status?.size)
    }

    @Test
    fun checkTtsStatus_returnsFailureOnHttpError() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(401))
        val result = repository.checkTtsStatus("s123", 0)
        assertTrue(result.isFailure)
    }

    @Test
    fun checkTtsStatus_returnsFailureOnNullBody() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody(""))
        val result = repository.checkTtsStatus("s123", 0)
        assertTrue(result.isFailure)
    }
}
