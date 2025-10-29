from rest_framework.test import APITestCase, APIClient
from rest_framework import status
from apis.models.user_model import User
from apis.models.session_model import Session
from apis.models.page_model import Page
from apis.models.bb_model import BB
from django.utils import timezone
import json
import os
import base64


class TestPageGetImageView(APITestCase):
    """Unit tests for Page Get Image endpoint"""

    def setUp(self):
        """Set up test client and test data"""
        self.client = APIClient()

        # Create test user and session
        self.test_user = User.objects.create(
            device_info="test-page-device",
            language_preference="en",
            created_at=timezone.now()
        )
        self.test_session = Session.objects.create(
            user=self.test_user,
            title="Test Session",
            created_at=timezone.now()
        )

        # Create a test image file
        self.test_image_path = "media/test_images/test_image.jpg"
        os.makedirs(os.path.dirname(self.test_image_path), exist_ok=True)
        # Write a minimal valid JPEG
        test_image_bytes = base64.b64decode(
            "/9j/4AAQSkZJRgABAQEAYABgAAD/2wBDAAgGBgcGBQgHBwcJCQgKDBQN"
            "DAsLDBkSEw8UHRofHh0aHBwgJC4nICIsIxwcKDcpLDAxNDQ0Hyc5PTgyPC"
            "4zNDL/2wBDAQkJCQwLDBgNDRgyIRwhMjIyMjIyMjIyMjIyMjIyMjIyMjIy"
            "MjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjL/wAARCAABAAEDASIAAh"
            "EBAxEB/8QAFQABAQAAAAAAAAAAAAAAAAAAAAv/xAAUEAEAAAAAAAAAAAAA"
            "AAAAAAAAAP/EABQBAQAAAAAAAAAAAAAAAAAAAAD/xAAUEQEAAAAAAAAAAAAA"
            "AAAAAAAAAP/aAAwDAQACEQMRAD8AP/gB/9k="
        )
        with open(self.test_image_path, "wb") as f:
            f.write(test_image_bytes)

        # Create test page
        self.test_page = Page.objects.create(
            session=self.test_session,
            img_url=self.test_image_path,
            bbox_json=json.dumps([]),
            created_at=timezone.now()
        )

    def tearDown(self):
        """Clean up test files"""
        if os.path.exists(self.test_image_path):
            os.remove(self.test_image_path)
        if os.path.exists("media/test_images"):
            os.rmdir("media/test_images")

    def test_01_get_image_success(self):
        """Test successful image retrieval"""
        response = self.client.get(
            "/page/get_image/",
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
        self.assertIn("image_base64", response.data)
        self.assertIsNotNone(response.data["image_base64"])

    def test_02_get_image_missing_session_id(self):
        """Test image retrieval without session_id"""
        response = self.client.get(
            "/page/get_image/",
            {"page_index": 0}
        )

        self.assertEqual(
            response.status_code, status.HTTP_400_BAD_REQUEST
        )

    def test_03_get_image_missing_page_index(self):
        """Test image retrieval without page_index"""
        response = self.client.get(
            "/page/get_image/",
            {"session_id": str(self.test_session.id)}
        )

        self.assertEqual(
            response.status_code, status.HTTP_400_BAD_REQUEST
        )

    def test_04_get_image_session_not_found(self):
        """Test image retrieval with non-existent session"""
        response = self.client.get(
            "/page/get_image/",
            {
                "session_id": "00000000-0000-0000-0000-000000000000",
                "page_index": 0
            }
        )

        self.assertEqual(
            response.status_code, status.HTTP_404_NOT_FOUND
        )

    def test_05_get_image_page_not_found(self):
        """Test image retrieval with invalid page_index"""
        response = self.client.get(
            "/page/get_image/",
            {
                "session_id": str(self.test_session.id),
                "page_index": 999
            }
        )

        self.assertEqual(
            response.status_code, status.HTTP_404_NOT_FOUND
        )


class TestPageGetOCRView(APITestCase):
    """Unit tests for Page Get OCR endpoint"""

    def setUp(self):
        """Set up test client and test data"""
        self.client = APIClient()

        # Create test user and session
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

        # Create test page
        self.test_page = Page.objects.create(
            session=self.test_session,
            img_url="test.jpg",
            bbox_json=json.dumps([]),
            created_at=timezone.now()
        )

    def test_01_get_ocr_success_with_bbs(self):
        """Test successful OCR retrieval with bounding boxes"""
        # Create test bounding boxes
        BB.objects.create(
            page=self.test_page,
            original_text="Original text 1",
            audio_base64=[],
            translated_text="Translated text 1",
            coordinates={"x": 10, "y": 20, "width": 100, "height": 50}
        )
        BB.objects.create(
            page=self.test_page,
            original_text="Original text 2",
            audio_base64=[],
            translated_text="Translated text 2",
            coordinates={"x": 30, "y": 80, "width": 120, "height": 60}
        )

        response = self.client.get(
            "/page/get_ocr/",
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
        self.assertEqual(len(response.data["ocr_results"]), 2)
        self.assertEqual(
            response.data["ocr_results"][0]["original_txt"],
            "Original text 1"
        )
        self.assertEqual(
            response.data["ocr_results"][0]["translation_txt"],
            "Translated text 1"
        )

    def test_02_get_ocr_success_no_bbs(self):
        """Test successful OCR retrieval with no bounding boxes"""
        response = self.client.get(
            "/page/get_ocr/",
            {
                "session_id": str(self.test_session.id),
                "page_index": 0
            }
        )

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(len(response.data["ocr_results"]), 0)

    def test_03_get_ocr_missing_session_id(self):
        """Test OCR retrieval without session_id"""
        response = self.client.get(
            "/page/get_ocr/",
            {"page_index": 0}
        )

        self.assertEqual(
            response.status_code, status.HTTP_400_BAD_REQUEST
        )

    def test_04_get_ocr_missing_page_index(self):
        """Test OCR retrieval without page_index"""
        response = self.client.get(
            "/page/get_ocr/",
            {"session_id": str(self.test_session.id)}
        )

        self.assertEqual(
            response.status_code, status.HTTP_400_BAD_REQUEST
        )

    def test_05_get_ocr_session_not_found(self):
        """Test OCR retrieval with non-existent session"""
        response = self.client.get(
            "/page/get_ocr/",
            {
                "session_id": "00000000-0000-0000-0000-000000000000",
                "page_index": 0
            }
        )

        self.assertEqual(
            response.status_code, status.HTTP_404_NOT_FOUND
        )

    def test_06_get_ocr_page_not_found(self):
        """Test OCR retrieval with invalid page_index"""
        response = self.client.get(
            "/page/get_ocr/",
            {
                "session_id": str(self.test_session.id),
                "page_index": 999
            }
        )

        self.assertEqual(
            response.status_code, status.HTTP_404_NOT_FOUND
        )


class TestPageGetTTSView(APITestCase):
    """Unit tests for Page Get TTS endpoint"""

    def setUp(self):
        """Set up test client and test data"""
        self.client = APIClient()

        # Create test user and session
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

        # Create test page
        self.test_page = Page.objects.create(
            session=self.test_session,
            img_url="test.jpg",
            bbox_json=json.dumps([]),
            created_at=timezone.now()
        )

    def test_01_get_tts_success_with_audio(self):
        """Test successful TTS retrieval with audio"""
        # Create BBs with audio
        BB.objects.create(
            page=self.test_page,
            original_text="Text 1",
            audio_base64=["audio_clip_1", "audio_clip_2"],
            translated_text="Translated 1",
            coordinates={}
        )
        BB.objects.create(
            page=self.test_page,
            original_text="Text 2",
            audio_base64=["audio_clip_3"],
            translated_text="Translated 2",
            coordinates={}
        )

        response = self.client.get(
            "/page/get_tts/",
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
        self.assertEqual(len(response.data["audio_results"]), 2)
        self.assertEqual(
            response.data["audio_results"][0]["bbox_index"],
            0
        )
        self.assertEqual(
            len(response.data["audio_results"][0]["audio_base64_list"]),
            2
        )

    def test_02_get_tts_success_no_audio(self):
        """Test successful TTS retrieval with no audio"""
        # Create BB without audio
        BB.objects.create(
            page=self.test_page,
            original_text="Text 1",
            audio_base64=[],
            translated_text="Translated 1",
            coordinates={}
        )

        response = self.client.get(
            "/page/get_tts/",
            {
                "session_id": str(self.test_session.id),
                "page_index": 0
            }
        )

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        # BBs without audio should not be included in results
        self.assertEqual(len(response.data["audio_results"]), 0)

    def test_03_get_tts_success_partial_audio(self):
        """Test TTS retrieval with partial audio completion"""
        # Create 3 BBs, only 2 have audio
        BB.objects.create(
            page=self.test_page,
            original_text="Text 1",
            audio_base64=["audio_1"],
            translated_text="Translated 1",
            coordinates={}
        )
        BB.objects.create(
            page=self.test_page,
            original_text="Text 2",
            audio_base64=[],
            translated_text="Translated 2",
            coordinates={}
        )
        BB.objects.create(
            page=self.test_page,
            original_text="Text 3",
            audio_base64=["audio_3"],
            translated_text="Translated 3",
            coordinates={}
        )

        response = self.client.get(
            "/page/get_tts/",
            {
                "session_id": str(self.test_session.id),
                "page_index": 0
            }
        )

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        # Only 2 BBs with audio should be returned
        self.assertEqual(len(response.data["audio_results"]), 2)
        self.assertEqual(
            response.data["audio_results"][0]["bbox_index"],
            0
        )
        self.assertEqual(
            response.data["audio_results"][1]["bbox_index"],
            2
        )

    def test_04_get_tts_missing_session_id(self):
        """Test TTS retrieval without session_id"""
        response = self.client.get(
            "/page/get_tts/",
            {"page_index": 0}
        )

        self.assertEqual(
            response.status_code, status.HTTP_400_BAD_REQUEST
        )

    def test_05_get_tts_missing_page_index(self):
        """Test TTS retrieval without page_index"""
        response = self.client.get(
            "/page/get_tts/",
            {"session_id": str(self.test_session.id)}
        )

        self.assertEqual(
            response.status_code, status.HTTP_400_BAD_REQUEST
        )

    def test_06_get_tts_session_not_found(self):
        """Test TTS retrieval with non-existent session"""
        response = self.client.get(
            "/page/get_tts/",
            {
                "session_id": "00000000-0000-0000-0000-000000000000",
                "page_index": 0
            }
        )

        self.assertEqual(
            response.status_code, status.HTTP_404_NOT_FOUND
        )

    def test_07_get_tts_page_not_found(self):
        """Test TTS retrieval with invalid page_index"""
        response = self.client.get(
            "/page/get_tts/",
            {
                "session_id": str(self.test_session.id),
                "page_index": 999
            }
        )

        self.assertEqual(
            response.status_code, status.HTTP_404_NOT_FOUND
        )
