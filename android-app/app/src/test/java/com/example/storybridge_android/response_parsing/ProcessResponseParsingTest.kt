import com.example.storybridge_android.network.*
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import org.junit.Test
import org.junit.Assert.*

class ProcessResponseParsingTest {
    private val gson = Gson()

    // ========== UploadImageResponse ==========
    @Test
    fun uploadImageResponse_successfulParsing() {
        val json = """
        {
            "session_id": "sess_upload_001",
            "page_index": 2,
            "status": "submitted",
            "submitted_at": "2025-10-28T19:00:00Z"
        }
        """.trimIndent()

        val response = gson.fromJson(json, UploadImageResponse::class.java)

        assertEquals("sess_upload_001", response.session_id)
        assertEquals(2, response.page_index)
        assertEquals("submitted", response.status)
        assertEquals("2025-10-28T19:00:00Z", response.submitted_at)
    }

    // ========== UploadCoverResponse (추가) ==========
    @Test
    fun uploadCoverResponse_successfulParsing() {
        val json = """
        {
            "session_id": "sess_cover_001",
            "page_index": 0,
            "status": "submitted",
            "submitted_at": "2025-10-28T19:00:00Z",
            "title": "Original Title",
            "translated_title": "traslated_title"
        }
        """.trimIndent()

        val response = gson.fromJson(json, UploadCoverResponse::class.java)

        assertEquals("sess_cover_001", response.session_id)
        assertEquals("Original Title", response.title)
        assertEquals("traslated_title", response.translated_title)
    }

    // ========== CheckOcrResponse ==========
    @Test
    fun checkOcrResponse_readyStatus() {
        val json = """
        {
            "session_id": "sess_ocr_check_002",
            "page_index": 3,
            "status": "ready",
            "progress": 100,
            "submitted_at": "2025-10-28T19:30:00Z",
            "processed_at": "2025-10-28T19:35:00Z"
        }
        """.trimIndent()

        val response = gson.fromJson(json, CheckOcrResponse::class.java)

        assertEquals("ready", response.status)
        assertEquals(100, response.progress)
        assertNotNull("Ready status must include processed_at timestamp", response.processed_at)
        assertEquals("2025-10-28T19:35:00Z", response.processed_at)
    }

    @Test
    fun checkOcrResponse_pendingStatus_nullableFieldMissing() {
        val json = """
        {
            "session_id": "sess_ocr_check_003",
            "page_index": 4,
            "status": "pending",
            "progress": 10,
            "submitted_at": "2025-10-28T19:40:00Z"
        }
        """.trimIndent()

        val response = gson.fromJson(json, CheckOcrResponse::class.java)

        assertEquals("pending", response.status)
        assertEquals(10, response.progress)
        assertNull("Pending status should have processed_at as null", response.processed_at)
    }

    @Test
    fun checkOcrResponse_processingStatus() {
        val json = """
        {
            "session_id": "sess_ocr_check_008",
            "page_index": 5,
            "status": "processing",
            "progress": 50,
            "submitted_at": "2025-10-28T19:40:00Z"
        }
        """.trimIndent()

        val response = gson.fromJson(json, CheckOcrResponse::class.java)

        assertEquals("processing", response.status)
        assertEquals(50, response.progress)
    }

    // ========== CheckTtsResponse ==========
    @Test
    fun checkTtsResponse_readyStatus_withBbStatus() {
        val json = """
        {
            "session_id": "sess_tts_check_004",
            "page_index": 5,
            "status": "ready",
            "progress": 100,
            "bb_status": [
                {
                    "bbox_index": 0,
                    "status": "ready",
                    "has_audio": true
                },
                {
                    "bbox_index": 1,
                    "status": "failed",
                    "has_audio": false
                }
            ]
        }
        """.trimIndent()

        val response = gson.fromJson(json, CheckTtsResponse::class.java)

        assertEquals("ready", response.status)
        assertEquals(100, response.progress)
        assertNotNull(response.bb_status)
        assertEquals(2, response.bb_status?.size)

        val firstStatus = response.bb_status!![0]
        assertEquals(0, firstStatus.bbox_index)
        assertEquals("ready", firstStatus.status)
        assertTrue(firstStatus.has_audio)

        val secondStatus = response.bb_status!![1]
        assertEquals(1, secondStatus.bbox_index)
        assertEquals("failed", secondStatus.status)
        assertFalse(secondStatus.has_audio)
    }

    @Test
    fun checkTtsResponse_processingStatus_nullableBbStatusMissing() {
        val json = """
        {
            "session_id": "sess_tts_check_005",
            "page_index": 6,
            "status": "processing",
            "progress": 50
        }
        """.trimIndent()

        val response = gson.fromJson(json, CheckTtsResponse::class.java)

        assertEquals("processing", response.status)
        assertEquals(50, response.progress)
        assertNull("Processing status should have bb_status as null", response.bb_status)
    }

    @Test
    fun checkTtsResponse_emptyBbStatusList() {
        val json = """
        {
            "session_id": "sess_tts_check_009",
            "page_index": 7,
            "status": "ready",
            "progress": 100,
            "bb_status": []
        }
        """.trimIndent()

        val response = gson.fromJson(json, CheckTtsResponse::class.java)

        assertNotNull(response.bb_status)
        assertTrue(response.bb_status!!.isEmpty())
    }

    @Test
    fun checkTtsResponse_withExtraField_ignoredSuccessfully() {
        val json = """
        {
            "session_id": "sess_tts_extra_006",
            "page_index": 7,
            "status": "ready",
            "progress": 100,
            "extra_field": "ignore_me"
        }
        """.trimIndent()

        val response = gson.fromJson(json, CheckTtsResponse::class.java)

        assertEquals("sess_tts_extra_006", response.session_id)
        assertEquals("ready", response.status)
        assertEquals(100, response.progress)
    }

    // ========== Error Cases ==========
    @Test
    fun checkOcrResponse_invalidType_throwsException() {
        val json = """
        {
            "session_id": "sess_ocr_check_007",
            "page_index": 8,
            "status": "ready",
            "progress": "not_a_number"
        }
        """.trimIndent()

        try {
            gson.fromJson(json, CheckOcrResponse::class.java)
            fail("Expected JsonSyntaxException to be thrown")
        } catch (e: JsonSyntaxException) {
            assertTrue(e.message?.contains("For input string") == true ||
                    e.message?.contains("Expected") == true)
        }
    }

    @Test
    fun checkTtsResponse_invalidBbStatusType_throwsException() {
        val json = """
        {
            "session_id": "sess_tts_010",
            "page_index": 8,
            "status": "ready",
            "progress": 100,
            "bb_status": "not_a_list"
        }
        """.trimIndent()

        try {
            gson.fromJson(json, CheckTtsResponse::class.java)
            fail("Expected JsonSyntaxException to be thrown")
        } catch (e: JsonSyntaxException) {
        }
    }
}