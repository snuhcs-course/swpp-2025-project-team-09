import com.example.storybridge_android.network.*
import com.google.gson.Gson
import org.junit.Test
import org.junit.Assert.*

class UserResponseParsingTest {
    private val gson = Gson()

    // 1. Login Response
    // 1. UserLoginResponse — verify correct parsing for successful login
    @Test
    fun loginResponse_successfulParsing() {
        val json = """
        {
            "user_id": "test_user_001",
            "language_preference": "en"
        }
    """.trimIndent()

        val response = gson.fromJson(json, UserLoginResponse::class.java)

        assertEquals("test_user_001", response.user_id)
        assertEquals("en", response.language_preference)
    }

    // 2. UserLoginResponse — verify nullable handling when language field missing
    @Test
    fun loginResponse_missingLanguageField() {
        val json = """
        {
            "user_id": "test_user_002"
        }
    """.trimIndent()

        val response = gson.fromJson(json, UserLoginResponse::class.java)

        assertEquals("test_user_002", response.user_id)
        assertNull("language_preference should be null if missing", response.language_preference)
    }

    // 3. UserLangResponse — verify correct parsing with datetime field
    @Test
    fun langResponse_successfulParsing_withDatetime() {
        val json = """
        {
            "user_id": "test_user_003",
            "language_preference": "en",
            "updated_at": "2025-10-28T09:30:00Z"
        }
    """.trimIndent()

        val response = gson.fromJson(json, UserLangResponse::class.java)

        assertEquals("test_user_003", response.user_id)
        assertEquals("en", response.language_preference)
        assertEquals("2025-10-28T09:30:00Z", response.updated_at)
    }

    // 4. UserInfoResponse — verify proper parsing of base64 field
    @Test
    fun userInfoResponse_successfulParsing_withBase64() {
        val fakeBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYGD4Dw"
        val json = """
        {
            "user_id": "test_user_004",
            "title": "Story Bridge Project",
            "image_base64": "$fakeBase64",
            "started_at": "2024-01-15T10:00:00Z"
        }
    """.trimIndent()

        val response = gson.fromJson(json, UserInfoResponse::class.java)

        assertEquals("test_user_004", response.user_id)
        assertEquals("Story Bridge Project", response.title)
        assertEquals(fakeBase64, response.image_base64)
        assertEquals("2024-01-15T10:00:00Z", response.started_at)
    }

    // 5. UserInfoResponse — verify parsing when Base64 is empty
    @Test
    fun userInfoResponse_emptyBase64() {
        val json = """
        {
            "user_id": "test_user_005",
            "title": "Empty Image Test",
            "image_base64": "",
            "started_at": "2024-02-01T00:00:00Z"
        }
    """.trimIndent()

        val response = gson.fromJson(json, UserInfoResponse::class.java)
        assertEquals("", response.image_base64)
    }

    // 6. UserLangResponse — verify handling of unexpected field
    @Test
    fun langResponse_withExtraField_ignoredSuccessfully() {
        val json = """
        {
            "user_id": "test_user_006",
            "language_preference": "ja",
            "extra_field": "ignore_this"
        }
    """.trimIndent()

        val response = gson.fromJson(json, UserLangResponse::class.java)

        assertEquals("test_user_006", response.user_id)
        assertEquals("ja", response.language_preference)
    }

    // 7. UserLoginResponse — verify invalid type handling for language_preference
    @Test
    fun loginResponse_numericToString_convertedAutomatically() {
        val json = """
        {
            "user_id": "test_user_007",
            "language_preference": 123
        }
    """.trimIndent()

        val response = gson.fromJson(json, UserLoginResponse::class.java)
        assertEquals("123", response.language_preference)
    }
}
