package com.example.storybridge_android.repository

import com.example.storybridge_android.data.PageRepositoryImpl
import com.example.storybridge_android.network.*
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class PageRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: PageRepositoryImpl

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
        RetrofitClient.overrideBaseUrl(server.url("/").toString())
        repository = PageRepositoryImpl()
    }

    @After
    fun teardown() {
        server.shutdown()
    }

    @Test
    fun getImage_returnsParsedResponse() = runBlocking {
        val json = """
            {
                "session_id": "s123",
                "page_index": 0,
                "image_base64": "imgbase64",
                "stored_at": "2025-11-15T10:00:00"
            }
        """
        server.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val result = repository.getImage("s123", 0)

        assertTrue(result.isSuccess)
        assertEquals("imgbase64", result.getOrNull()?.image_base64)
    }

    @Test
    fun getImage_returnsFailureOnHttpError() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(404))
        val result = repository.getImage("s123", 0)
        assertTrue(result.isFailure)
    }

    @Test
    fun getImage_returnsFailureOnNullBody() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody(""))
        val result = repository.getImage("s123", 0)
        assertTrue(result.isFailure)
    }

    @Test
    fun getOcrResults_returnsParsedResponse() = runBlocking {
        val json = """
            {
                "session_id": "s123",
                "page_index": 0,
                "ocr_results": [
                    {
                        "bbox": { "x1": 0, "y1": 0, "x2": 10, "y2": 0, "x3": 10, "y3": 10, "x4": 0, "y4": 10 },
                        "original_txt": "hello",
                        "translation_txt": "안녕"
                    }
                ],
                "processed_at": "2025-11-15T10:00:00"
            }
        """
        server.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val result = repository.getOcrResults("s123", 0)

        assertTrue(result.isSuccess)
        assertEquals("hello", result.getOrNull()?.ocr_results?.get(0)?.original_txt)
    }

    @Test
    fun getOcrResults_returnsFailureOnHttpError() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500))
        val result = repository.getOcrResults("s123", 0)
        assertTrue(result.isFailure)
    }

    @Test
    fun getOcrResults_returnsFailureOnNullBody() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody(""))
        val result = repository.getOcrResults("s123", 0)
        assertTrue(result.isFailure)
    }

    @Test
    fun getTtsResults_returnsParsedResponse() = runBlocking {
        val json = """
            {
                "session_id": "s123",
                "page_index": 0,
                "audio_results": [
                    {
                        "bbox_index": 0,
                        "audio_base64_list": ["abc123", "def456"]
                    }
                ],
                "generated_at": "2025-11-15T10:00:00"
            }
        """
        server.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val result = repository.getTtsResults("s123", 0)

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull()?.audio_results?.get(0)?.audio_base64_list?.size)
    }

    @Test
    fun getTtsResults_returnsFailureOnHttpError() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(403))
        val result = repository.getTtsResults("s123", 0)
        assertTrue(result.isFailure)
    }

    @Test
    fun getTtsResults_returnsFailureOnNullBody() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody(""))
        val result = repository.getTtsResults("s123", 0)
        assertTrue(result.isFailure)
    }
}
