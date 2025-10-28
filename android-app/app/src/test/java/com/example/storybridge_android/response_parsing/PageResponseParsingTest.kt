import com.example.storybridge_android.network.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.junit.Test
import org.junit.Assert.*

class PageResponseParsingTest {
    private val gson = Gson()

    // 1. Test parsing of GetImageResponse with normal JSON
    @Test
    fun getImageResponse_successfulParsing() {
        val fakeBase64 = "image_data_base64_part_1234"
        val json = """
            {
                "session_id": "sess_img_001",
                "page_index": 1,
                "image_base64": "$fakeBase64",
                "stored_at": "2025-10-28T15:00:00Z"
            }
        """.trimIndent()

        val response = gson.fromJson(json, GetImageResponse::class.java)

        assertEquals("sess_img_001", response.session_id)
        assertEquals(1, response.page_index)
        assertEquals(fakeBase64, response.image_base64)
    }

    // 2. Test nested JSON structure for OCR results (BBox and text fields)
    @Test
    fun getOcrResults_successfulParsing_withNestedObjects() {
        val json = """
            {
                "session_id": "sess_ocr_002",
                "page_index": 5,
                "ocr_results": [
                    {
                        "bbox": { 
                            "x1": 10, "y1": 20, "x2": 100, "y2": 20, 
                            "x3": 100, "y3": 50, "x4": 10, "y4": 50 
                        },
                        "original_txt": "작은 별",
                        "translation_txt": "The little star"
                    },
                    {
                        "bbox": { 
                            "x1": 150, "y1": 300, "x2": 250, "y2": 300, 
                            "x3": 250, "y3": 350, "x4": 150, "y4": 350 
                        },
                        "original_txt": "밝게 빛난다.",
                        "translation_txt": "shines bright."
                    }
                ],
                "processed_at": "2025-10-28T16:00:00Z"
            }
        """.trimIndent()

        val response = gson.fromJson(json, GetOcrTranslationResponse::class.java)

        assertEquals("sess_ocr_002", response.session_id)
        assertEquals(2, response.ocr_results.size)

        val firstOcr = response.ocr_results[0]
        assertEquals("The little star", firstOcr.translation_txt)

        assertEquals(10, firstOcr.bbox.x1)
        assertEquals(50, firstOcr.bbox.y4)

        assertEquals(90, firstOcr.bbox.width)
        assertEquals(30, firstOcr.bbox.height)
    }

    // 3. Test empty OCR list (should parse as empty)
    @Test
    fun getOcrResults_emptyListParsing() {
        val json = """
            {
                "session_id": "sess_ocr_003",
                "page_index": 6,
                "ocr_results": [],
                "processed_at": "2025-10-28T17:00:00Z"
            }
        """.trimIndent()

        val response = gson.fromJson(json, GetOcrTranslationResponse::class.java)
        assertEquals(0, response.ocr_results.size)
    }

    // 4. Test nested list parsing for TTS response
    @Test
    fun getTtsResults_successfulParsing_withNestedList() {
        val json = """
            {
                "session_id": "sess_tts_004",
                "page_index": 7,
                "audio_results": [
                    {
                        "bbox_index": 0,
                        "audio_base64_list": ["b64_word1", "b64_word2"]
                    },
                    {
                        "bbox_index": 1,
                        "audio_base64_list": ["b64_word3"]
                    }
                ],
                "generated_at": "2025-10-28T18:00:00Z"
            }
        """.trimIndent()

        val response = gson.fromJson(json, GetTtsResponse::class.java)

        assertEquals("sess_tts_004", response.session_id)
        assertEquals(2, response.audio_results.size)

        val firstAudio = response.audio_results[0]
        assertEquals(2, firstAudio.audio_base64_list.size)
        assertEquals("b64_word1", firstAudio.audio_base64_list[0])

        val secondAudio = response.audio_results[1]
        assertEquals(1, secondAudio.audio_base64_list.size)
        assertEquals("b64_word3", secondAudio.audio_base64_list[0])
    }

    // 5. Test missing field handling (when bbox is not provided)
    @Test
    fun getOcrResults_missingField_defaultsHandled() {
        val json = """
            {
                "session_id": "sess_missing_001",
                "ocr_results": [
                    { "original_txt": "No bbox", "translation_txt": "No box" }
                ]
            }
        """.trimIndent()

        val response = gson.fromJson(json, GetOcrTranslationResponse::class.java)
        assertEquals("sess_missing_001", response.session_id)
        assertEquals(1, response.ocr_results.size)
        assertNull(response.ocr_results[0].bbox)
    }

    // 6. Test type mismatch (wrong type should throw JsonSyntaxException)
    @Test
    fun getImageResponse_incorrectType_throwsException() {
        val json = """
        {
            "session_id": "sess_wrong_001",
            "page_index": "not_a_number",
            "image_base64": "b64",
            "stored_at": "2025-10-28T15:00:00Z"
        }
    """.trimIndent()

        try {
            gson.fromJson(json, GetImageResponse::class.java)
            fail("Expected JsonSyntaxException to be thrown")
        } catch (e: com.google.gson.JsonSyntaxException) {
            assertTrue(e.message?.contains("For input string") == true)
        }
    }

    // 7. Test that extra/unknown fields are safely ignored
    @Test
    fun getTtsResponse_withExtraField_ignoredSuccessfully() {
        val json = """
            {
                "session_id": "sess_extra_001",
                "page_index": 9,
                "audio_results": [],
                "generated_at": "2025-10-28T18:00:00Z",
                "extra_field": "should_be_ignored"
            }
        """.trimIndent()

        val response = gson.fromJson(json, GetTtsResponse::class.java)
        assertEquals("sess_extra_001", response.session_id)
    }
}
