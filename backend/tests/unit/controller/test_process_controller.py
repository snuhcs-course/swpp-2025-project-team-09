from rest_framework.test import APITestCase, APIClient
from rest_framework import status
from apis.models.user_model import User
from apis.models.session_model import Session
from apis.models.page_model import Page
from apis.models.bb_model import BB
from django.utils import timezone
from unittest.mock import patch, MagicMock, AsyncMock
import json


class TestProcessUploadView(APITestCase):
    """Unit tests for Process Upload endpoint"""

    def setUp(self):
        """Set up test client and test data"""
        self.client = APIClient()

        # Create test user and session
        self.test_user = User.objects.create(
            device_info="test-process-device",
            language_preference="en",
            created_at=timezone.now()
        )
        self.test_session = Session.objects.create(
            user=self.test_user,
            title="Test Session",
            created_at=timezone.now()
        )

        # Simple test image base64 (minimal JPEG)
        self.test_image_base64 = (
            "/9j/4AAQSkZJRgABAQEAYABgAAD/2wBDAAgGBgcGBQgHBwcJCQgKDBQN"
            "DAsLDBkSEw8UHRofHh0aHBwgJC4nICIsIxwcKDcpLDAxNDQ0Hyc5PTgyPC"
            "4zNDL/2wBDAQkJCQwLDBgNDRgyIRwhMjIyMjIyMjIyMjIyMjIyMjIyMjIy"
            "MjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjL/wAARCAABAAEDASIAAh"
            "EBAxEB/8QAFQABAQAAAAAAAAAAAAAAAAAAAAv/xAAUEAEAAAAAAAAAAAAA"
            "AAAAAAAAAP/EABQBAQAAAAAAAAAAAAAAAAAAAAD/xAAUEQEAAAAAAAAAAAAA"
            "AAAAAAAAAP/aAAwDAQACEQMRAD8AP/gB/9k="
        )

    @patch('apis.controller.process_controller.views.OCRModule')
    @patch('apis.controller.process_controller.views.TTSModule')
    def test_01_upload_success(self, mock_tts_class, mock_ocr_class):
        """Test successful image upload and processing"""
        # Mock OCR result
        mock_ocr_instance = MagicMock()
        mock_ocr_instance.process_page.return_value = [
            {
                "text": "Test paragraph",
                "bbox": {"x1": 0, "y1": 0, "x2": 100, "y2": 100}
            }
        ]
        mock_ocr_class.return_value = mock_ocr_instance

        # Mock TTS result (비동기 함수로 설정!)
        mock_tts_instance = MagicMock()
        mock_tts_instance.get_translations_only = AsyncMock(return_value={
            "status": "ok",
            "sentences": [
                {
                    "translation": "Test translation",
                    "tone": "neutral",
                    "emotion": "calm",
                    "pacing": "normal",
                    "korean": "Test paragraph"
                }
            ]
        })
        mock_tts_class.return_value = mock_tts_instance

        data = {
            "session_id": str(self.test_session.id),
            "lang": "en",
            "image_base64": self.test_image_base64
        }

        response = self.client.post("/process/upload/", data, format='json')

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertIn("session_id", response.data)
        self.assertIn("page_index", response.data)
        self.assertEqual(response.data["status"], "ready")

        # Verify session was updated
        self.test_session.refresh_from_db()
        self.assertEqual(self.test_session.totalPages, 1)

    def test_02_upload_missing_session_id(self):
        """Test upload with missing session_id"""
        data = {
            "lang": "en",
            "image_base64": self.test_image_base64
        }
        response = self.client.post(
            "/process/upload/", data, format='json'
        )

        self.assertEqual(
            response.status_code, status.HTTP_400_BAD_REQUEST
        )

    def test_03_upload_missing_lang(self):
        """Test upload with missing lang"""
        data = {
            "session_id": str(self.test_session.id),
            "image_base64": self.test_image_base64
        }
        response = self.client.post(
            "/process/upload/", data, format='json'
        )

        self.assertEqual(
            response.status_code, status.HTTP_400_BAD_REQUEST
        )

    def test_04_upload_missing_image(self):
        """Test upload with missing image_base64"""
        data = {
            "session_id": str(self.test_session.id),
            "lang": "en"
        }
        response = self.client.post(
            "/process/upload/", data, format='json'
        )

        self.assertEqual(
            response.status_code, status.HTTP_400_BAD_REQUEST
        )

    def test_05_upload_session_not_found(self):
        """Test upload with non-existent session"""
        data = {
            "session_id": "00000000-0000-0000-0000-000000000000",
            "lang": "en",
            "image_base64": self.test_image_base64
        }
        response = self.client.post(
            "/process/upload/", data, format='json'
        )

        self.assertEqual(
            response.status_code, status.HTTP_404_NOT_FOUND
        )

    @patch('apis.controller.process_controller.views.OCRModule')
    def test_06_upload_ocr_failure(self, mock_ocr_class):
        """Test upload when OCR fails to process image"""
        # Mock OCR to return empty result
        mock_ocr_instance = MagicMock()
        mock_ocr_instance.process_page.return_value = []
        mock_ocr_class.return_value = mock_ocr_instance

        data = {
            "session_id": str(self.test_session.id),
            "lang": "en",
            "image_base64": self.test_image_base64
        }
        response = self.client.post(
            "/process/upload/", data, format='json'
        )

        self.assertEqual(
            response.status_code,
            status.HTTP_422_UNPROCESSABLE_ENTITY
        )
        self.assertEqual(response.data["error_code"], 422)
        self.assertEqual(
            response.data["message"],
            "PROCESS__UNABLE_TO_PROCESS_IMAGE"
        )


class TestCheckOCRStatusView(APITestCase):
    """Unit tests for Check OCR Status endpoint"""

    def setUp(self):
        """Set up test client and test data"""
        self.client = APIClient()

        # Create test user, session, and page
        self.test_user = User.objects.create(
            device_info="test-ocr-device",
            language_preference="en",
            created_at=timezone.now()
        )
        self.test_session = Session.objects.create(
            user=self.test_user,
            title="Test Session",
            created_at=timezone.now()
        )
        self.test_page = Page.objects.create(
            session=self.test_session,
            img_url="test.jpg",
            bbox_json=json.dumps([]),
            created_at=timezone.now()
        )

    def test_01_check_ocr_status_success(self):
        """Test successful OCR status check"""
        response = self.client.get(
            "/process/check_ocr/",
            {
                "session_id": str(self.test_session.id),
                "page_index": 0
            }
        )

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(
            str(response.data["session_id"]),
            str(self.test_session.id)
        )
        self.assertEqual(response.data["page_index"], 0)
        self.assertEqual(response.data["status"], "ready")
        self.assertEqual(response.data["progress"], 100)

    def test_02_check_ocr_missing_session_id(self):
        """Test OCR status check without session_id"""
        response = self.client.get(
            "/process/check_ocr/",
            {"page_index": 0}
        )

        self.assertEqual(
            response.status_code, status.HTTP_400_BAD_REQUEST
        )

    def test_03_check_ocr_missing_page_index(self):
        """Test OCR status check without page_index"""
        response = self.client.get(
            "/process/check_ocr/",
            {"session_id": str(self.test_session.id)}
        )

        self.assertEqual(
            response.status_code, status.HTTP_400_BAD_REQUEST
        )

    def test_04_check_ocr_session_not_found(self):
        """Test OCR status check with non-existent session"""
        response = self.client.get(
            "/process/check_ocr/",
            {
                "session_id": "00000000-0000-0000-0000-000000000000",
                "page_index": 0
            }
        )

        self.assertEqual(
            response.status_code, status.HTTP_404_NOT_FOUND
        )

    def test_05_check_ocr_page_not_found(self):
        """Test OCR status check with invalid page_index"""
        response = self.client.get(
            "/process/check_ocr/",
            {
                "session_id": str(self.test_session.id),
                "page_index": 999
            }
        )

        self.assertEqual(
            response.status_code, status.HTTP_404_NOT_FOUND
        )


class TestCheckTTSStatusView(APITestCase):
    """Unit tests for Check TTS Status endpoint"""

    def setUp(self):
        """Set up test client and test data"""
        self.client = APIClient()

        # Create test user, session, and page
        self.test_user = User.objects.create(
            device_info="test-tts-device",
            language_preference="en",
            created_at=timezone.now()
        )
        self.test_session = Session.objects.create(
            user=self.test_user,
            title="Test Session",
            created_at=timezone.now()
        )
        self.test_page = Page.objects.create(
            session=self.test_session,
            img_url="test.jpg",
            bbox_json=json.dumps([]),
            created_at=timezone.now()
        )

    def test_01_check_tts_status_no_bbs(self):
        """Test TTS status check with no bounding boxes"""
        response = self.client.get(
            "/process/check_tts/",
            {
                "session_id": str(self.test_session.id),
                "page_index": 0
            }
        )

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data["status"], "ready")
        self.assertEqual(response.data["progress"], 100)

    def test_02_check_tts_status_with_audio(self):
        """Test TTS status check with completed audio"""
        # Create BB with audio
        BB.objects.create(
            page=self.test_page,
            original_text="Test text",
            audio_base64=["base64_audio_data"],
            translated_text="Translated text",
            coordinates={}
        )

        response = self.client.get(
            "/process/check_tts/",
            {
                "session_id": str(self.test_session.id),
                "page_index": 0
            }
        )

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data["status"], "ready")
        self.assertEqual(response.data["progress"], 100)

    def test_03_check_tts_status_without_audio(self):
        """Test TTS status check with incomplete audio"""
        # Create BB without audio
        BB.objects.create(
            page=self.test_page,
            original_text="Test text",
            audio_base64=[],
            translated_text="Translated text",
            coordinates={}
        )

        response = self.client.get(
            "/process/check_tts/",
            {
                "session_id": str(self.test_session.id),
                "page_index": 0
            }
        )

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data["status"], "processing")
        self.assertEqual(response.data["progress"], 0)

    def test_04_check_tts_status_partial_complete(self):
        """Test TTS status check with partial completion"""
        # Create 2 BBs, one with audio and one without
        BB.objects.create(
            page=self.test_page,
            original_text="Test text 1",
            audio_base64=["base64_audio_data"],
            translated_text="Translated text 1",
            coordinates={}
        )
        BB.objects.create(
            page=self.test_page,
            original_text="Test text 2",
            audio_base64=[],
            translated_text="Translated text 2",
            coordinates={}
        )

        response = self.client.get(
            "/process/check_tts/",
            {
                "session_id": str(self.test_session.id),
                "page_index": 0
            }
        )

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data["status"], "processing")
        self.assertEqual(response.data["progress"], 50)

    def test_05_check_tts_missing_session_id(self):
        """Test TTS status check without session_id"""
        response = self.client.get(
            "/process/check_tts/",
            {"page_index": 0}
        )

        self.assertEqual(
            response.status_code, status.HTTP_400_BAD_REQUEST
        )

    def test_06_check_tts_missing_page_index(self):
        """Test TTS status check without page_index"""
        response = self.client.get(
            "/process/check_tts/",
            {"session_id": str(self.test_session.id)}
        )

        self.assertEqual(
            response.status_code, status.HTTP_400_BAD_REQUEST
        )

    def test_07_check_tts_session_not_found(self):
        """Test TTS status check with non-existent session"""
        response = self.client.get(
            "/process/check_tts/",
            {
                "session_id": "00000000-0000-0000-0000-000000000000",
                "page_index": 0
            }
        )

        self.assertEqual(
            response.status_code, status.HTTP_404_NOT_FOUND
        )

    def test_08_check_tts_page_not_found(self):
        """Test TTS status check with invalid page_index"""
        response = self.client.get(
            "/process/check_tts/",
            {
                "session_id": str(self.test_session.id),
                "page_index": 999
            }
        )

        self.assertEqual(
            response.status_code, status.HTTP_404_NOT_FOUND
        )
