from django.test import TestCase
from unittest.mock import Mock, patch, MagicMock
from rest_framework import status
from rest_framework.test import APIRequestFactory
from apis.controller.session_controller.views import (
    StartSessionView,
    SelectVoiceView,
    EndSessionView,
    GetSessionInfoView,
    GetSessionStatsView,
    SessionReviewView,
    SessionReloadView,
    SessionReloadAllView,
    DiscardSessionView,
)
from apis.models.user_model import User
from apis.models.session_model import Session
from django.utils import timezone
from datetime import timedelta
import uuid


class TestStartSessionView(TestCase):
    """Unit tests for StartSessionView - testing view logic only"""

    def setUp(self):
        """Set up test fixtures"""
        self.factory = APIRequestFactory()
        self.view = StartSessionView.as_view()

    @patch("apis.controller.session_controller.views.User.objects.get")
    @patch("apis.controller.session_controller.views.Session.objects.create")
    def test_01_start_session_success(self, mock_session_create, mock_user_get):
        """01: Test successful session start"""
        # Setup mocks
        mock_user = Mock()
        mock_user.uid = uuid.uuid4()
        mock_user_get.return_value = mock_user

        mock_session = Mock()
        mock_session.id = uuid.uuid4()
        mock_session.created_at = timezone.now()
        mock_session_create.return_value = mock_session

        # Make request
        data = {"user_id": "test-device", "page_index": 0}
        request = self.factory.post("/session/start", data, format="json")
        response = self.view(request)

        # Assertions
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertIn("session_id", response.data)
        self.assertIn("started_at", response.data)
        self.assertEqual(response.data["page_index"], 0)

        # Verify mocks
        mock_user_get.assert_called_once_with(device_info="test-device")
        mock_session_create.assert_called_once()

    @patch("apis.controller.session_controller.views.User.objects.get")
    @patch("apis.controller.session_controller.views.Session.objects.create")
    def test_02_start_session_with_custom_page_index(
        self, mock_session_create, mock_user_get
    ):
        """02: Test session start with custom page index"""
        mock_user = Mock()
        mock_user_get.return_value = mock_user

        mock_session = Mock()
        mock_session.id = uuid.uuid4()
        mock_session.created_at = timezone.now()
        mock_session_create.return_value = mock_session

        data = {"user_id": "test-device", "page_index": 5}
        request = self.factory.post("/session/start", data, format="json")
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data["page_index"], 5)

    @patch("apis.controller.session_controller.views.User.objects.get")
    def test_03_start_session_missing_user_id(self, mock_user_get):
        """03: Test session start with missing user_id"""
        data = {"page_index": 0}
        request = self.factory.post("/session/start", data, format="json")
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        mock_user_get.assert_not_called()

    @patch("apis.controller.session_controller.views.User.objects.get")
    def test_04_start_session_user_not_found(self, mock_user_get):
        """04: Test session start with non-existent user"""
        mock_user_get.side_effect = User.DoesNotExist

        data = {"user_id": "non-existent-user", "page_index": 0}
        request = self.factory.post("/session/start", data, format="json")
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)
        self.assertEqual(response.data["error_code"], 404)
        self.assertEqual(response.data["message"], "USER__NOT_FOUND")

    @patch("apis.controller.session_controller.views.User.objects.get")
    @patch("apis.controller.session_controller.views.Session.objects.create")
    def test_05_start_session_database_error(self, mock_session_create, mock_user_get):
        """05: Test session start with database error"""
        mock_user = Mock()
        mock_user_get.return_value = mock_user
        mock_session_create.side_effect = Exception("Database error")

        data = {"user_id": "test-device", "page_index": 0}
        request = self.factory.post("/session/start", data, format="json")
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_500_INTERNAL_SERVER_ERROR)


class TestSelectVoiceView(TestCase):
    """Unit tests for SelectVoiceView - testing view logic only"""

    def setUp(self):
        """Set up test fixtures"""
        self.factory = APIRequestFactory()
        self.view = SelectVoiceView.as_view()

    @patch("apis.controller.session_controller.views.Session.objects.get")
    def test_06_select_voice_success(self, mock_session_get):
        """06: Test successful voice selection"""
        # Setup mock
        mock_session = Mock()
        mock_session.id = uuid.uuid4()
        mock_session.voicePreference = "male"
        mock_session.save = Mock()
        mock_session_get.return_value = mock_session

        data = {"session_id": str(mock_session.id), "voice_style": "male"}
        request = self.factory.post("/session/voice", data, format="json")
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data["voice_style"], "male")
        mock_session.save.assert_called_once()

    @patch("apis.controller.session_controller.views.Session.objects.get")
    def test_07_select_voice_different_style(self, mock_session_get):
        """07: Test voice selection with different style"""
        mock_session = Mock()
        mock_session.id = uuid.uuid4()
        mock_session.voicePreference = "female"
        mock_session.save = Mock()
        mock_session_get.return_value = mock_session

        data = {"session_id": str(mock_session.id), "voice_style": "female"}
        request = self.factory.post("/session/voice", data, format="json")
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data["voice_style"], "female")

    @patch("apis.controller.session_controller.views.Session.objects.get")
    def test_08_select_voice_missing_session_id(self, mock_session_get):
        """08: Test voice selection with missing session_id"""
        data = {"voice_style": "male"}
        request = self.factory.post("/session/voice", data, format="json")
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        mock_session_get.assert_not_called()

    @patch("apis.controller.session_controller.views.Session.objects.get")
    def test_09_select_voice_missing_voice_style(self, mock_session_get):
        """09: Test voice selection with missing voice_style"""
        data = {"session_id": str(uuid.uuid4())}
        request = self.factory.post("/session/voice", data, format="json")
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        mock_session_get.assert_not_called()

    @patch("apis.controller.session_controller.views.Session.objects.get")
    def test_10_select_voice_session_not_found(self, mock_session_get):
        """10: Test voice selection with non-existent session"""
        mock_session_get.side_effect = Session.DoesNotExist

        data = {"session_id": str(uuid.uuid4()), "voice_style": "male"}
        request = self.factory.post("/session/voice", data, format="json")
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)
        self.assertEqual(response.data["error_code"], 404)
        self.assertEqual(response.data["message"], "SESSION__NOT_FOUND")


class TestEndSessionView(TestCase):
    """Unit tests for EndSessionView - testing view logic only"""

    def setUp(self):
        """Set up test fixtures"""
        self.factory = APIRequestFactory()
        self.view = EndSessionView.as_view()

    @patch("apis.controller.session_controller.views.Session.objects.get")
    def test_11_end_session_success(self, mock_session_get):
        """11: Test successful session end"""
        # Setup mock
        mock_session = Mock()
        mock_session.id = uuid.uuid4()
        mock_session.totalPages = 10
        mock_session.isOngoing = True
        mock_session.ended_at = timezone.now()
        mock_session.save = Mock()
        mock_session_get.return_value = mock_session

        data = {"session_id": str(mock_session.id)}
        request = self.factory.post("/session/end", data, format="json")
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertIn("ended_at", response.data)
        self.assertEqual(response.data["total_pages"], 10)

        # Verify session was updated
        self.assertFalse(mock_session.isOngoing)
        mock_session.save.assert_called_once()

    @patch("apis.controller.session_controller.views.Session.objects.get")
    def test_12_end_session_missing_session_id(self, mock_session_get):
        """12: Test session end with missing session_id"""
        data = {}
        request = self.factory.post("/session/end", data, format="json")
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        mock_session_get.assert_not_called()

    @patch("apis.controller.session_controller.views.Session.objects.get")
    def test_13_end_session_not_found(self, mock_session_get):
        """13: Test session end with non-existent session"""
        mock_session_get.side_effect = Session.DoesNotExist

        data = {"session_id": str(uuid.uuid4())}
        request = self.factory.post("/session/end", data, format="json")
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)
        self.assertEqual(response.data["error_code"], 404)
        self.assertEqual(response.data["message"], "SESSION__NOT_FOUND")


class TestGetSessionInfoView(TestCase):
    """Unit tests for GetSessionInfoView - testing view logic only"""

    def setUp(self):
        """Set up test fixtures"""
        self.factory = APIRequestFactory()
        self.view = GetSessionInfoView.as_view()

    @patch("apis.controller.session_controller.views.Session.objects.get")
    def test_14_get_session_info_success(self, mock_session_get):
        """14: Test successful session info retrieval"""
        # Setup mock
        mock_user = Mock()
        mock_user.uid = uuid.uuid4()

        mock_session = Mock()
        mock_session.id = uuid.uuid4()
        mock_session.user = mock_user
        mock_session.voicePreference = "male"
        mock_session.isOngoing = True
        mock_session.created_at = timezone.now()
        mock_session.ended_at = None
        mock_session.totalPages = 5
        mock_session.totalWords = 500
        mock_session_get.return_value = mock_session

        request = self.factory.get(
            "/session/info", {"session_id": str(mock_session.id)}
        )
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data["voice_style"], "male")
        self.assertTrue(response.data["isOngoing"])
        self.assertEqual(response.data["total_pages"], 5)

    @patch("apis.controller.session_controller.views.Session.objects.get")
    def test_15_get_session_info_ended_session(self, mock_session_get):
        """15: Test getting info for ended session"""
        mock_user = Mock()
        mock_user.uid = uuid.uuid4()

        mock_session = Mock()
        mock_session.id = uuid.uuid4()
        mock_session.user = mock_user
        mock_session.voicePreference = "female"
        mock_session.isOngoing = False
        mock_session.created_at = timezone.now() - timedelta(hours=1)
        mock_session.ended_at = timezone.now()
        mock_session.totalPages = 10
        mock_session.totalWords = 1000
        mock_session_get.return_value = mock_session

        request = self.factory.get(
            "/session/info", {"session_id": str(mock_session.id)}
        )
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertFalse(response.data["isOngoing"])
        self.assertIsNotNone(response.data["ended_at"])

    @patch("apis.controller.session_controller.views.Session.objects.get")
    def test_16_get_session_info_missing_session_id(self, mock_session_get):
        """16: Test getting session info without session_id"""
        request = self.factory.get("/session/info")
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        mock_session_get.assert_not_called()

    @patch("apis.controller.session_controller.views.Session.objects.get")
    def test_17_get_session_info_not_found(self, mock_session_get):
        """17: Test getting info for non-existent session"""
        mock_session_get.side_effect = Session.DoesNotExist

        request = self.factory.get("/session/info", {"session_id": str(uuid.uuid4())})
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)
        self.assertEqual(response.data["error_code"], 404)
        self.assertEqual(response.data["message"], "SESSION__NOT_FOUND")


class TestGetSessionStatsView(TestCase):
    """Unit tests for GetSessionStatsView - testing view logic only"""

    def setUp(self):
        """Set up test fixtures"""
        self.factory = APIRequestFactory()
        self.view = GetSessionStatsView.as_view()

    @patch("apis.controller.session_controller.views.Session.objects.get")
    def test_18_get_session_stats_ongoing(self, mock_session_get):
        """18: Test getting stats for ongoing session"""
        mock_user = Mock()
        mock_user.uid = uuid.uuid4()

        mock_session = Mock()
        mock_session.id = uuid.uuid4()
        mock_session.user = mock_user
        mock_session.voicePreference = "male"
        mock_session.isOngoing = True
        mock_session.created_at = timezone.now()
        mock_session.ended_at = None
        mock_session.totalPages = 8
        mock_session.totalWords = 800
        mock_session_get.return_value = mock_session

        request = self.factory.get(
            "/session/stats", {"session_id": str(mock_session.id)}
        )
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data["total_pages"], 8)
        self.assertIsNone(response.data["ended_at"])

    @patch("apis.controller.session_controller.views.Session.objects.get")
    def test_19_get_session_stats_ended(self, mock_session_get):
        """19: Test getting stats for ended session with duration"""
        mock_user = Mock()
        mock_user.uid = uuid.uuid4()

        mock_session = Mock()
        mock_session.id = uuid.uuid4()
        mock_session.user = mock_user
        mock_session.voicePreference = "female"
        mock_session.isOngoing = False
        mock_session.started_at = timezone.now() - timedelta(seconds=3600)  # Fixed: use started_at
        mock_session.created_at = timezone.now() - timedelta(seconds=3600)
        mock_session.ended_at = timezone.now()
        mock_session.totalPages = 8
        mock_session.totalWords = 800
        mock_session_get.return_value = mock_session

        request = self.factory.get(
            "/session/stats", {"session_id": str(mock_session.id)}
        )
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertIsNotNone(response.data["ended_at"])
        self.assertIsNotNone(response.data["total_time_spent"])

    @patch("apis.controller.session_controller.views.Session.objects.get")
    def test_20_get_session_stats_missing_session_id(self, mock_session_get):
        """20: Test getting stats without session_id"""
        request = self.factory.get("/session/stats")
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        mock_session_get.assert_not_called()

    @patch("apis.controller.session_controller.views.Session.objects.get")
    def test_21_get_session_stats_not_found(self, mock_session_get):
        """21: Test getting stats for non-existent session"""
        mock_session_get.side_effect = Session.DoesNotExist

        request = self.factory.get("/session/stats", {"session_id": str(uuid.uuid4())})
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)


class TestSessionReviewView(TestCase):
    """Unit tests for SessionReviewView - testing view logic only"""

    def setUp(self):
        """Set up test fixtures"""
        self.factory = APIRequestFactory()
        self.view = SessionReviewView.as_view()

    @patch("apis.controller.session_controller.views.Session.objects.get")
    def test_22_get_session_review_success(self, mock_session_get):
        """22: Test successful session review retrieval"""
        mock_user = Mock()
        mock_user.uid = uuid.uuid4()

        mock_session = Mock()
        mock_session.id = uuid.uuid4()
        mock_session.user = mock_user
        mock_session.started_at = timezone.now() - timedelta(hours=2)  # Fixed: add started_at
        mock_session.created_at = timezone.now() - timedelta(hours=2)
        mock_session.ended_at = timezone.now()
        mock_session.totalPages = 12
        mock_session_get.return_value = mock_session

        request = self.factory.get(
            "/session/review", {"session_id": str(mock_session.id)}
        )
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertIn("started_at", response.data)
        self.assertIn("ended_at", response.data)
        self.assertEqual(response.data["total_pages"], 12)

    @patch("apis.controller.session_controller.views.Session.objects.get")
    def test_23_get_session_review_missing_session_id(self, mock_session_get):
        """23: Test getting review without session_id"""
        request = self.factory.get("/session/review")
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        mock_session_get.assert_not_called()

    @patch("apis.controller.session_controller.views.Session.objects.get")
    def test_24_get_session_review_not_found(self, mock_session_get):
        """24: Test getting review for non-existent session"""
        mock_session_get.side_effect = Session.DoesNotExist

        request = self.factory.get("/session/review", {"session_id": str(uuid.uuid4())})
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)


class TestSessionReloadView(TestCase):
    """Unit tests for SessionReloadView - testing view logic only"""

    def setUp(self):
        """Set up test fixtures"""
        self.factory = APIRequestFactory()
        self.view = SessionReloadView.as_view()

    @patch("apis.controller.session_controller.views.User.objects.get")
    @patch("apis.controller.session_controller.views.Session.objects.filter")
    @patch("apis.controller.session_controller.views.Page.objects.filter")
    def test_25_reload_session_no_pages(
        self, mock_page_filter, mock_session_filter, mock_user_get
    ):
        """25: Test reload session with no pages"""
        mock_user = Mock()
        mock_user.uid = uuid.uuid4()
        mock_user_get.return_value = mock_user

        mock_session = Mock()
        mock_session.id = uuid.uuid4()
        mock_session_filter.return_value.first.return_value = mock_session

        mock_page_filter.return_value.exists.return_value = False

        params = {
            "user_id": str(mock_user.uid),
            "started_at": "2025-11-15T10:00:00Z",
            "page_index": 0,
        }
        request = self.factory.get("/session/reload", params)
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        # View returns None when no pages exist
        self.assertEqual(response.data["page_index"], 0)
        self.assertIn("session_id", response.data)

    @patch("apis.controller.session_controller.views.User.objects.get")
    @patch("apis.controller.session_controller.views.Session.objects.filter")
    @patch("apis.controller.session_controller.views.Page.objects.filter")
    @patch("builtins.open", create=True)
    @patch("base64.b64encode")
    def test_26_reload_session_with_page(
        self,
        mock_b64encode,
        mock_open,
        mock_page_filter,
        mock_session_filter,
        mock_user_get,
    ):
        """26: Test reload session with page data"""
        mock_user = Mock()
        mock_user.uid = uuid.uuid4()
        mock_user_get.return_value = mock_user

        mock_session = Mock()
        mock_session.id = uuid.uuid4()
        mock_session_filter.return_value.first.return_value = mock_session

        mock_page = Mock()
        mock_page.img_url = "/fake/path/image.jpg"
        mock_page.translation_text = "Translated text"
        mock_page.audio_url = "/fake/audio.mp3"

        # Create a mock queryset that behaves like a list
        mock_page_queryset = Mock()
        mock_page_queryset.__len__ = Mock(return_value=1)
        mock_page_queryset.__getitem__ = Mock(return_value=mock_page)

        mock_page_filter.return_value.exists.return_value = True
        mock_page_filter.return_value.order_by.return_value = mock_page_queryset

        mock_file = MagicMock()
        mock_file.read.return_value = b"fake image"
        mock_open.return_value.__enter__.return_value = mock_file
        mock_b64encode.return_value = b"ZmFrZSBpbWFnZQ=="

        params = {
            "user_id": str(mock_user.uid),
            "started_at": "2025-11-15T10:00:00Z",
            "page_index": 0,
        }
        request = self.factory.get("/session/reload", params)
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertIsNotNone(response.data["image_base64"])

    @patch("apis.controller.session_controller.views.User.objects.get")
    def test_27_reload_session_missing_params(self, mock_user_get):
        """27: Test reload without required params"""
        request = self.factory.get("/session/reload", {"user_id": str(uuid.uuid4())})
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    @patch("apis.controller.session_controller.views.User.objects.get")
    def test_28_reload_session_user_not_found(self, mock_user_get):
        """28: Test reload with non-existent user"""
        mock_user_get.side_effect = User.DoesNotExist

        params = {
            "user_id": str(uuid.uuid4()),
            "started_at": "2025-11-15T10:00:00Z",
        }
        request = self.factory.get("/session/reload", params)
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)

    @patch("apis.controller.session_controller.views.User.objects.get")
    @patch("apis.controller.session_controller.views.Session.objects.filter")
    def test_29_reload_session_not_found(self, mock_session_filter, mock_user_get):
        """29: Test reload with non-existent session"""
        mock_user = Mock()
        mock_user_get.return_value = mock_user
        mock_session_filter.return_value.first.return_value = None

        params = {
            "user_id": str(uuid.uuid4()),
            "started_at": "2025-11-15T10:00:00Z",
        }
        request = self.factory.get("/session/reload", params)
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)

    @patch("apis.controller.session_controller.views.User.objects.get")
    def test_30_reload_session_invalid_datetime(self, mock_user_get):
        """30: Test reload with invalid datetime format"""
        mock_user = Mock()
        mock_user_get.return_value = mock_user

        params = {
            "user_id": str(uuid.uuid4()),
            "started_at": "invalid-datetime",
        }
        request = self.factory.get("/session/reload", params)
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    @patch("apis.controller.session_controller.views.User.objects.get")
    @patch("apis.controller.session_controller.views.Session.objects.filter")
    @patch("apis.controller.session_controller.views.Page.objects.filter")
    def test_31_reload_session_negative_page_index(
        self, mock_page_filter, mock_session_filter, mock_user_get
    ):
        """31: Test reload with negative page_index (boundary test)"""
        mock_user = Mock()
        mock_user.uid = uuid.uuid4()
        mock_user_get.return_value = mock_user

        mock_session = Mock()
        mock_session.id = uuid.uuid4()
        mock_session_filter.return_value.first.return_value = mock_session

        mock_page = Mock()
        mock_page.img_url = "/fake/image.jpg"
        mock_page.translation_text = "Text"
        mock_page.audio_url = "/fake/audio.mp3"

        mock_page_queryset = Mock()
        mock_page_queryset.__len__ = Mock(return_value=5)
        mock_page_queryset.__getitem__ = Mock(return_value=mock_page)
        mock_page_queryset.first.return_value = mock_page

        mock_page_filter.return_value.exists.return_value = True
        mock_page_filter.return_value.order_by.return_value = mock_page_queryset

        # Negative page_index should use Python's negative indexing
        params = {
            "user_id": str(mock_user.uid),
            "started_at": "2025-11-15T10:00:00Z",
            "page_index": -1,
        }
        request = self.factory.get("/session/reload", params)

        # Mock file operations
        with patch("builtins.open", create=True) as mock_open:
            with patch("base64.b64encode") as mock_b64encode:
                mock_file = MagicMock()
                mock_file.read.return_value = b"fake image"
                mock_open.return_value.__enter__.return_value = mock_file
                mock_b64encode.return_value = b"ZmFrZSBpbWFnZQ=="

                response = self.view(request)

        # View handles negative indices via Python's list indexing (gets last item)
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        mock_page_queryset.__getitem__.assert_called_with(-1)

    @patch("apis.controller.session_controller.views.User.objects.get")
    @patch("apis.controller.session_controller.views.Session.objects.filter")
    @patch("apis.controller.session_controller.views.Page.objects.filter")
    def test_32_reload_session_large_page_index(
        self, mock_page_filter, mock_session_filter, mock_user_get
    ):
        """32: Test reload with page_index beyond available pages"""
        mock_user = Mock()
        mock_user.uid = uuid.uuid4()
        mock_user_get.return_value = mock_user

        mock_session = Mock()
        mock_session.id = uuid.uuid4()
        mock_session_filter.return_value.first.return_value = mock_session

        mock_page = Mock()
        mock_page.img_url = "/fake/image.jpg"
        mock_page.translation_text = "Text"
        mock_page.audio_url = "/fake/audio.mp3"

        mock_page_queryset = Mock()
        mock_page_queryset.__len__ = Mock(return_value=3)
        mock_page_queryset.__getitem__ = Mock(return_value=mock_page)
        mock_page_queryset.first.return_value = mock_page

        mock_page_filter.return_value.exists.return_value = True
        mock_page_filter.return_value.order_by.return_value = mock_page_queryset

        params = {
            "user_id": str(mock_user.uid),
            "started_at": "2025-11-15T10:00:00Z",
            "page_index": 999,
        }
        request = self.factory.get("/session/reload", params)

        # Mock file operations
        with patch("builtins.open", create=True) as mock_open:
            with patch("base64.b64encode") as mock_b64encode:
                mock_file = MagicMock()
                mock_file.read.return_value = b"fake image"
                mock_open.return_value.__enter__.return_value = mock_file
                mock_b64encode.return_value = b"ZmFrZSBpbWFnZQ=="

                response = self.view(request)

        # Should fallback to first page when index >= len(pages)
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        mock_page_queryset.first.assert_called()

    @patch("apis.controller.session_controller.views.User.objects.get")
    @patch("apis.controller.session_controller.views.Session.objects.filter")
    @patch("apis.controller.session_controller.views.Page.objects.filter")
    def test_33_reload_session_file_not_found(
        self, mock_page_filter, mock_session_filter, mock_user_get
    ):
        """33: Test reload with missing image file (IO error)"""
        mock_user = Mock()
        mock_user.uid = uuid.uuid4()
        mock_user_get.return_value = mock_user

        mock_session = Mock()
        mock_session.id = uuid.uuid4()
        mock_session_filter.return_value.first.return_value = mock_session

        mock_page = Mock()
        mock_page.img_url = "/fake/nonexistent.jpg"
        mock_page.translation_text = "Text"
        mock_page.audio_url = "/fake/audio.mp3"

        mock_page_queryset = Mock()
        mock_page_queryset.__len__ = Mock(return_value=1)
        mock_page_queryset.__getitem__ = Mock(return_value=mock_page)

        mock_page_filter.return_value.exists.return_value = True
        mock_page_filter.return_value.order_by.return_value = mock_page_queryset

        params = {
            "user_id": str(mock_user.uid),
            "started_at": "2025-11-15T10:00:00Z",
            "page_index": 0,
        }
        request = self.factory.get("/session/reload", params)

        # Mock file open to raise FileNotFoundError
        with patch("builtins.open", create=True) as mock_open:
            mock_open.side_effect = FileNotFoundError("File not found")
            response = self.view(request)

        # Should handle gracefully and return None for image_base64
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertIsNone(response.data["image_base64"])

    @patch("apis.controller.session_controller.views.User.objects.get")
    @patch("apis.controller.session_controller.views.Session.objects.filter")
    @patch("apis.controller.session_controller.views.Page.objects.filter")
    def test_34_reload_session_file_permission_error(
        self, mock_page_filter, mock_session_filter, mock_user_get
    ):
        """34: Test reload with file permission error"""
        mock_user = Mock()
        mock_user.uid = uuid.uuid4()
        mock_user_get.return_value = mock_user

        mock_session = Mock()
        mock_session.id = uuid.uuid4()
        mock_session_filter.return_value.first.return_value = mock_session

        mock_page = Mock()
        mock_page.img_url = "/fake/protected.jpg"
        mock_page.translation_text = "Text"
        mock_page.audio_url = "/fake/audio.mp3"

        mock_page_queryset = Mock()
        mock_page_queryset.__len__ = Mock(return_value=1)
        mock_page_queryset.__getitem__ = Mock(return_value=mock_page)

        mock_page_filter.return_value.exists.return_value = True
        mock_page_filter.return_value.order_by.return_value = mock_page_queryset

        params = {
            "user_id": str(mock_user.uid),
            "started_at": "2025-11-15T10:00:00Z",
            "page_index": 0,
        }
        request = self.factory.get("/session/reload", params)

        # Mock file open to raise PermissionError
        with patch("builtins.open", create=True) as mock_open:
            mock_open.side_effect = PermissionError("Permission denied")
            response = self.view(request)

        # Should handle gracefully and return None for image_base64
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertIsNone(response.data["image_base64"])

    @patch("apis.controller.session_controller.views.User.objects.get")
    @patch("apis.controller.session_controller.views.Session.objects.filter")
    @patch("apis.controller.session_controller.views.Page.objects.filter")
    def test_35_reload_session_timezone_aware_datetime(
        self, mock_page_filter, mock_session_filter, mock_user_get
    ):
        """35: Test reload with timezone-aware datetime"""
        mock_user = Mock()
        mock_user.uid = uuid.uuid4()
        mock_user_get.return_value = mock_user

        mock_session = Mock()
        mock_session.id = uuid.uuid4()
        mock_session_filter.return_value.first.return_value = mock_session

        mock_page_filter.return_value.exists.return_value = False

        # Test with explicit UTC timezone
        params = {
            "user_id": str(mock_user.uid),
            "started_at": "2025-11-15T10:00:00+00:00",
            "page_index": 0,
        }
        request = self.factory.get("/session/reload", params)
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_200_OK)

    @patch("apis.controller.session_controller.views.User.objects.get")
    @patch("apis.controller.session_controller.views.Session.objects.filter")
    @patch("apis.controller.session_controller.views.Page.objects.filter")
    def test_36_reload_session_different_timezone(
        self, mock_page_filter, mock_session_filter, mock_user_get
    ):
        """36: Test reload with different timezone offset"""
        mock_user = Mock()
        mock_user.uid = uuid.uuid4()
        mock_user_get.return_value = mock_user

        mock_session = Mock()
        mock_session.id = uuid.uuid4()
        mock_session_filter.return_value.first.return_value = mock_session

        mock_page_filter.return_value.exists.return_value = False

        # Test with KST timezone (+09:00)
        params = {
            "user_id": str(mock_user.uid),
            "started_at": "2025-11-15T19:00:00+09:00",
            "page_index": 0,
        }
        request = self.factory.get("/session/reload", params)
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_200_OK)


class TestSessionReloadAllView(TestCase):
    """Unit tests for SessionReloadAllView - testing view logic only"""

    def setUp(self):
        """Set up test fixtures"""
        self.factory = APIRequestFactory()
        self.view = SessionReloadAllView.as_view()

    @patch("apis.controller.session_controller.views.User.objects.get")
    @patch("apis.controller.session_controller.views.Session.objects.filter")
    def test_37_reload_all_success(self, mock_session_filter, mock_user_get):
        """37: Test successful reload all pages"""
        mock_user = Mock()
        mock_user_get.return_value = mock_user

        mock_page = Mock()
        mock_page.id = 1
        mock_page.img_url = "/fake/image.jpg"
        mock_page.translation_text = "Text"
        mock_page.audio_url = "/fake/audio.mp3"
        mock_page.bbs = Mock()
        mock_page.bbs.all.return_value = []

        mock_session = Mock()
        mock_session.id = uuid.uuid4()
        mock_session.created_at = timezone.now()
        mock_session.pages = Mock()
        mock_session.pages.all.return_value.order_by.return_value = [mock_page]
        mock_session_filter.return_value.first.return_value = mock_session

        params = {
            "user_id": "test-device",
            "started_at": "2025-11-15T10:00:00Z",
        }
        request = self.factory.get("/session/reload-all", params)
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertIn("pages", response.data)

    @patch("apis.controller.session_controller.views.User.objects.get")
    def test_38_reload_all_missing_params(self, mock_user_get):
        """38: Test reload all without required params"""
        request = self.factory.get("/session/reload-all", {"user_id": "test"})
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    @patch("apis.controller.session_controller.views.User.objects.get")
    def test_39_reload_all_user_not_found(self, mock_user_get):
        """39: Test reload all with non-existent user"""
        mock_user_get.side_effect = User.DoesNotExist

        params = {
            "user_id": "test-device",
            "started_at": "2025-11-15T10:00:00Z",
        }
        request = self.factory.get("/session/reload-all", params)
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)

    @patch("apis.controller.session_controller.views.User.objects.get")
    @patch("apis.controller.session_controller.views.Session.objects.filter")
    def test_40_reload_all_session_not_found(self, mock_session_filter, mock_user_get):
        """40: Test reload all with non-existent session"""
        mock_user = Mock()
        mock_user_get.return_value = mock_user
        mock_session_filter.return_value.first.return_value = None

        params = {
            "user_id": "test-device",
            "started_at": "2025-11-15T10:00:00Z",
        }
        request = self.factory.get("/session/reload-all", params)
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)

    @patch("apis.controller.session_controller.views.User.objects.get")
    @patch("apis.controller.session_controller.views.Session.objects.filter")
    def test_41_reload_all_with_large_pages(self, mock_session_filter, mock_user_get):
        """41: Test reload all with large number of pages"""
        mock_user = Mock()
        mock_user_get.return_value = mock_user

        # Create 100 mock pages
        mock_pages = []
        for i in range(100):
            mock_page = Mock()
            mock_page.id = i
            mock_page.img_url = f"/fake/image_{i}.jpg"
            mock_page.translation_text = f"Text {i}"
            mock_page.audio_url = f"/fake/audio_{i}.mp3"
            mock_bb = Mock()
            mock_bb.coordinates = {"x": 0, "y": 0}
            mock_bb.original_text = f"Original {i}"
            mock_bb.translated_text = f"Translated {i}"
            mock_page.bbs = Mock()
            mock_page.bbs.all.return_value = [mock_bb]
            mock_pages.append(mock_page)

        mock_session = Mock()
        mock_session.id = uuid.uuid4()
        mock_session.created_at = timezone.now()
        mock_session.pages = Mock()
        mock_session.pages.all.return_value.order_by.return_value = mock_pages
        mock_session_filter.return_value.first.return_value = mock_session

        params = {
            "user_id": "test-device",
            "started_at": "2025-11-15T10:00:00Z",
        }
        request = self.factory.get("/session/reload-all", params)
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(len(response.data["pages"]), 100)

    @patch("apis.controller.session_controller.views.User.objects.get")
    @patch("apis.controller.session_controller.views.Session.objects.filter")
    def test_42_reload_all_with_bounding_boxes(
        self, mock_session_filter, mock_user_get
    ):
        """42: Test reload all with multiple bounding boxes per page"""
        mock_user = Mock()
        mock_user_get.return_value = mock_user

        # Create mock bounding boxes
        mock_bb1 = Mock()
        mock_bb1.coordinates = {"x": 10, "y": 20}
        mock_bb1.original_text = "Original 1"
        mock_bb1.translated_text = "Translated 1"

        mock_bb2 = Mock()
        mock_bb2.coordinates = {"x": 30, "y": 40}
        mock_bb2.original_text = "Original 2"
        mock_bb2.translated_text = "Translated 2"

        mock_page = Mock()
        mock_page.id = 1
        mock_page.img_url = "/fake/image.jpg"
        mock_page.translation_text = "Text"
        mock_page.audio_url = "/fake/audio.mp3"
        mock_page.bbs = Mock()
        mock_page.bbs.all.return_value = [mock_bb1, mock_bb2]

        mock_session = Mock()
        mock_session.id = uuid.uuid4()
        mock_session.created_at = timezone.now()
        mock_session.pages = Mock()
        mock_session.pages.all.return_value.order_by.return_value = [mock_page]
        mock_session_filter.return_value.first.return_value = mock_session

        params = {
            "user_id": "test-device",
            "started_at": "2025-11-15T10:00:00Z",
        }
        request = self.factory.get("/session/reload-all", params)
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(len(response.data["pages"][0]["ocr_results"]), 2)

    @patch("apis.controller.session_controller.views.User.objects.get")
    @patch("apis.controller.session_controller.views.Session.objects.filter")
    def test_43_reload_all_invalid_datetime(self, mock_session_filter, mock_user_get):
        """43: Test reload all with invalid datetime format"""
        mock_user = Mock()
        mock_user_get.return_value = mock_user

        params = {
            "user_id": "test-device",
            "started_at": "invalid-datetime",
        }
        request = self.factory.get("/session/reload-all", params)
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    @patch("apis.controller.session_controller.views.User.objects.get")
    @patch("apis.controller.session_controller.views.Session.objects.filter")
    def test_44_reload_all_database_error(self, mock_session_filter, mock_user_get):
        """44: Test reload all with database error (500 error handling)"""
        mock_user = Mock()
        mock_user_get.return_value = mock_user

        mock_session = Mock()
        mock_session.pages = Mock()
        mock_session.pages.all.side_effect = Exception("Database error")
        mock_session_filter.return_value.first.return_value = mock_session

        params = {
            "user_id": "test-device",
            "started_at": "2025-11-15T10:00:00Z",
        }
        request = self.factory.get("/session/reload-all", params)
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_500_INTERNAL_SERVER_ERROR)
        self.assertIn("error", response.data)

    @patch("apis.controller.session_controller.views.User.objects.get")
    @patch("apis.controller.session_controller.views.Session.objects.filter")
    def test_45_reload_all_empty_pages(self, mock_session_filter, mock_user_get):
        """45: Test reload all with session containing no pages"""
        mock_user = Mock()
        mock_user_get.return_value = mock_user

        mock_session = Mock()
        mock_session.id = uuid.uuid4()
        mock_session.created_at = timezone.now()
        mock_session.pages = Mock()
        mock_session.pages.all.return_value.order_by.return_value = []
        mock_session_filter.return_value.first.return_value = mock_session

        params = {
            "user_id": "test-device",
            "started_at": "2025-11-15T10:00:00Z",
        }
        request = self.factory.get("/session/reload-all", params)
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(len(response.data["pages"]), 0)

    @patch("apis.controller.session_controller.views.User.objects.get")
    @patch("apis.controller.session_controller.views.Session.objects.filter")
    def test_46_reload_all_timezone_aware(self, mock_session_filter, mock_user_get):
        """46: Test reload all with timezone-aware datetime"""
        mock_user = Mock()
        mock_user_get.return_value = mock_user

        mock_session = Mock()
        mock_session.id = uuid.uuid4()
        mock_session.created_at = timezone.now()
        mock_session.pages = Mock()
        mock_session.pages.all.return_value.order_by.return_value = []
        mock_session_filter.return_value.first.return_value = mock_session

        # Test with explicit timezone
        params = {
            "user_id": "test-device",
            "started_at": "2025-11-15T10:00:00+09:00",
        }
        request = self.factory.get("/session/reload-all", params)
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        # Verify datetime is properly serialized in response
        self.assertIn("started_at", response.data)


class TestDiscardSessionView(TestCase):
    """Unit tests for DiscardSessionView - testing view logic only"""

    def setUp(self):
        """Set up test fixtures"""
        self.factory = APIRequestFactory()
        self.view = DiscardSessionView.as_view()

    @patch("apis.controller.session_controller.views.Session.objects.get")
    @patch("os.path.exists")
    @patch("os.remove")
    def test_47_discard_session_success(
        self, mock_remove, mock_exists, mock_session_get
    ):
        """47: Test successful session discard"""
        mock_page = Mock()
        mock_page.img_url = "/fake/image.jpg"

        mock_session = Mock()
        mock_session.id = uuid.uuid4()
        mock_session.pages = Mock()
        mock_session.pages.all.return_value = [mock_page]
        mock_session.delete = Mock()
        mock_session_get.return_value = mock_session

        mock_exists.return_value = True

        data = {"session_id": str(mock_session.id)}
        request = self.factory.post("/session/discard", data, format="json")
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data["message"], "Session discarded successfully")
        mock_session.delete.assert_called_once()
        mock_remove.assert_called_once_with("/fake/image.jpg")

    @patch("apis.controller.session_controller.views.Session.objects.get")
    def test_48_discard_session_missing_session_id(self, mock_session_get):
        """48: Test discard without session_id"""
        data = {}
        request = self.factory.post("/session/discard", data, format="json")
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        mock_session_get.assert_not_called()

    @patch("apis.controller.session_controller.views.Session.objects.get")
    def test_49_discard_session_not_found(self, mock_session_get):
        """49: Test discard with non-existent session"""
        mock_session_get.side_effect = Session.DoesNotExist

        data = {"session_id": str(uuid.uuid4())}
        request = self.factory.post("/session/discard", data, format="json")
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)

    @patch("apis.controller.session_controller.views.Session.objects.get")
    @patch("os.path.exists")
    def test_50_discard_session_no_images(self, mock_exists, mock_session_get):
        """50: Test discard session with no image files"""
        mock_session = Mock()
        mock_session.id = uuid.uuid4()
        mock_session.pages = Mock()
        mock_session.pages.all.return_value = []
        mock_session.delete = Mock()
        mock_session_get.return_value = mock_session

        data = {"session_id": str(mock_session.id)}
        request = self.factory.post("/session/discard", data, format="json")
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        mock_session.delete.assert_called_once()

    @patch("apis.controller.session_controller.views.Session.objects.get")
    def test_51_discard_session_database_error(self, mock_session_get):
        """51: Test discard with database error (500 error handling)"""
        mock_session = Mock()
        mock_session.pages = Mock()
        mock_session.pages.all.side_effect = Exception("Database error")
        mock_session_get.return_value = mock_session

        data = {"session_id": str(uuid.uuid4())}
        request = self.factory.post("/session/discard", data, format="json")
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_500_INTERNAL_SERVER_ERROR)
        self.assertIn("error", response.data)

    @patch("apis.controller.session_controller.views.Session.objects.get")
    @patch("os.path.exists")
    @patch("os.remove")
    def test_52_discard_session_file_removal_error(
        self, mock_remove, mock_exists, mock_session_get
    ):
        """52: Test discard when file removal fails"""
        mock_page = Mock()
        mock_page.img_url = "/fake/image.jpg"

        mock_session = Mock()
        mock_session.id = uuid.uuid4()
        mock_session.pages = Mock()
        mock_session.pages.all.return_value = [mock_page]
        mock_session.delete = Mock()
        mock_session_get.return_value = mock_session

        mock_exists.return_value = True
        mock_remove.side_effect = OSError("Permission denied")

        data = {"session_id": str(mock_session.id)}
        request = self.factory.post("/session/discard", data, format="json")
        response = self.view(request)

        # Should still succeed even if file removal fails
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        mock_session.delete.assert_called_once()
