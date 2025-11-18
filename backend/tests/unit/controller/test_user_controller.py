from rest_framework.test import APITestCase, APIClient
from rest_framework import status
from apis.models.user_model import User
from apis.models.session_model import Session
from django.utils import timezone


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
        self.assertEqual(response.data["language_preference"], "en")

        # Verify user was created in database
        user = User.objects.get(device_info="test-device-123")
        self.assertEqual(user.language_preference, "en")

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
        self.assertEqual(str(response.data["user_id"]), str(self.test_user.uid))
        self.assertEqual(response.data["language_preference"], "en")

    def test_02_login_missing_device_info(self):
        """Test login with missing device_info"""
        data = {}
        response = self.client.post("/user/login", data, format="json")

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_03_login_device_not_registered(self):
        """Test login with non-existent device"""
        data = {"device_info": "non-existent-device"}
        response = self.client.post("/user/login", data, format="json")

        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)
        self.assertEqual(response.data["error_code"], 404)
        self.assertEqual(response.data["message"], "USER__DEVICE_NOT_REGISTERED")


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
        self.assertEqual(str(response.data["user_id"]), str(self.test_user.uid))
        self.assertEqual(response.data["language_preference"], "ko")
        self.assertIn("updated_at", response.data)

        # Verify database was updated
        self.test_user.refresh_from_db()
        self.assertEqual(self.test_user.language_preference, "ko")

    def test_02_change_language_missing_device_info(self):
        """Test language change with missing device_info"""
        data = {"language_preference": "ko"}
        response = self.client.patch("/user/lang", data, format="json")

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_03_change_language_missing_language_preference(self):
        """Test language change with missing language_preference"""
        data = {"device_info": "test-lang-device"}
        response = self.client.patch("/user/lang", data, format="json")

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_04_change_language_device_not_found(self):
        """Test language change for non-existent device"""
        data = {"device_info": "non-existent-device", "language_preference": "ko"}
        response = self.client.patch("/user/lang", data, format="json")

        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)
        self.assertEqual(response.data["error_code"], 404)
        self.assertEqual(response.data["message"], "USER__DEVICE_NOT_REGISTERED")


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

    def test_01_get_user_info_no_sessions(self):
        """Test getting user info with no sessions"""
        response = self.client.get("/user/info", {"device_info": "test-info-device"})

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertIsInstance(response.data, list)
        self.assertEqual(len(response.data), 0)

    def test_02_get_user_info_with_sessions(self):
        """Test getting user info with sessions"""
        # Create test session
        Session.objects.create(
            user=self.test_user, title="Test Book", created_at=timezone.now()
        )

        response = self.client.get("/user/info", {"device_info": "test-info-device"})

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertIsInstance(response.data, list)
        self.assertEqual(len(response.data), 1)
        self.assertEqual(response.data[0]["title"], "Test Book")

    def test_03_get_user_info_missing_device_info(self):
        """Test getting user info without device_info parameter"""
        response = self.client.get("/user/info")

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_04_get_user_info_device_not_found(self):
        """Test getting user info for non-existent device"""
        response = self.client.get("/user/info", {"device_info": "non-existent-device"})

        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)
        self.assertEqual(response.data["error_code"], 404)
        self.assertEqual(response.data["message"], "USER__DEVICE_NOT_REGISTERED")
