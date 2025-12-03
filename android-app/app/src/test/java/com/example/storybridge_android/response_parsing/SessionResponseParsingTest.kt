package com.example.storybridge_android.response_parsing

import com.example.storybridge_android.network.*
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import org.junit.Test
import org.junit.Assert.*

class SessionResponseParsingTest {
    private val gson = Gson()

    // 1. startSession
    @Test
    fun startSessionResponse_successfulParsing() {
        val json = """
        {
            "session_id": "sess_001",
            "started_at": "2025-10-28T10:00:00Z",
            "page_index": 0
        }
    """.trimIndent()

        val response = gson.fromJson(json, StartSessionResponse::class.java)

        assertEquals("sess_001", response.session_id)
        assertEquals("2025-10-28T10:00:00Z", response.started_at)
        assertEquals(0, response.page_index)
    }

    // 2. selectVoice
    @Test
    fun selectVoiceResponse_successfulParsing() {
        val json = """
        {
            "session_id": "sess_002",
            "voice_style": "Kore_Cheerful"
        }
    """.trimIndent()

        val response = gson.fromJson(json, SelectVoiceResponse::class.java)

        assertEquals("sess_002", response.session_id)
        assertEquals("Kore_Cheerful", response.voice_style)
    }

    // 3. endSession
    @Test
    fun endSessionResponse_successfulParsing() {
        val json = """
        {
            "session_id": "sess_003",
            "ended_at": "2025-10-28T11:00:00Z",
            "total_pages": 15
        }
    """.trimIndent()

        val response = gson.fromJson(json, EndSessionResponse::class.java)

        assertEquals("sess_003", response.session_id)
        assertEquals("2025-10-28T11:00:00Z", response.ended_at)
        assertEquals(15, response.total_pages)
    }

    // 4. sessionStats
    @Test
    fun sessionStatsResponse_successfulParsing() {
        val json = """
        {
            "session_id": "sess_004",
            "user_id": "user_stats_1",
            "started_at": "2025-10-28T09:00:00Z",
            "ended_at": "2025-10-28T09:45:00Z",
            "total_pages": 10,
            "total_time_spent": 2700,
            "total_words_read": 15000
        }
    """.trimIndent()

        val response = gson.fromJson(json, SessionStatsResponse::class.java)

        assertEquals("sess_004", response.session_id)
        assertEquals("user_stats_1", response.user_id)
        assertEquals(2700, response.total_time_spent)
        assertEquals(15000, response.total_words_read)
    }

    @Test
    fun sessionStatsResponse_invalidType_throwsException() {
        val json = """
        {
            "session_id": "sess_008_invalid",
            "user_id": "user_invalid",
            "total_time_spent": "not_a_number",
            "total_words_read": 1000
        }
    """.trimIndent()

        try {
            gson.fromJson(json, SessionStatsResponse::class.java)
            fail("Expected JsonSyntaxException to be thrown")
        } catch (e: JsonSyntaxException) {
            assertTrue(e.message?.contains("For input string") == true)
        }
    }

    @Test
    fun sessionStatsResponse_isOngoingBooleanParsing() {
        // Case 1: isOngoing = true
        var json = """
        {
            "session_id": "S_ONGOING",
            "user_id": "user_stats_2",
            "isOngoing": true,
            "total_pages": 0,
            "total_time_spent": 0,
            "total_words_read": 0
        }
    """.trimIndent()
        var response = gson.fromJson(json, SessionStatsResponse::class.java)
        assertTrue("isOngoing should be true", response.isOngoing)

        // Case 2: isOngoing = false
        json = """
        {
            "session_id": "S_ENDED",
            "user_id": "user_stats_3",
            "isOngoing": false,
            "total_pages": 5,
            "total_time_spent": 100,
            "total_words_read": 500
        }
    """.trimIndent()
        response = gson.fromJson(json, SessionStatsResponse::class.java)
        assertFalse("isOngoing should be false", response.isOngoing)
    }

    // 5. reloadAllSession
    @Test
    fun reloadAllSessionResponse_successfulParsing() {
        val json = """
        {
            "session_id": "sess_reload_001",
            "started_at": "2025-10-28T12:00:00Z",
            "pages": [
                {
                    "page_index": 0,
                    "img_url": "http://example.com/img0",
                    "translation_text": "Hello",
                    "audio_url": "http://example.com/audio0",
                    "ocr_results": []
                },
                {
                    "page_index": 1,
                    "img_url": null,
                    "translation_text": null,
                    "audio_url": null,
                    "ocr_results": null
                }
            ]
        }
        """.trimIndent()

        val response = gson.fromJson(json, ReloadAllSessionResponse::class.java)

        assertEquals("sess_reload_001", response.session_id)
        assertEquals("2025-10-28T12:00:00Z", response.started_at)
        assertEquals(2, response.pages.size)
    }

    @Test
    fun reloadedPage_parsingWithNullableFields() {
        val json = """
        {
            "session_id": "sess_reload_002",
            "started_at": "2025-10-28T12:30:00Z",
            "pages": [
                {
                    "page_index": 5,
                    "img_url": null,
                    "translation_text": "Sample text",
                    "audio_url": null,
                    "ocr_results": []
                }
            ]
        }
        """.trimIndent()

        val response = gson.fromJson(json, ReloadAllSessionResponse::class.java)
        val page = response.pages[0]

        assertEquals(5, page.page_index)
        assertNull(page.img_url)
        assertEquals("Sample text", page.translation_text)
        assertNull(page.audio_url)
        assertTrue(page.ocr_results?.isEmpty() == true)
    }

    // 6. discardSession
    @Test
    fun discardSessionResponse_successfulParsing() {
        val json = """ 
        { 
            "message": "session discarded successfully" 
        } 
        """.trimIndent()

        val response = gson.fromJson(json, DiscardSessionResponse::class.java)

        assertEquals("session discarded successfully", response.message)
    }
}
