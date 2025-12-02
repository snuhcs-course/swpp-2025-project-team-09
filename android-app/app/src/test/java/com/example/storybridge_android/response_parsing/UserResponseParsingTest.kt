package com.example.storybridge_android.response_parsing

import com.example.storybridge_android.network.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.junit.Test
import org.junit.Assert.*

class UserResponseParsingTest {
    private val gson = Gson()
    private val flexibleGson: Gson by lazy {
        GsonBuilder()
            // custom adapter needed to register
            .registerTypeAdapter(UserInfoResponse::class.java, FlexibleUserInfoAdapter())
            .create()
    }

    // 1. UserRegister
    @Test
    fun registerResponse_successfulParsing() {
        val json = """
        {
            "user_id": "reg_user_001",
            "language_preference": "en"
        }
        """.trimIndent()

        val response = gson.fromJson(json, UserRegisterResponse::class.java)

        assertEquals("reg_user_001", response.user_id)
        assertEquals("en", response.language_preference)
    }

    // 2. UserLogin
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

    @Test
    fun loginResponse_missingLanguageField_null() {
        val json = """
        {
            "user_id": "test_user_002"
        }
        """.trimIndent()

        val response = gson.fromJson(json, UserLoginResponse::class.java)

        assertEquals("test_user_002", response.user_id)
        assertNull("language_preference should be null if missing", response.language_preference)
    }

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

    // 3. UserLang
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

    @Test
    fun langResponse_missingUpdatedAt_allowsNull() {
        val json = """
    {
        "user_id": "test_user_x",
        "language_preference": "ko"
    }
    """.trimIndent()

        val response = gson.fromJson(json, UserLangResponse::class.java)

        assertEquals("test_user_x", response.user_id)
        assertEquals("ko", response.language_preference)
        assertNull(response.updated_at)
    }

    // 4. UserInfo
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

    @Test
    fun userInfoResponse_emptyBase64_emptyString() {
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

    @Test
    fun userInfoResponse_arrayInput_handlesSuccessfully() {
        val json = """
        [
            {
                "user_id": "test_user_008",
                "session_id": "S_ARRAY",
                "title": "Array Story",
                "translated_title": "",
                "image_base64": "",
                "started_at": "2025-02-02T00:00:00"
            }
        ]
        """.trimIndent()

        val response = flexibleGson.fromJson(json, UserInfoResponse::class.java)

        assertEquals("test_user_008", response.user_id)
        assertEquals("S_ARRAY", response.session_id)
    }

    @Test
    fun userInfoResponse_missingOptionalFields() {
        val json = """
        {
            "user_id": "user_missing",
            "title": "Missing Fields",
            "image_base64": "",
            "started_at": "2025-01-01T00:00:00Z"
        }
        """.trimIndent()

        val response = gson.fromJson(json, UserInfoResponse::class.java)

        assertEquals("user_missing", response.user_id)
        assertNull(response.session_id)
        assertNull(response.translated_title)
    }
}
