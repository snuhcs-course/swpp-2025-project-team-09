from django.test import TestCase
from unittest.mock import Mock, patch, MagicMock
from rest_framework import status
from rest_framework.test import APIRequestFactory
from apis.controller.user_controller.views import (
    UserRegisterView,
    UserLoginView,
    UserChangeLangView,
    UserInfoView,
)
from apis.models.user_model import User
from django.utils import timezone
import uuid


class TestUserRegisterView(TestCase):
    """Unit tests for UserRegisterView - testing view logic only"""

    def setUp(self):
        """Set up test fixtures"""
        self.factory = APIRequestFactory()
        self.view = UserRegisterView.as_view()

    @patch("apis.controller.user_controller.views.User.objects.filter")
    @patch("apis.controller.user_controller.views.User.objects.create")
    def test_01_register_success(self, mock_create, mock_filter):
        """01: Test successful user registration"""
        # Setup mocks
        mock_filter.return_value.exists.return_value = False
        mock_user = Mock()
        mock_user.uid = uuid.uuid4()
        mock_user.language_preference = "en"
        mock_create.return_value = mock_user

        # Make request
        data = {"device_info": "test-device-123", "language_preference": "en"}
        request = self.factory.post("/user/register", data, format="json")
        response = self.view(request)

        # Assertions
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertIn("user_id", response.data)
        self.assertIn("language_preference", response.data)
        self.assertEqual(response.data["language_preference"], "en")

        # Verify mocks called correctly
        mock_filter.assert_called_once_with(device_info="test-device-123")
        mock_create.assert_called_once()

    @patch("apis.controller.user_controller.views.User.objects.filter")
    def test_02_register_missing_device_info(self, mock_filter):
        """02: Test registration with missing device_info"""
        data = {"language_preference": "en"}
        request = self.factory.post("/user/register", data, format="json")
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertEqual(response.data["error_code"], 400)
        self.assertEqual(response.data["message"], "USER__INVALID_REQUEST_BODY")

        # Verify no database calls were made
        mock_filter.assert_not_called()

    @patch("apis.controller.user_controller.views.User.objects.filter")
    def test_03_register_missing_language_preference(self, mock_filter):
        """03: Test registration with missing language_preference"""
        data = {"device_info": "test-device-456"}
        request = self.factory.post("/user/register", data, format="json")
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertEqual(response.data["error_code"], 400)
        self.assertEqual(response.data["message"], "USER__INVALID_REQUEST_BODY")

        # Verify no database calls were made
        mock_filter.assert_not_called()

    @patch("apis.controller.user_controller.views.User.objects.filter")
    def test_04_register_duplicate_device(self, mock_filter):
        """04: Test registration with already registered device"""
        # Setup mock to indicate device already exists
        mock_filter.return_value.exists.return_value = True

        data = {"device_info": "existing-device", "language_preference": "ko"}
        request = self.factory.post("/user/register", data, format="json")
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_409_CONFLICT)
        self.assertEqual(response.data["error_code"], 409)
        self.assertEqual(response.data["message"], "USER__DEVICE_ALREADY_REGISTERED")

        # Verify filter was called but not create
        mock_filter.assert_called_once_with(device_info="existing-device")

    @patch("apis.controller.user_controller.views.User.objects.filter")
    def test_05_register_empty_device_info(self, mock_filter):
        """05: Test registration with empty string device_info"""
        data = {"device_info": "", "language_preference": "en"}
        request = self.factory.post("/user/register", data, format="json")
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertEqual(response.data["error_code"], 400)
        self.assertEqual(response.data["message"], "USER__INVALID_REQUEST_BODY")

    @patch("apis.controller.user_controller.views.User.objects.filter")
    def test_06_register_empty_language_preference(self, mock_filter):
        """06: Test registration with empty string language_preference"""
        data = {"device_info": "test-device-789", "language_preference": ""}
        request = self.factory.post("/user/register", data, format="json")
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertEqual(response.data["error_code"], 400)
        self.assertEqual(response.data["message"], "USER__INVALID_REQUEST_BODY")

    @patch("apis.controller.user_controller.views.User.objects.filter")
    @patch("apis.controller.user_controller.views.User.objects.create")
    def test_07_register_database_error(self, mock_create, mock_filter):
        """07: Test registration when database error occurs"""
        mock_filter.return_value.exists.return_value = False
        mock_create.side_effect = Exception("Database error")

        data = {"device_info": "test-device", "language_preference": "en"}
        request = self.factory.post("/user/register", data, format="json")
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_500_INTERNAL_SERVER_ERROR)
        self.assertEqual(response.data["error_code"], 500)
        self.assertEqual(response.data["message"], "SERVER__INTERNAL_ERROR")


class TestUserLoginView(TestCase):
    """Unit tests for UserLoginView - testing view logic only"""

    def setUp(self):
        """Set up test fixtures"""
        self.factory = APIRequestFactory()
        self.view = UserLoginView.as_view()

    @patch("apis.controller.user_controller.views.User.objects.get")
    def test_08_login_success(self, mock_get):
        """08: Test successful user login"""
        # Setup mock
        mock_user = Mock()
        mock_user.uid = uuid.uuid4()
        mock_user.language_preference = "en"
        mock_get.return_value = mock_user

        data = {"device_info": "test-login-device"}
        request = self.factory.post("/user/login", data, format="json")
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertIn("user_id", response.data)
        self.assertIn("language_preference", response.data)
        self.assertEqual(response.data["language_preference"], "en")

        # Verify mock called correctly
        mock_get.assert_called_once_with(device_info="test-login-device")

    @patch("apis.controller.user_controller.views.User.objects.get")
    def test_09_login_missing_device_info(self, mock_get):
        """09: Test login with missing device_info"""
        data = {}
        request = self.factory.post("/user/login", data, format="json")
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        mock_get.assert_not_called()

    @patch("apis.controller.user_controller.views.User.objects.get")
    def test_10_login_device_not_registered(self, mock_get):
        """10: Test login with non-existent device"""
        mock_get.side_effect = User.DoesNotExist

        data = {"device_info": "non-existent-device"}
        request = self.factory.post("/user/login", data, format="json")
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)
        self.assertEqual(response.data["error_code"], 404)
        self.assertEqual(response.data["message"], "USER__DEVICE_NOT_REGISTERED")

    @patch("apis.controller.user_controller.views.User.objects.get")
    def test_11_login_empty_device_info(self, mock_get):
        """11: Test login with empty string device_info"""
        data = {"device_info": ""}
        request = self.factory.post("/user/login", data, format="json")
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        mock_get.assert_not_called()

    @patch("apis.controller.user_controller.views.User.objects.get")
    def test_12_login_database_error(self, mock_get):
        """12: Test login when database error occurs"""
        mock_get.side_effect = Exception("Database error")

        data = {"device_info": "test-device"}
        request = self.factory.post("/user/login", data, format="json")
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_500_INTERNAL_SERVER_ERROR)


class TestUserChangeLangView(TestCase):
    """Improved unit tests for UserChangeLangView"""

    def setUp(self):
        self.factory = APIRequestFactory()
        self.view = UserChangeLangView.as_view()

    @patch("apis.controller.user_controller.views.User.objects.get")
    def test_13_change_language_success(self, mock_get):
        """13: Test successful language change with full verification"""

        # Mock user with explicit attributes and save() method
        mock_user = Mock()
        mock_user.uid = uuid.uuid4()
        mock_user.language_preference = "en"
        mock_user.updated_at = timezone.now()
        old_updated_at = mock_user.updated_at

        mock_user.save = Mock()  # ensure explicit save method exists
        mock_get.return_value = mock_user

        data = {"device_info": "test-lang-device", "language_preference": "ko"}
        request = self.factory.patch("/user/lang", data, format="json")
        response = self.view(request)

        # Response basics
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data["language_preference"], "ko")

        # Ensure view updated values correctly
        self.assertNotEqual(
            mock_user.updated_at, old_updated_at
        )  # updated timestamp must change
        self.assertEqual(mock_user.language_preference, "ko")  # language must change

        # Ensure DB get() and save() are called
        mock_get.assert_called_once_with(device_info="test-lang-device")
        mock_user.save.assert_called_once()

    @patch("apis.controller.user_controller.views.User.objects.get")
    def test_14_change_language_missing_device_info(self, mock_get):
        """14: Missing device_info should return 400"""
        data = {"language_preference": "ko"}
        request = self.factory.patch("/user/lang", data, format="json")
        response = self.view(request)

        self.assertEqual(response.status_code, 400)
        mock_get.assert_not_called()

    @patch("apis.controller.user_controller.views.User.objects.get")
    def test_15_change_language_missing_language_preference(self, mock_get):
        """15: Missing language_preference should return 400"""
        data = {"device_info": "test-lang-device"}
        request = self.factory.patch("/user/lang", data, format="json")
        response = self.view(request)

        self.assertEqual(response.status_code, 400)
        mock_get.assert_not_called()

    @patch("apis.controller.user_controller.views.User.objects.get")
    def test_16_change_language_device_not_found(self, mock_get):
        """16: Device doesn't exist → 404"""
        mock_get.side_effect = User.DoesNotExist

        data = {"device_info": "non-existent", "language_preference": "ko"}
        request = self.factory.patch("/user/lang", data, format="json")
        response = self.view(request)

        self.assertEqual(response.status_code, 404)
        self.assertEqual(response.data["error_code"], 404)
        self.assertEqual(response.data["message"], "USER__DEVICE_NOT_REGISTERED")

    @patch("apis.controller.user_controller.views.User.objects.get")
    def test_17_change_language_same_language(self, mock_get):
        """17: Changing to the same language still succeeds"""
        mock_user = Mock()
        mock_user.uid = uuid.uuid4()
        mock_user.language_preference = "en"
        mock_user.updated_at = timezone.now()
        mock_user.save = Mock()

        mock_get.return_value = mock_user

        data = {"device_info": "device", "language_preference": "en"}
        request = self.factory.patch("/user/lang", data, format="json")
        response = self.view(request)

        self.assertEqual(response.status_code, 200)
        mock_user.save.assert_called_once()

    @patch("apis.controller.user_controller.views.User.objects.get")
    def test_18_change_language_empty_device_info(self, mock_get):
        """18: Empty device_info should 400"""
        data = {"device_info": "", "language_preference": "ko"}
        request = self.factory.patch("/user/lang", data, format="json")
        response = self.view(request)

        self.assertEqual(response.status_code, 400)
        mock_get.assert_not_called()

    @patch("apis.controller.user_controller.views.User.objects.get")
    def test_19_change_language_empty_language_preference(self, mock_get):
        """19: Empty language_preference should 400"""
        data = {"device_info": "device", "language_preference": ""}
        request = self.factory.patch("/user/lang", data, format="json")
        response = self.view(request)

        self.assertEqual(response.status_code, 400)
        mock_get.assert_not_called()

    @patch("apis.controller.user_controller.views.User.objects.get")
    def test_20_change_language_database_error(self, mock_get):
        """20: Database error → 500"""
        mock_get.side_effect = Exception("DB error")

        data = {"device_info": "device", "language_preference": "ko"}
        request = self.factory.patch("/user/lang", data, format="json")
        response = self.view(request)

        self.assertEqual(response.status_code, 500)


class TestUserInfoView(TestCase):
    """Unit tests for UserInfoView - testing view logic only"""

    def setUp(self):
        """Set up test fixtures"""
        self.factory = APIRequestFactory()
        self.view = UserInfoView.as_view()

    @patch("apis.controller.user_controller.views.User.objects.get")
    @patch("apis.controller.user_controller.views.Session.objects.filter")
    def test_21_get_user_info_no_sessions(self, mock_session_filter, mock_user_get):
        """21: Test getting user info with no sessions"""
        mock_user = Mock()
        mock_user.uid = uuid.uuid4()
        mock_user_get.return_value = mock_user
        mock_session_filter.return_value.exists.return_value = False

        request = self.factory.get("/user/info", {"device_info": "test-info-device"})
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertIsInstance(response.data, list)
        self.assertEqual(len(response.data), 0)

    @patch("apis.controller.user_controller.views.User.objects.get")
    @patch("apis.controller.user_controller.views.Session.objects.filter")
    @patch("apis.controller.user_controller.views.Page.objects.filter")
    def test_22_get_user_info_translated_title_none(
        self, mock_page_filter, mock_session_filter, mock_user_get
    ):
        """22: Test session where translated_title=None"""
        # mock user
        mock_user = Mock()
        mock_user.uid = uuid.uuid4()
        mock_user_get.return_value = mock_user

        # mock session
        mock_session = Mock()
        mock_session.id = uuid.uuid4()
        mock_session.title = "Original Title"
        mock_session.translated_title = None
        mock_session.created_at = timezone.now()

        mock_session_filter.return_value.exists.return_value = True
        mock_session_filter.return_value.__iter__.return_value = iter([mock_session])

        # no page
        mock_page_filter.return_value.exists.return_value = False

        request = self.factory.get("/user/info", {"device_info": "test-device"})
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data[0]["translated_title"], None)

    @patch("apis.controller.user_controller.views.User.objects.get")
    @patch("apis.controller.user_controller.views.Session.objects.filter")
    @patch("apis.controller.user_controller.views.Page.objects.filter")
    def test_23_get_user_info_translated_title_present(
        self, mock_page_filter, mock_session_filter, mock_user_get
    ):
        """23: Test session where translated_title is available"""
        mock_user = Mock()
        mock_user.uid = uuid.uuid4()
        mock_user_get.return_value = mock_user

        mock_session = Mock()
        mock_session.id = uuid.uuid4()
        mock_session.title = "Some Book"
        mock_session.translated_title = "번역된 책 제목"
        mock_session.created_at = timezone.now()

        mock_session_filter.return_value.exists.return_value = True
        mock_session_filter.return_value.__iter__.return_value = iter([mock_session])

        mock_page_filter.return_value.exists.return_value = False

        request = self.factory.get("/user/info", {"device_info": "test-device"})
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data[0]["translated_title"], "번역된 책 제목")

    @patch("apis.controller.user_controller.views.User.objects.get")
    @patch("apis.controller.user_controller.views.Session.objects.filter")
    @patch("apis.controller.user_controller.views.Page.objects.filter")
    def test_24_get_user_info_with_session_no_pages(
        self, mock_page_filter, mock_session_filter, mock_user_get
    ):
        """24: Test getting user info with session but no pages"""
        # Setup user mock
        mock_user = Mock()
        mock_user.uid = uuid.uuid4()
        mock_user_get.return_value = mock_user

        # Setup session mock
        mock_session = Mock()
        mock_session.id = uuid.uuid4()
        mock_session.title = "Test Book No Pages"
        mock_session.translated_title = "번역된 제목"
        mock_session.created_at = timezone.now()

        mock_session_filter.return_value.exists.return_value = True
        mock_session_filter.return_value.__iter__ = Mock(
            return_value=iter([mock_session])
        )

        # Setup page mock (no pages)
        mock_page_filter.return_value.exists.return_value = False

        request = self.factory.get("/user/info", {"device_info": "test-info-device"})
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertIsInstance(response.data, list)
        self.assertEqual(len(response.data), 1)

        session_data = response.data[0]
        self.assertIn("user_id", session_data)
        self.assertIn("session_id", session_data)
        self.assertIn("title", session_data)
        self.assertIn("image_base64", session_data)
        self.assertEqual(session_data["title"], "Test Book No Pages")
        self.assertIsNone(session_data["image_base64"])

    @patch("apis.controller.user_controller.views.User.objects.get")
    @patch("apis.controller.user_controller.views.Session.objects.filter")
    @patch("apis.controller.user_controller.views.Page.objects.filter")
    def test_25_get_user_info_translated_title_empty_string(
        self, mock_page_filter, mock_session_filter, mock_user_get
    ):
        """25: Test session where translated_title="" """
        mock_user = Mock()
        mock_user.uid = uuid.uuid4()
        mock_user_get.return_value = mock_user

        mock_session = Mock()
        mock_session.id = uuid.uuid4()
        mock_session.title = "Empty Title Test"
        mock_session.translated_title = ""
        mock_session.created_at = timezone.now()

        mock_session_filter.return_value.exists.return_value = True
        mock_session_filter.return_value.__iter__.return_value = iter([mock_session])

        mock_page_filter.return_value.exists.return_value = False

        request = self.factory.get("/user/info", {"device_info": "test-device"})
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data[0]["translated_title"], "")

    @patch("apis.controller.user_controller.views.User.objects.get")
    @patch("apis.controller.user_controller.views.Session.objects.filter")
    @patch("apis.controller.user_controller.views.Page.objects.filter")
    @patch("builtins.open", create=True)
    @patch("base64.b64encode")
    def test_26_get_user_info_with_session_and_page(
        self,
        mock_b64encode,
        mock_open,
        mock_page_filter,
        mock_session_filter,
        mock_user_get,
    ):
        """26: Test getting user info with session and page with valid image"""
        # Setup user mock
        mock_user = Mock()
        mock_user.uid = uuid.uuid4()
        mock_user_get.return_value = mock_user

        # Setup session mock
        mock_session = Mock()
        mock_session.id = uuid.uuid4()
        mock_session.title = "Test Book With Pages"
        mock_session.translated_title = "페이지가 있는 책"
        mock_session.created_at = timezone.now()

        mock_session_filter.return_value.exists.return_value = True
        mock_session_filter.return_value.__iter__ = Mock(
            return_value=iter([mock_session])
        )

        # Setup page mock
        mock_page = Mock()
        mock_page.img_url = "/fake/path/image.jpg"
        mock_page_filter.return_value.exists.return_value = True
        mock_page_filter.return_value.first.return_value = mock_page

        # Setup file and base64 mocks
        mock_file = MagicMock()
        mock_file.read.return_value = b"fake image content"
        mock_open.return_value.__enter__.return_value = mock_file
        mock_b64encode.return_value = b"ZmFrZSBpbWFnZSBjb250ZW50"

        request = self.factory.get("/user/info", {"device_info": "test-info-device"})
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertIsInstance(response.data, list)
        self.assertEqual(len(response.data), 1)

        session_data = response.data[0]
        self.assertEqual(session_data["title"], "Test Book With Pages")
        self.assertIsNotNone(session_data["image_base64"])

    @patch("apis.controller.user_controller.views.User.objects.get")
    @patch("apis.controller.user_controller.views.Session.objects.filter")
    @patch("apis.controller.user_controller.views.Page.objects.filter")
    @patch("builtins.open", side_effect=FileNotFoundError)
    def test_27_get_user_info_with_missing_image_file(
        self, mock_open, mock_page_filter, mock_session_filter, mock_user_get
    ):
        """27: Test getting user info when image file doesn't exist"""
        # Setup user mock
        mock_user = Mock()
        mock_user.uid = uuid.uuid4()
        mock_user_get.return_value = mock_user

        # Setup session mock
        mock_session = Mock()
        mock_session.id = uuid.uuid4()
        mock_session.title = "Test Book Missing Image"
        mock_session.translated_title = None
        mock_session.created_at = timezone.now()

        mock_session_filter.return_value.exists.return_value = True
        mock_session_filter.return_value.__iter__ = Mock(
            return_value=iter([mock_session])
        )

        # Setup page mock with non-existent image
        mock_page = Mock()
        mock_page.img_url = "/non/existent/path.jpg"
        mock_page_filter.return_value.exists.return_value = True
        mock_page_filter.return_value.first.return_value = mock_page

        request = self.factory.get("/user/info", {"device_info": "test-info-device"})
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(len(response.data), 1)
        self.assertIsNone(response.data[0]["image_base64"])

    @patch("apis.controller.user_controller.views.User.objects.get")
    @patch("apis.controller.user_controller.views.Session.objects.filter")
    @patch("apis.controller.user_controller.views.Page.objects.filter")
    def test_28_get_user_info_with_multiple_sessions(
        self, mock_page_filter, mock_session_filter, mock_user_get
    ):
        """28: Test getting user info with multiple sessions"""
        # Setup user mock
        mock_user = Mock()
        mock_user.uid = uuid.uuid4()
        mock_user_get.return_value = mock_user

        # Setup multiple session mocks
        sessions = []
        for i in range(3):
            mock_session = Mock()
            mock_session.id = uuid.uuid4()
            mock_session.title = f"Test Book {i+1}"
            mock_session.translated_title = f"테스트 책 {i+1}"
            mock_session.created_at = timezone.now()
            sessions.append(mock_session)

        mock_session_filter.return_value.exists.return_value = True
        mock_session_filter.return_value.__iter__ = Mock(return_value=iter(sessions))

        # Setup page mock (no pages for simplicity)
        mock_page_filter.return_value.exists.return_value = False

        request = self.factory.get("/user/info", {"device_info": "test-info-device"})
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(len(response.data), 3)

        titles = [session["title"] for session in response.data]
        self.assertIn("Test Book 1", titles)
        self.assertIn("Test Book 2", titles)
        self.assertIn("Test Book 3", titles)

    @patch("apis.controller.user_controller.views.User.objects.get")
    def test_29_get_user_info_missing_device_info(self, mock_user_get):
        """29: Test getting user info without device_info parameter"""
        request = self.factory.get("/user/info")
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        mock_user_get.assert_not_called()

    @patch("apis.controller.user_controller.views.User.objects.get")
    def test_30_get_user_info_device_not_found(self, mock_user_get):
        """30: Test getting user info for non-existent device"""
        mock_user_get.side_effect = User.DoesNotExist

        request = self.factory.get("/user/info", {"device_info": "non-existent-device"})
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)
        self.assertEqual(response.data["error_code"], 404)
        self.assertEqual(response.data["message"], "USER__DEVICE_NOT_REGISTERED")

    @patch("apis.controller.user_controller.views.User.objects.get")
    def test_31_get_user_info_empty_device_info(self, mock_user_get):
        """31: Test getting user info with empty device_info"""
        request = self.factory.get("/user/info", {"device_info": ""})
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        mock_user_get.assert_not_called()

    @patch("apis.controller.user_controller.views.User.objects.get")
    @patch("apis.controller.user_controller.views.Session.objects.filter")
    def test_32_get_user_info_database_error(self, mock_session_filter, mock_user_get):
        """32: Test getting user info when database error occurs"""
        mock_user = Mock()
        mock_user_get.return_value = mock_user
        mock_session_filter.side_effect = Exception("Database error")

        request = self.factory.get("/user/info", {"device_info": "test-device"})
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_500_INTERNAL_SERVER_ERROR)
