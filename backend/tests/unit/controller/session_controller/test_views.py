from django.test import TestCase, Client
from django.utils import timezone
from apis.models.user_model import User
from apis.models.session_model import Session
import uuid

class TestSessionController(TestCase):
    """Unit tests for Session Controller views"""

    def setUp(self):
        self.client = Client()
        self.user = User.objects.create(
            device_info="test-user",
            language_preference="en",
            created_at=timezone.now()
        )

    def test_01_start_session_success(self):
        data = {
            "user_id": "test-user",
            "page_index": 0
        }
        response = self.client.post("/session/start", data, content_type="application/json")
        self.assertEqual(response.status_code, 200)
        self.assertIn("session_id", response.json())
        self.assertIn("started_at", response.json())
        self.assertEqual(response.json()["page_index"], 0)

        session_id = response.json()["session_id"]
        self.assertTrue(Session.objects.filter(id=session_id).exists())

    def test_02_start_session_missing_user_id(self):
        data = {"page_index": 0}
        response = self.client.post("/session/start", data, content_type="application/json")
        self.assertEqual(response.status_code, 400)

    def test_03_start_session_user_not_found(self):
        data = {"user_id": "nonexistent", "page_index": 0}
        response = self.client.post("/session/start", data, content_type="application/json")
        self.assertEqual(response.status_code, 404)
        self.assertEqual(response.json()["message"], "USER__NOT_FOUND")

    def test_04_select_voice_success(self):
        session = Session.objects.create(
            user=self.user,
            title="Voice Test",
            created_at=timezone.now(),
            isOngoing=True
        )
        data = {"session_id": str(session.id), "voice_style": "female"}
        response = self.client.post("/session/voice", data, content_type="application/json")
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()["voice_style"], "female")

        session.refresh_from_db()
        self.assertEqual(session.voicePreference, "female")

    def test_05_select_voice_missing_fields(self):
        response = self.client.post("/session/voice", {}, content_type="application/json")
        self.assertEqual(response.status_code, 400)

    def test_06_select_voice_not_found(self):
        data = {"session_id": str(uuid.uuid4()), "voice_style": "male"}
        response = self.client.post("/session/voice", data, content_type="application/json")
        self.assertEqual(response.status_code, 404)
        self.assertEqual(response.json()["message"], "SESSION__NOT_FOUND")

    def test_07_end_session_success(self):
        session = Session.objects.create(
            user=self.user,
            title="End Test",
            created_at=timezone.now(),
            totalPages=5,
            isOngoing=True
        )
        data = {"session_id": str(session.id)}
        response = self.client.post("/session/end", data, content_type="application/json")
        self.assertEqual(response.status_code, 200)
        self.assertIn("ended_at", response.json())
        self.assertEqual(response.json()["total_pages"], 5)

        session.refresh_from_db()
        self.assertFalse(session.isOngoing)
        self.assertIsNotNone(session.ended_at)

    def test_08_end_session_missing_id(self):
        response = self.client.post("/session/end", {}, content_type="application/json")
        self.assertEqual(response.status_code, 400)

    def test_09_end_session_not_found(self):
        data = {"session_id": str(uuid.uuid4())}
        response = self.client.post("/session/end", data, content_type="application/json")
        self.assertEqual(response.status_code, 404)
        self.assertEqual(response.json()["message"], "SESSION__NOT_FOUND")

    def test_10_get_session_info_success(self):
        session = Session.objects.create(
            user=self.user,
            title="Info Test",
            created_at=timezone.now(),
            totalPages=3,
            voicePreference="neutral",
            isOngoing=False
        )
        response = self.client.get("/session/info", {"session_id": str(session.id)})
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()["voice_style"], "neutral")
        self.assertEqual(response.json()["total_pages"], 3)

    def test_11_get_session_info_missing_param(self):
        response = self.client.get("/session/info")
        self.assertEqual(response.status_code, 400)

    def test_12_get_session_info_not_found(self):
        response = self.client.get("/session/info", {"session_id": str(uuid.uuid4())})
        self.assertEqual(response.status_code, 404)
        self.assertEqual(response.json()["message"], "SESSION__NOT_FOUND")

    def test_13_get_session_stats_success(self):
        session = Session.objects.create(
            user=self.user,
            title="Stats Test",
            created_at=timezone.now(),
            totalPages=4,
            isOngoing=False
        )
        response = self.client.get("/session/stats", {"session_id": str(session.id)})
        self.assertEqual(response.status_code, 200)
        self.assertIn("total_words_read", response.json())
        self.assertEqual(response.json()["total_words_read"], 400)

    def test_14_get_session_stats_missing_param(self):
        response = self.client.get("/session/stats")
        self.assertEqual(response.status_code, 400)

    def test_15_get_session_stats_not_found(self):
        response = self.client.get("/session/stats", {"session_id": str(uuid.uuid4())})
        self.assertEqual(response.status_code, 404)

    def test_16_get_session_review_success(self):
        session = Session.objects.create(
            user=self.user,
            title="Review Test",
            created_at=timezone.now(),
            totalPages=10,
            isOngoing=False
        )
        response = self.client.get("/session/review", {"session_id": str(session.id)})
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()["total_pages"], 10)

    def test_17_get_session_review_missing_param(self):
        response = self.client.get("/session/review")
        self.assertEqual(response.status_code, 400)

    def test_18_get_session_review_not_found(self):
        response = self.client.get("/session/review", {"session_id": str(uuid.uuid4())})
        self.assertEqual(response.status_code, 404)
