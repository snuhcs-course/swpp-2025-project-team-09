import com.example.storybridge_android.network.*
import com.google.gson.Gson
import org.junit.Test
import org.junit.Assert.*

class SessionResponseParsingTest {
    private val gson = Gson()

    // 1. StartSessionResponse — verify correct parsing for session start
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

    // 2. SelectVoiceResponse — verify correct parsing for voice style
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

    // 3. EndSessionResponse — verify proper parsing for session termination info
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

    // 4. SessionStatsResponse — verify correct parsing of numerical fields
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

    // 5. SessionInfoResponse — verify all fields when session is completed
    @Test
    fun sessionInfoResponse_allFieldsPresent() {
        val json = """
        {
            "session_id": "sess_005_full",
            "user_id": "user_info_1",
            "page_index": 5,
            "voice_style": "Fenrir_Neutral",
            "started_at": "2025-10-28T12:00:00Z",
            "ended_at": "2025-10-28T12:30:00Z",
            "total_pages": 8
        }
    """.trimIndent()

        val response = gson.fromJson(json, SessionInfoResponse::class.java)

        assertNotNull(response.ended_at)
        assertNotNull(response.total_pages)
        assertEquals(8, response.total_pages)
    }

    // 6. SessionInfoResponse — verify correct handling of missing nullable fields
    @Test
    fun sessionInfoResponse_nullableFieldsMissing() {
        val json = """
        {
            "session_id": "sess_005_ongoing",
            "user_id": "user_info_2",
            "page_index": 3,
            "voice_style": "Fenrir_Neutral",
            "started_at": "2025-10-28T13:00:00Z"
        }
    """.trimIndent()

        val response = gson.fromJson(json, SessionInfoResponse::class.java)

        assertEquals("sess_005_ongoing", response.session_id)
        assertEquals(3, response.page_index)
        assertNull("ended_at should be null when omitted", response.ended_at)
        assertNull("total_pages should be null when omitted", response.total_pages)
    }

    // 7. SessionReviewResponse — verify correct parsing of final review data
    @Test
    fun sessionReviewResponse_successfulParsing() {
        val json = """
        {
            "session_id": "sess_006",
            "user_id": "user_review_1",
            "started_at": "2025-10-28T14:00:00Z",
            "ended_at": "2025-10-28T14:30:00Z",
            "total_pages": 12
        }
    """.trimIndent()

        val response = gson.fromJson(json, SessionReviewResponse::class.java)

        assertEquals("sess_006", response.session_id)
        assertEquals("user_review_1", response.user_id)
        assertEquals(12, response.total_pages)
    }

    // 8. SessionInfoResponse — verify unknown fields are ignored safely
    @Test
    fun sessionInfoResponse_extraField_ignoredSuccessfully() {
        val json = """
        {
            "session_id": "sess_007_extra",
            "user_id": "user_extra",
            "page_index": 10,
            "voice_style": "Lyra_Warm",
            "started_at": "2025-10-28T15:00:00Z",
            "ended_at": "2025-10-28T15:45:00Z",
            "total_pages": 9,
            "unexpected_field": "ignore_me"
        }
    """.trimIndent()

        val response = gson.fromJson(json, SessionInfoResponse::class.java)

        assertEquals("sess_007_extra", response.session_id)
        assertEquals(9, response.total_pages)
    }

    // 9. SessionStatsResponse — verify invalid type handling for numeric field
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
        } catch (e: com.google.gson.JsonSyntaxException) {
            assertTrue(e.message?.contains("For input string") == true)
        }
    }
}
