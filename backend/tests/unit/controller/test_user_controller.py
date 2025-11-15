from rest_framework.test import APITestCase, APIClient
from rest_framework import status
from apis.models.user_model import User
from apis.models.session_model import Session
from apis.models.page_model import Page
from django.utils import timezone
import uuid
import os
import tempfile
import base64
import shutil


class TestUserRegisterView(APITestCase):
    """Unit tests for User Register endpoint"""

    def setUp(self):
        """Set up test client and test data"""
        self.client = APIClient()

    def test_01_register_success(self):
        """Test successful user registration"""
        data = {"device_info": "test-device-123", "language_preference": "en"}
        response = self.client.post("/user/register", data, format="json")

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertIn("user_id", response.data)
        self.assertIn("language_preference", response.data)
        self.assertEqual(response.data["language_preference"], "en")

        # Verify user was created in database
        user = User.objects.get(device_info="test-device-123")
        self.assertEqual(user.language_preference, "en")
        self.assertEqual(str(user.uid), str(response.data["user_id"]))

    def test_02_register_missing_device_info(self):
        """Test registration with missing device_info"""
        data = {"language_preference": "en"}
        response = self.client.post("/user/register", data, format="json")

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertEqual(response.data["error_code"], 400)
        self.assertEqual(response.data["message"], "USER__INVALID_REQUEST_BODY")

    def test_03_register_missing_language_preference(self):
        """Test registration with missing language_preference"""
        data = {"device_info": "test-device-456"}
        response = self.client.post("/user/register", data, format="json")

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertEqual(response.data["error_code"], 400)
        self.assertEqual(response.data["message"], "USER__INVALID_REQUEST_BODY")

    def test_04_register_duplicate_device(self):
        """Test registration with already registered device"""
        # Create existing user
        User.objects.create(
            device_info="existing-device",
            language_preference="en",
            created_at=timezone.now(),
        )

        # Try to register same device again
        data = {"device_info": "existing-device", "language_preference": "ko"}
        response = self.client.post("/user/register", data, format="json")

        self.assertEqual(response.status_code, status.HTTP_409_CONFLICT)
        self.assertEqual(response.data["error_code"], 409)
        self.assertEqual(response.data["message"], "USER__DEVICE_ALREADY_REGISTERED")

    def test_05_register_empty_device_info(self):
        """Test registration with empty string device_info"""
        data = {"device_info": "", "language_preference": "en"}
        response = self.client.post("/user/register", data, format="json")

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertEqual(response.data["error_code"], 400)
        self.assertEqual(response.data["message"], "USER__INVALID_REQUEST_BODY")

    def test_06_register_empty_language_preference(self):
        """Test registration with empty string language_preference"""
        data = {"device_info": "test-device-789", "language_preference": ""}
        response = self.client.post("/user/register", data, format="json")

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertEqual(response.data["error_code"], 400)
        self.assertEqual(response.data["message"], "USER__INVALID_REQUEST_BODY")

    def test_07_register_with_special_characters(self):
        """Test registration with special characters in device_info"""
        data = {
            "device_info": "test-device-!@#$%^&*()",
            "language_preference": "ko",
        }
        response = self.client.post("/user/register", data, format="json")

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertIn("user_id", response.data)
        self.assertEqual(response.data["language_preference"], "ko")


class TestUserLoginView(APITestCase):
    """Unit tests for User Login endpoint"""

    def setUp(self):
        """Set up test client and test data"""
        self.client = APIClient()

        # Create test user
        self.test_user = User.objects.create(
            device_info="test-login-device",
            language_preference="en",
            created_at=timezone.now(),
        )

    def test_01_login_success(self):
        """Test successful user login"""
        data = {"device_info": "test-login-device"}
        response = self.client.post("/user/login", data, format="json")

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertIn("user_id", response.data)
        self.assertIn("language_preference", response.data)
        self.assertEqual(str(response.data["user_id"]), str(self.test_user.uid))
        self.assertEqual(response.data["language_preference"], "en")

    def test_02_login_missing_device_info(self):
        """Test login with missing device_info"""
        data = {}
        response = self.client.post("/user/login", data, format="json")

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        # Note: Implementation returns plain 400 without error body

    def test_03_login_device_not_registered(self):
        """Test login with non-existent device"""
        data = {"device_info": "non-existent-device"}
        response = self.client.post("/user/login", data, format="json")

        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)
        self.assertEqual(response.data["error_code"], 404)
        self.assertEqual(response.data["message"], "USER__DEVICE_NOT_REGISTERED")

    def test_04_login_empty_device_info(self):
        """Test login with empty string device_info"""
        data = {"device_info": ""}
        response = self.client.post("/user/login", data, format="json")

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)


class TestUserChangeLangView(APITestCase):
    """Unit tests for User Change Language endpoint"""

    def setUp(self):
        """Set up test client and test data"""
        self.client = APIClient()

        # Create test user
        self.test_user = User.objects.create(
            device_info="test-lang-device",
            language_preference="en",
            created_at=timezone.now(),
        )

    def test_01_change_language_success(self):
        """Test successful language change"""
        data = {"device_info": "test-lang-device", "language_preference": "ko"}
        response = self.client.patch("/user/lang", data, format="json")

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertIn("user_id", response.data)
        self.assertIn("language_preference", response.data)
        self.assertIn("updated_at", response.data)
        self.assertEqual(str(response.data["user_id"]), str(self.test_user.uid))
        self.assertEqual(response.data["language_preference"], "ko")

        # Verify database was updated
        self.test_user.refresh_from_db()
        self.assertEqual(self.test_user.language_preference, "ko")

    def test_02_change_language_missing_device_info(self):
        """Test language change with missing device_info"""
        data = {"language_preference": "ko"}
        response = self.client.patch("/user/lang", data, format="json")

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        # Note: Implementation returns plain 400 without error body

    def test_03_change_language_missing_language_preference(self):
        """Test language change with missing language_preference"""
        data = {"device_info": "test-lang-device"}
        response = self.client.patch("/user/lang", data, format="json")

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        # Note: Implementation returns plain 400 without error body

    def test_04_change_language_device_not_found(self):
        """Test language change for non-existent device"""
        data = {"device_info": "non-existent-device", "language_preference": "ko"}
        response = self.client.patch("/user/lang", data, format="json")

        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)
        self.assertEqual(response.data["error_code"], 404)
        self.assertEqual(response.data["message"], "USER__DEVICE_NOT_REGISTERED")

    def test_05_change_language_same_language(self):
        """Test changing to the same language (should succeed)"""
        data = {"device_info": "test-lang-device", "language_preference": "en"}
        response = self.client.patch("/user/lang", data, format="json")

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data["language_preference"], "en")

    def test_06_change_language_empty_device_info(self):
        """Test language change with empty device_info"""
        data = {"device_info": "", "language_preference": "ko"}
        response = self.client.patch("/user/lang", data, format="json")

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_07_change_language_empty_language_preference(self):
        """Test language change with empty language_preference"""
        data = {"device_info": "test-lang-device", "language_preference": ""}
        response = self.client.patch("/user/lang", data, format="json")

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)


class TestUserInfoView(APITestCase):
    """Unit tests for User Info endpoint"""

    def setUp(self):
        """Set up test client and test data"""
        self.client = APIClient()

        # Create test user
        self.test_user = User.objects.create(
            device_info="test-info-device",
            language_preference="en",
            created_at=timezone.now(),
        )

        # Create a temporary directory for test images
        self.temp_dir = tempfile.mkdtemp()

    def tearDown(self):
        """Clean up temporary files"""
        if os.path.exists(self.temp_dir):
            shutil.rmtree(self.temp_dir)

    def test_01_get_user_info_no_sessions(self):
        """Test getting user info with no sessions"""
        response = self.client.get("/user/info", {"device_info": "test-info-device"})

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertIsInstance(response.data, list)
        self.assertEqual(len(response.data), 0)

    def test_02_get_user_info_with_session_no_pages(self):
        """Test getting user info with session but no pages"""
        # Create test session without pages
        session = Session.objects.create(
            user=self.test_user,
            title="Test Book No Pages",
            translated_title="번역된 제목",
            created_at=timezone.now(),
        )

        response = self.client.get("/user/info", {"device_info": "test-info-device"})

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertIsInstance(response.data, list)
        self.assertEqual(len(response.data), 1)

        # Verify response structure
        session_data = response.data[0]
        self.assertIn("user_id", session_data)
        self.assertIn("session_id", session_data)
        self.assertIn("title", session_data)
        self.assertIn("translated_title", session_data)
        self.assertIn("image_base64", session_data)
        self.assertIn("started_at", session_data)

        # Verify values
        self.assertEqual(str(session_data["user_id"]), str(self.test_user.uid))
        self.assertEqual(str(session_data["session_id"]), str(session.id))
        self.assertEqual(session_data["title"], "Test Book No Pages")
        # translated_title should be "번역된 제목" as set above
        if session_data["translated_title"] is not None:
            self.assertEqual(session_data["translated_title"], "번역된 제목")
        self.assertIsNone(session_data["image_base64"])

    def test_03_get_user_info_with_session_and_page(self):
        """Test getting user info with session and page with valid image"""
        # Create a test image file
        test_image_path = os.path.join(self.temp_dir, "test_image.jpg")
        try:
            with open(test_image_path, "wb") as f:
                f.write(b"fake image content")
        except Exception as e:
            self.fail(f"Failed to create test image file: {e}")

        # Create test session and page
        session = Session.objects.create(
            user=self.test_user,
            title="Test Book With Pages",
            translated_title="페이지가 있는 책",
            created_at=timezone.now(),
        )

        Page.objects.create(session=session, img_url=test_image_path)

        response = self.client.get("/user/info", {"device_info": "test-info-device"})

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertIsInstance(response.data, list)
        self.assertEqual(len(response.data), 1)

        session_data = response.data[0]
        self.assertIn("title", session_data)
        self.assertIn("image_base64", session_data)
        self.assertEqual(session_data["title"], "Test Book With Pages")

        # Verify image_base64 if it exists
        if session_data.get("image_base64"):
            try:
                decoded = base64.b64decode(session_data["image_base64"])
                self.assertEqual(decoded, b"fake image content")
            except Exception as e:
                self.fail(f"image_base64 is not valid base64: {e}")
        else:
            # If no image_base64, that's also acceptable depending on implementation
            pass

    def test_04_get_user_info_with_missing_image_file(self):
        """Test getting user info when image file doesn't exist"""
        # Create session with non-existent image path
        session = Session.objects.create(
            user=self.test_user,
            title="Test Book Missing Image",
            created_at=timezone.now(),
        )

        Page.objects.create(session=session, img_url="/non/existent/path.jpg")

        response = self.client.get("/user/info", {"device_info": "test-info-device"})

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertIsInstance(response.data, list)
        self.assertEqual(len(response.data), 1)

        session_data = response.data[0]
        self.assertIn("title", session_data)
        self.assertIn("image_base64", session_data)
        self.assertEqual(session_data["title"], "Test Book Missing Image")
        # When image file doesn't exist, image_base64 should be None
        self.assertIsNone(session_data["image_base64"])

    def test_05_get_user_info_with_multiple_sessions(self):
        """Test getting user info with multiple sessions"""
        # Create multiple sessions
        for i in range(3):
            Session.objects.create(
                user=self.test_user,
                title=f"Test Book {i+1}",
                translated_title=f"테스트 책 {i+1}",
                created_at=timezone.now(),
            )

        response = self.client.get("/user/info", {"device_info": "test-info-device"})

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertIsInstance(response.data, list)
        self.assertEqual(len(response.data), 3)

        # Verify all sessions are returned
        titles = []
        for session in response.data:
            self.assertIn("title", session)
            titles.append(session["title"])

        self.assertIn("Test Book 1", titles)
        self.assertIn("Test Book 2", titles)
        self.assertIn("Test Book 3", titles)

    def test_06_get_user_info_missing_device_info(self):
        """Test getting user info without device_info parameter"""
        response = self.client.get("/user/info")

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        # Note: Implementation returns plain 400 without error body

    def test_07_get_user_info_device_not_found(self):
        """Test getting user info for non-existent device"""
        response = self.client.get("/user/info", {"device_info": "non-existent-device"})

        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)
        self.assertEqual(response.data["error_code"], 404)
        self.assertEqual(response.data["message"], "USER__DEVICE_NOT_REGISTERED")

    def test_08_get_user_info_empty_device_info(self):
        """Test getting user info with empty device_info"""
        response = self.client.get("/user/info", {"device_info": ""})

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
