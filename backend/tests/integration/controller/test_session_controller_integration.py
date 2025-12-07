from django.test import TestCase
from rest_framework.test import APIClient
from rest_framework import status
from apis.models.user_model import User
from apis.models.session_model import Session
from apis.models.page_model import Page
from django.utils import timezone
import tempfile
import os
import shutil


class TestStartSessionViewIntegration(TestCase):
    """Integration tests for Start Session endpoint"""

    def setUp(self):
        """Set up test client and test data"""
        self.client = APIClient()

        # Create test user
        self.test_user = User.objects.create(
            device_info="test-session-device",
            language_preference="en",
            created_at=timezone.now(),
        )

    def test_01_start_session_success(self):
        """Test successful session start"""
        data = {"user_id": "test-session-device", "page_index": 0}
        response = self.client.post("/session/start", data, format="json")

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertIn("session_id", response.data)
        self.assertIn("started_at", response.data)
        self.assertEqual(response.data["page_index"], 0)

        # Verify session was created in database
        session = Session.objects.get(user=self.test_user)
        self.assertTrue(session.isOngoing)
        self.assertEqual(session.totalPages, 0)

    def test_02_start_session_with_custom_page_index(self):
        """Test session start with custom page index"""
        data = {"user_id": "test-session-device", "page_index": 5}
        response = self.client.post("/session/start", data, format="json")

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data["page_index"], 5)

    def test_03_start_session_missing_user_id(self):
        """Test session start with missing user_id"""
        data = {"page_index": 0}
        response = self.client.post("/session/start", data, format="json")

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_04_start_session_user_not_found(self):
        """Test session start with non-existent user"""
        data = {"user_id": "non-existent-user", "page_index": 0}
        response = self.client.post("/session/start", data, format="json")

        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)
        self.assertEqual(response.data["error_code"], 404)
        self.assertEqual(response.data["message"], "USER__NOT_FOUND")


class TestSelectVoiceViewIntegration(TestCase):
    """Integration tests for Select Voice endpoint"""

    def setUp(self):
        """Set up test client and test data"""
        self.client = APIClient()

        # Create test user and session
        self.test_user = User.objects.create(
            device_info="test-voice-device",
            language_preference="en",
            created_at=timezone.now(),
        )
        self.test_session = Session.objects.create(
            user=self.test_user, title="Test Session", created_at=timezone.now()
        )

    def test_01_select_voice_success(self):
        """Test successful voice selection"""
        data = {"session_id": str(self.test_session.id), "voice_style": "male"}
        response = self.client.post("/session/voice", data, format="json")

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(str(response.data["session_id"]), str(self.test_session.id))
        self.assertEqual(response.data["voice_style"], "male")

        # Verify voice preference was updated in database
        self.test_session.refresh_from_db()
        self.assertEqual(self.test_session.voicePreference, "male")

    def test_02_select_voice_different_style(self):
        """Test voice selection with different style"""
        data = {"session_id": str(self.test_session.id), "voice_style": "female"}
        response = self.client.post("/session/voice", data, format="json")

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data["voice_style"], "female")

    def test_03_select_voice_missing_session_id(self):
        """Test voice selection with missing session_id"""
        data = {"voice_style": "male"}
        response = self.client.post("/session/voice", data, format="json")

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_04_select_voice_missing_voice_style(self):
        """Test voice selection with missing voice_style"""
        data = {"session_id": str(self.test_session.id)}
        response = self.client.post("/session/voice", data, format="json")

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_05_select_voice_session_not_found(self):
        """Test voice selection with non-existent session"""
        data = {
            "session_id": "00000000-0000-0000-0000-000000000000",
            "voice_style": "male",
        }
        response = self.client.post("/session/voice", data, format="json")

        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)
        self.assertEqual(response.data["error_code"], 404)
        self.assertEqual(response.data["message"], "SESSION__NOT_FOUND")


class TestEndSessionViewIntegration(TestCase):
    """Integration tests for End Session endpoint"""

    def setUp(self):
        """Set up test client and test data"""
        self.client = APIClient()

        # Create test user and session
        self.test_user = User.objects.create(
            device_info="test-end-device",
            language_preference="en",
            created_at=timezone.now(),
        )
        self.test_session = Session.objects.create(
            user=self.test_user,
            title="Test Session",
            created_at=timezone.now(),
            totalPages=10,
            isOngoing=True,
        )

    def test_01_end_session_success(self):
        """Test successful session end"""
        data = {"session_id": str(self.test_session.id)}
        response = self.client.post("/session/end", data, format="json")

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(str(response.data["session_id"]), str(self.test_session.id))
        self.assertIn("ended_at", response.data)
        self.assertEqual(response.data["total_pages"], 10)

        # Verify session was ended in database
        self.test_session.refresh_from_db()
        self.assertFalse(self.test_session.isOngoing)
        self.assertIsNotNone(self.test_session.ended_at)

    def test_02_end_session_missing_session_id(self):
        """Test session end with missing session_id"""
        data = {}
        response = self.client.post("/session/end", data, format="json")

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_03_end_session_not_found(self):
        """Test session end with non-existent session"""
        data = {"session_id": "00000000-0000-0000-0000-000000000000"}
        response = self.client.post("/session/end", data, format="json")

        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)
        self.assertEqual(response.data["error_code"], 404)
        self.assertEqual(response.data["message"], "SESSION__NOT_FOUND")


class TestGetSessionStatsViewIntegration(TestCase):
    """Integration tests for Get Session Stats endpoint"""

    def setUp(self):
        """Set up test client and test data"""
        self.client = APIClient()

        # Create test user and session
        self.test_user = User.objects.create(
            device_info="test-stats-device",
            language_preference="en",
            created_at=timezone.now(),
        )
        self.test_session = Session.objects.create(
            user=self.test_user,
            title="Test Session",
            created_at=timezone.now(),
            totalPages=8,
        )

    def test_01_get_session_stats_ongoing(self):
        """Test getting stats for ongoing session"""
        response = self.client.get(
            "/session/stats", {"session_id": str(self.test_session.id)}
        )

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(str(response.data["session_id"]), str(self.test_session.id))
        self.assertEqual(str(response.data["user_id"]), str(self.test_user.uid))
        self.assertEqual(response.data["total_pages"], 8)

    def test_02_get_session_stats_ended(self):
        """Test getting stats for ended session with duration"""
        # End the session
        self.test_session.ended_at = timezone.now()
        self.test_session.save()

        response = self.client.get(
            "/session/stats", {"session_id": str(self.test_session.id)}
        )

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertIsNotNone(response.data["ended_at"])
        self.assertIsNotNone(response.data["total_time_spent"])

    def test_03_get_session_stats_missing_session_id(self):
        """Test getting stats without session_id"""
        response = self.client.get("/session/stats")

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_04_get_session_stats_not_found(self):
        """Test getting stats for non-existent session"""
        response = self.client.get(
            "/session/stats", {"session_id": "00000000-0000-0000-0000-000000000000"}
        )

        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)


class TestDiscardSessionViewIntegration(TestCase):
    """Integration tests for Discard Session endpoint"""

    def setUp(self):
        """Set up test client and test data"""
        self.client = APIClient()
        self.temp_dir = tempfile.mkdtemp()

        # Create test user and session
        self.test_user = User.objects.create(
            device_info="test-discard-device",
            language_preference="en",
            created_at=timezone.now(),
        )
        self.test_session = Session.objects.create(
            user=self.test_user,
            title="Test Session",
            created_at=timezone.now(),
        )

    def tearDown(self):
        """Clean up temporary files"""
        if os.path.exists(self.temp_dir):
            shutil.rmtree(self.temp_dir)

    def test_01_discard_session_success(self):
        """Test successful session discard"""
        # Create test image
        test_image_path = os.path.join(self.temp_dir, "test_image.jpg")
        with open(test_image_path, "wb") as f:
            f.write(b"fake image content")

        # Create page with image
        Page.objects.create(
            session=self.test_session,
            img_url=test_image_path,
        )

        data = {"session_id": str(self.test_session.id)}
        response = self.client.post("/session/discard", data, format="json")

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data["message"], "Session discarded successfully")

        # Verify session was deleted
        self.assertFalse(Session.objects.filter(id=self.test_session.id).exists())
        # Verify image file was deleted
        self.assertFalse(os.path.exists(test_image_path))

    def test_02_discard_session_missing_session_id(self):
        """Test discard without session_id"""
        data = {}
        response = self.client.post("/session/discard", data, format="json")

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_03_discard_session_not_found(self):
        """Test discard with non-existent session"""
        data = {"session_id": "00000000-0000-0000-0000-000000000000"}
        response = self.client.post("/session/discard", data, format="json")

        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)
