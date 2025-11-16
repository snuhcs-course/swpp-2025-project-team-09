from django.test import TestCase
from unittest.mock import Mock, patch, MagicMock, AsyncMock
from rest_framework import status
from rest_framework.test import APIRequestFactory
from apis.controller.process_controller.views import (
    ProcessUploadView,
    CheckOCRStatusView,
    CheckTTSStatusView,
    ProcessUploadCoverView,
)
from apis.models.user_model import User
from apis.models.session_model import Session
from apis.models.page_model import Page
from django.utils import timezone
import uuid
import json


class TestProcessUploadView(TestCase):
    """Unit tests for ProcessUploadView - testing view logic only"""

    def setUp(self):
        """Set up test fixtures"""
        self.factory = APIRequestFactory()
        self.view = ProcessUploadView.as_view()
        self.test_image_base64 = (
            "/9j/4AAQSkZJRgABAQEAYABgAAD/2wBDAAgGBgcGBQgHBwcJCQgKDBQN"
            "DAsLDBkSEw8UHRofHh0aHBwgJC4nICIsIxwcKDcpLDAxNDQ0Hyc5PTgyPC"
            "4zNDL/2wBDAQkJCQwLDBgNDRgyIRwhMjIyMjIyMjIyMjIyMjIyMjIyMjIy"
            "MjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjL/wAARCAABAAEDASIAAh"
            "EBAxEB/8QAFQABAQAAAAAAAAAAAAAAAAAAAAv/xAAUEAEAAAAAAAAAAAAA"
            "AAAAAAAAAP/EABQBAQAAAAAAAAAAAAAAAAAAAAD/xAAUEQEAAAAAAAAAAAAA"
            "AAAAAAAAAP/aAAwDAQACEQMRAD8AP/gB/9k="
        )

    @patch("apis.controller.process_controller.views.Session.objects.get")
    @patch("apis.controller.process_controller.views.OCRModule")
    @patch("apis.controller.process_controller.views.TTSModule")
    @patch("apis.controller.process_controller.views.Page.objects.create")
    @patch("apis.controller.process_controller.views.BB.objects.create")
    @patch("os.makedirs")
    @patch("builtins.open", create=True)
    @patch("threading.Thread")
    def test_01_upload_success(
        self,
        mock_thread,
        mock_open,
        mock_makedirs,
        mock_bb_create,
        mock_page_create,
        mock_tts_class,
        mock_ocr_class,
        mock_session_get,
    ):
        """01: Test successful image upload and processing"""
        # Setup session mock
        mock_session = Mock()
        mock_session.id = uuid.uuid4()
        mock_session.totalWords = 0
        mock_session.totalPages = 0
        mock_session.voicePreference = "male"
        mock_session.getPages.return_value.count.return_value = 0
        mock_session_get.return_value = mock_session

        # Mock OCR result
        mock_ocr_instance = Mock()
        mock_ocr_instance.process_page.return_value = [
            {"text": "Test paragraph", "bbox": {"x1": 0, "y1": 0, "x2": 100, "y2": 100}}
        ]
        mock_ocr_class.return_value = mock_ocr_instance

        # Mock TTS result
        mock_tts_instance = Mock()
        mock_tts_instance.get_translations_only = AsyncMock(
            return_value={
                "status": "ok",
                "sentences": [{"translation": "Test translation"}],
            }
        )
        mock_tts_class.return_value = mock_tts_instance

        # Mock page creation
        mock_page = Mock()
        mock_page.getBBs.return_value = []
        mock_page_create.return_value = mock_page

        data = {
            "session_id": str(mock_session.id),
            "lang": "en",
            "image_base64": self.test_image_base64,
        }

        request = self.factory.post("/process/upload/", data, format="json")
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertIn("session_id", response.data)
        self.assertIn("page_index", response.data)
        self.assertEqual(response.data["status"], "ready")

    @patch("apis.controller.process_controller.views.Session.objects.get")
    def test_02_upload_missing_session_id(self, mock_session_get):
        """02: Test upload with missing session_id"""
        data = {"lang": "en", "image_base64": self.test_image_base64}
        request = self.factory.post("/process/upload/", data, format="json")
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        mock_session_get.assert_not_called()

    @patch("apis.controller.process_controller.views.Session.objects.get")
    def test_03_upload_missing_lang(self, mock_session_get):
        """03: Test upload with missing lang"""
        data = {
            "session_id": str(uuid.uuid4()),
            "image_base64": self.test_image_base64,
        }
        request = self.factory.post("/process/upload/", data, format="json")
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        mock_session_get.assert_not_called()

    @patch("apis.controller.process_controller.views.Session.objects.get")
    def test_04_upload_missing_image(self, mock_session_get):
        """04: Test upload with missing image_base64"""
        data = {"session_id": str(uuid.uuid4()), "lang": "en"}
        request = self.factory.post("/process/upload/", data, format="json")
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        mock_session_get.assert_not_called()

    @patch("apis.controller.process_controller.views.Session.objects.get")
    def test_05_upload_session_not_found(self, mock_session_get):
        """05: Test upload with non-existent session"""
        mock_session_get.side_effect = Session.DoesNotExist

        data = {
            "session_id": str(uuid.uuid4()),
            "lang": "en",
            "image_base64": self.test_image_base64,
        }
        request = self.factory.post("/process/upload/", data, format="json")
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)

    @patch("apis.controller.process_controller.views.Session.objects.get")
    @patch("apis.controller.process_controller.views.OCRModule")
    @patch("os.makedirs")
    @patch("builtins.open", create=True)
    def test_06_upload_ocr_failure(
        self, mock_open, mock_makedirs, mock_ocr_class, mock_session_get
    ):
        """06: Test upload when OCR fails to process image"""
        # Setup session mock
        mock_session = Mock()
        mock_session.id = uuid.uuid4()
        mock_session.getPages.return_value.count.return_value = 0
        mock_session_get.return_value = mock_session

        # Mock OCR to return empty result
        mock_ocr_instance = Mock()
        mock_ocr_instance.process_page.return_value = []
        mock_ocr_class.return_value = mock_ocr_instance

        data = {
            "session_id": str(mock_session.id),
            "lang": "en",
            "image_base64": self.test_image_base64,
        }
        request = self.factory.post("/process/upload/", data, format="json")
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_422_UNPROCESSABLE_ENTITY)
        self.assertEqual(response.data["error_code"], 422)
        self.assertEqual(response.data["message"], "PROCESS__UNABLE_TO_PROCESS_IMAGE")

    @patch("apis.controller.process_controller.views.Session.objects.get")
    @patch("apis.controller.process_controller.views.OCRModule")
    @patch("apis.controller.process_controller.views.TTSModule")
    @patch("apis.controller.process_controller.views.Page.objects.create")
    @patch("apis.controller.process_controller.views.BB.objects.create")
    @patch("os.makedirs")
    @patch("builtins.open", create=True)
    @patch("threading.Thread")
    def test_07_upload_with_chinese_lang(
        self,
        mock_thread,
        mock_open,
        mock_makedirs,
        mock_bb_create,
        mock_page_create,
        mock_tts_class,
        mock_ocr_class,
        mock_session_get,
    ):
        """07: Test upload with Chinese language"""
        mock_session = Mock()
        mock_session.id = uuid.uuid4()
        mock_session.totalWords = 0
        mock_session.totalPages = 0
        mock_session.voicePreference = "male"
        mock_session.getPages.return_value.count.return_value = 0
        mock_session_get.return_value = mock_session

        mock_ocr_instance = Mock()
        mock_ocr_instance.process_page.return_value = [
            {"text": "测试文本", "bbox": {"x1": 0, "y1": 0, "x2": 100, "y2": 100}}
        ]
        mock_ocr_class.return_value = mock_ocr_instance

        mock_tts_instance = Mock()
        mock_tts_instance.get_translations_only = AsyncMock(
            return_value={
                "status": "ok",
                "sentences": [{"translation": "Test text"}],
            }
        )
        mock_tts_class.return_value = mock_tts_instance

        mock_page = Mock()
        mock_page.getBBs.return_value = []
        mock_page_create.return_value = mock_page

        data = {
            "session_id": str(mock_session.id),
            "lang": "zh",  # Chinese
            "image_base64": self.test_image_base64,
        }

        request = self.factory.post("/process/upload/", data, format="json")
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        # Verify Chinese language was used
        mock_tts_class.assert_called_with(target_lang="Chinese")

    @patch("apis.controller.process_controller.views.Session.objects.get")
    @patch("apis.controller.process_controller.views.OCRModule")
    @patch("apis.controller.process_controller.views.TTSModule")
    @patch("apis.controller.process_controller.views.Page.objects.create")
    @patch("apis.controller.process_controller.views.BB.objects.create")
    @patch("os.makedirs")
    @patch("builtins.open", create=True)
    @patch("threading.Thread")
    def test_08_upload_with_failed_translation(
        self,
        mock_thread,
        mock_open,
        mock_makedirs,
        mock_bb_create,
        mock_page_create,
        mock_tts_class,
        mock_ocr_class,
        mock_session_get,
    ):
        """08: Test upload when translation fails for a paragraph (covers line 171)"""
        mock_session = Mock()
        mock_session.id = uuid.uuid4()
        mock_session.totalWords = 0
        mock_session.totalPages = 0
        mock_session.voicePreference = "male"
        mock_session.getPages.return_value.count.return_value = 0
        mock_session_get.return_value = mock_session

        # Mock OCR result with 2 paragraphs
        mock_ocr_instance = Mock()
        mock_ocr_instance.process_page.return_value = [
            {"text": "First paragraph", "bbox": {"x1": 0, "y1": 0, "x2": 100, "y2": 100}},
            {"text": "Second paragraph", "bbox": {"x1": 0, "y1": 100, "x2": 100, "y2": 200}},
        ]
        mock_ocr_class.return_value = mock_ocr_instance

        # Mock TTS to return one success and one failure
        async def mock_get_translations(page_data):
            if "0.jpg" in page_data["fileName"]:
                return {
                    "status": "ok",
                    "sentences": [{"translation": "First translation"}],
                }
            else:
                return {"status": "error", "sentences": []}

        mock_tts_instance = Mock()
        mock_tts_instance.get_translations_only = AsyncMock(side_effect=mock_get_translations)
        mock_tts_class.return_value = mock_tts_instance

        # Mock page creation
        mock_page = Mock()
        mock_page.getBBs.return_value = []
        mock_page_create.return_value = mock_page

        data = {
            "session_id": str(mock_session.id),
            "lang": "en",
            "image_base64": self.test_image_base64,
        }

        request = self.factory.post("/process/upload/", data, format="json")
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        # Verify BB was created twice (once with translation, once without)
        self.assertEqual(mock_bb_create.call_count, 2)

    @patch("apis.controller.process_controller.views.Session.objects.get")
    @patch("apis.controller.process_controller.views.OCRModule")
    @patch("apis.controller.process_controller.views.TTSModule")
    @patch("apis.controller.process_controller.views.Page.objects.create")
    @patch("apis.controller.process_controller.views.BB.objects.create")
    @patch("os.makedirs")
    @patch("builtins.open", create=True)
    @patch("threading.Thread")
    def test_09_upload_with_background_tts_audio_update(
        self,
        mock_thread,
        mock_open,
        mock_makedirs,
        mock_bb_create,
        mock_page_create,
        mock_tts_class,
        mock_ocr_class,
        mock_session_get,
    ):
        """09: Test background TTS thread with audio updates (covers lines 227-235)"""
        mock_session = Mock()
        mock_session.id = uuid.uuid4()
        mock_session.totalWords = 0
        mock_session.totalPages = 0
        mock_session.voicePreference = "female"
        mock_session.getPages.return_value.count.return_value = 0
        mock_session_get.return_value = mock_session

        # Mock OCR result
        mock_ocr_instance = Mock()
        mock_ocr_instance.process_page.return_value = [
            {"text": "Test paragraph", "bbox": {"x1": 0, "y1": 0, "x2": 100, "y2": 100}}
        ]
        mock_ocr_class.return_value = mock_ocr_instance

        # Mock TTS translation result
        mock_tts_instance = Mock()
        mock_tts_instance.get_translations_only = AsyncMock(
            return_value={
                "status": "ok",
                "sentences": [{"translation": "Test translation"}],
            }
        )
        # Mock TTS audio generation with actual audio data
        mock_tts_instance.run_tts_only = AsyncMock(
            return_value=["audio_clip_1", "audio_clip_2"]
        )
        mock_tts_class.return_value = mock_tts_instance

        # Create mock BB that can be updated
        mock_bb = Mock()
        mock_bb.id = 123
        mock_bb.audio_base64 = []

        # Mock page with getBBs that returns a list we can access by index
        mock_page = Mock()
        mock_page.getBBs.return_value = [mock_bb]
        mock_page_create.return_value = mock_page

        # Capture the thread target function so we can execute it manually
        thread_target = None
        def capture_thread(target, daemon):
            nonlocal thread_target
            thread_target = target
            return Mock()

        mock_thread.side_effect = capture_thread

        data = {
            "session_id": str(mock_session.id),
            "lang": "en",
            "image_base64": self.test_image_base64,
        }

        request = self.factory.post("/process/upload/", data, format="json")
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_200_OK)

        # Now execute the background thread function
        if thread_target:
            thread_target()

            # Verify that BB audio was updated
            self.assertEqual(mock_bb.audio_base64, ["audio_clip_1", "audio_clip_2"])
            mock_bb.save.assert_called_once()

    @patch("apis.controller.process_controller.views.Session.objects.get")
    @patch("apis.controller.process_controller.views.OCRModule")
    @patch("apis.controller.process_controller.views.TTSModule")
    @patch("apis.controller.process_controller.views.Page.objects.create")
    @patch("apis.controller.process_controller.views.BB.objects.create")
    @patch("os.makedirs")
    @patch("builtins.open", create=True)
    @patch("threading.Thread")
    def test_10_upload_background_tts_skips_failed_translation(
        self,
        mock_thread,
        mock_open,
        mock_makedirs,
        mock_bb_create,
        mock_page_create,
        mock_tts_class,
        mock_ocr_class,
        mock_session_get,
    ):
        """10: Test background TTS skips paragraphs with failed translation (covers line 206)"""
        mock_session = Mock()
        mock_session.id = uuid.uuid4()
        mock_session.totalWords = 0
        mock_session.totalPages = 0
        mock_session.voicePreference = "male"
        mock_session.getPages.return_value.count.return_value = 0
        mock_session_get.return_value = mock_session

        # Mock OCR result with 2 paragraphs
        mock_ocr_instance = Mock()
        mock_ocr_instance.process_page.return_value = [
            {"text": "First paragraph", "bbox": {"x1": 0, "y1": 0, "x2": 100, "y2": 100}},
            {"text": "Second paragraph", "bbox": {"x1": 0, "y1": 100, "x2": 100, "y2": 200}},
        ]
        mock_ocr_class.return_value = mock_ocr_instance

        # Mock TTS translation - first fails, second succeeds
        async def mock_get_translations(page_data):
            if "0.jpg" in page_data["fileName"]:
                return {"status": "error", "sentences": []}  # Failed
            else:
                return {
                    "status": "ok",
                    "sentences": [{"translation": "Second translation"}],
                }

        mock_tts_instance = Mock()
        mock_tts_instance.get_translations_only = AsyncMock(side_effect=mock_get_translations)
        mock_tts_instance.run_tts_only = AsyncMock(
            return_value=["audio_clip"]
        )
        mock_tts_class.return_value = mock_tts_instance

        # Create mock BBs
        mock_bb1 = Mock()
        mock_bb1.id = 101
        mock_bb2 = Mock()
        mock_bb2.id = 102

        # Mock page with getBBs
        mock_page = Mock()
        mock_page.getBBs.return_value = [mock_bb1, mock_bb2]
        mock_page_create.return_value = mock_page

        # Capture the thread target function
        thread_target = None
        def capture_thread(target, daemon):
            nonlocal thread_target
            thread_target = target
            return Mock()

        mock_thread.side_effect = capture_thread

        data = {
            "session_id": str(mock_session.id),
            "lang": "en",
            "image_base64": self.test_image_base64,
        }

        request = self.factory.post("/process/upload/", data, format="json")
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_200_OK)

        # Execute background thread
        if thread_target:
            thread_target()

            # First BB should not be saved (translation failed, skipped with continue)
            mock_bb1.save.assert_not_called()
            # Second BB should be saved
            mock_bb2.save.assert_called_once()

    @patch("apis.controller.process_controller.views.Session.objects.get")
    @patch("apis.controller.process_controller.views.OCRModule")
    @patch("apis.controller.process_controller.views.TTSModule")
    @patch("apis.controller.process_controller.views.Page.objects.create")
    @patch("apis.controller.process_controller.views.BB.objects.create")
    @patch("os.makedirs")
    @patch("builtins.open", create=True)
    @patch("threading.Thread")
    def test_11_upload_background_tts_exception_handling(
        self,
        mock_thread,
        mock_open,
        mock_makedirs,
        mock_bb_create,
        mock_page_create,
        mock_tts_class,
        mock_ocr_class,
        mock_session_get,
    ):
        """11: Test background TTS exception handling (covers lines 237-241)"""
        mock_session = Mock()
        mock_session.id = uuid.uuid4()
        mock_session.totalWords = 0
        mock_session.totalPages = 0
        mock_session.voicePreference = "male"
        mock_session.getPages.return_value.count.return_value = 0
        mock_session_get.return_value = mock_session

        # Mock OCR result
        mock_ocr_instance = Mock()
        mock_ocr_instance.process_page.return_value = [
            {"text": "Test paragraph", "bbox": {"x1": 0, "y1": 0, "x2": 100, "y2": 100}}
        ]
        mock_ocr_class.return_value = mock_ocr_instance

        # Mock TTS translation to succeed
        mock_tts_instance = Mock()
        mock_tts_instance.get_translations_only = AsyncMock(
            return_value={
                "status": "ok",
                "sentences": [{"translation": "Test translation"}],
            }
        )
        # Mock TTS audio generation to raise exception
        mock_tts_instance.run_tts_only = AsyncMock(
            side_effect=Exception("TTS service unavailable")
        )
        mock_tts_class.return_value = mock_tts_instance

        # Mock page
        mock_page = Mock()
        mock_page.getBBs.return_value = []
        mock_page_create.return_value = mock_page

        # Capture the thread target function
        thread_target = None
        def capture_thread(target, daemon):
            nonlocal thread_target
            thread_target = target
            return Mock()

        mock_thread.side_effect = capture_thread

        data = {
            "session_id": str(mock_session.id),
            "lang": "en",
            "image_base64": self.test_image_base64,
        }

        request = self.factory.post("/process/upload/", data, format="json")
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_200_OK)

        # Execute background thread - should handle exception gracefully
        if thread_target:
            # Should not raise exception, should print error message
            thread_target()  # This will trigger the exception handler


class TestCheckOCRStatusView(TestCase):
    """Unit tests for CheckOCRStatusView - testing view logic only"""

    def setUp(self):
        """Set up test fixtures"""
        self.factory = APIRequestFactory()
        self.view = CheckOCRStatusView.as_view()

    @patch("apis.controller.process_controller.views.Session.objects.get")
    @patch("apis.controller.process_controller.views.Page.objects.filter")
    def test_08_check_ocr_status_success(self, mock_page_filter, mock_session_get):
        """08: Test successful OCR status check"""
        mock_session = Mock()
        mock_session.id = uuid.uuid4()
        mock_session_get.return_value = mock_session

        mock_page = Mock()
        mock_page.created_at = timezone.now()

        mock_page_queryset = Mock()
        mock_page_queryset.order_by.return_value.__getitem__ = Mock(
            return_value=mock_page
        )
        mock_page_filter.return_value = mock_page_queryset

        request = self.factory.get(
            "/process/check_ocr/",
            {"session_id": str(mock_session.id), "page_index": 0},
        )
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data["page_index"], 0)
        self.assertEqual(response.data["status"], "ready")
        self.assertEqual(response.data["progress"], 100)

    @patch("apis.controller.process_controller.views.Session.objects.get")
    def test_09_check_ocr_missing_session_id(self, mock_session_get):
        """09: Test OCR status check without session_id"""
        request = self.factory.get("/process/check_ocr/", {"page_index": 0})
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        mock_session_get.assert_not_called()

    @patch("apis.controller.process_controller.views.Session.objects.get")
    def test_10_check_ocr_missing_page_index(self, mock_session_get):
        """10: Test OCR status check without page_index"""
        request = self.factory.get(
            "/process/check_ocr/", {"session_id": str(uuid.uuid4())}
        )
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        mock_session_get.assert_not_called()

    @patch("apis.controller.process_controller.views.Session.objects.get")
    def test_11_check_ocr_session_not_found(self, mock_session_get):
        """11: Test OCR status check with non-existent session"""
        mock_session_get.side_effect = Session.DoesNotExist

        request = self.factory.get(
            "/process/check_ocr/",
            {"session_id": str(uuid.uuid4()), "page_index": 0},
        )
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)

    @patch("apis.controller.process_controller.views.Session.objects.get")
    @patch("apis.controller.process_controller.views.Page.objects.filter")
    def test_12_check_ocr_page_not_found(self, mock_page_filter, mock_session_get):
        """12: Test OCR status check with invalid page_index"""
        mock_session = Mock()
        mock_session_get.return_value = mock_session

        mock_page_queryset = Mock()
        mock_page_queryset.order_by.return_value.__getitem__ = Mock(
            side_effect=IndexError
        )
        mock_page_filter.return_value = mock_page_queryset

        request = self.factory.get(
            "/process/check_ocr/",
            {"session_id": str(uuid.uuid4()), "page_index": 999},
        )
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)


class TestCheckTTSStatusView(TestCase):
    """Unit tests for CheckTTSStatusView - testing view logic only"""

    def setUp(self):
        """Set up test fixtures"""
        self.factory = APIRequestFactory()
        self.view = CheckTTSStatusView.as_view()

    @patch("apis.controller.process_controller.views.Session.objects.get")
    @patch("apis.controller.process_controller.views.Page.objects.filter")
    def test_13_check_tts_status_no_bbs(self, mock_page_filter, mock_session_get):
        """13: Test TTS status check with no bounding boxes"""
        mock_session = Mock()
        mock_session_get.return_value = mock_session

        mock_page = Mock()
        mock_page.created_at = timezone.now()
        mock_page.getBBs.return_value.count.return_value = 0

        mock_page_queryset = Mock()
        mock_page_queryset.order_by.return_value.__getitem__ = Mock(
            return_value=mock_page
        )
        mock_page_filter.return_value = mock_page_queryset

        request = self.factory.get(
            "/process/check_tts/",
            {"session_id": str(uuid.uuid4()), "page_index": 0},
        )
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data["status"], "ready")
        self.assertEqual(response.data["progress"], 100)

    @patch("apis.controller.process_controller.views.Session.objects.get")
    @patch("apis.controller.process_controller.views.Page.objects.filter")
    def test_14_check_tts_status_with_audio(self, mock_page_filter, mock_session_get):
        """14: Test TTS status check with completed audio"""
        mock_session = Mock()
        mock_session_get.return_value = mock_session

        # Create mock BB with audio
        mock_bb = Mock()
        mock_bb.audio_base64 = ["audio_data"]

        # Create mock queryset with count
        mock_bbs = Mock()
        mock_bbs.count.return_value = 1
        mock_bbs.__iter__ = Mock(return_value=iter([mock_bb]))

        mock_page = Mock()
        mock_page.created_at = timezone.now()
        mock_page.getBBs.return_value = mock_bbs

        mock_page_queryset = Mock()
        mock_page_queryset.order_by.return_value.__getitem__ = Mock(
            return_value=mock_page
        )
        mock_page_filter.return_value = mock_page_queryset

        request = self.factory.get(
            "/process/check_tts/",
            {"session_id": str(uuid.uuid4()), "page_index": 0},
        )
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data["status"], "ready")
        self.assertEqual(response.data["progress"], 100)

    @patch("apis.controller.process_controller.views.Session.objects.get")
    @patch("apis.controller.process_controller.views.Page.objects.filter")
    def test_15_check_tts_status_without_audio(
        self, mock_page_filter, mock_session_get
    ):
        """15: Test TTS status check with incomplete audio"""
        mock_session = Mock()
        mock_session_get.return_value = mock_session

        # Create mock BB without audio
        mock_bb = Mock()
        mock_bb.audio_base64 = []

        # Create mock queryset with count
        mock_bbs = Mock()
        mock_bbs.count.return_value = 1
        mock_bbs.__iter__ = Mock(return_value=iter([mock_bb]))

        mock_page = Mock()
        mock_page.created_at = timezone.now()
        mock_page.getBBs.return_value = mock_bbs

        mock_page_queryset = Mock()
        mock_page_queryset.order_by.return_value.__getitem__ = Mock(
            return_value=mock_page
        )
        mock_page_filter.return_value = mock_page_queryset

        request = self.factory.get(
            "/process/check_tts/",
            {"session_id": str(uuid.uuid4()), "page_index": 0},
        )
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data["status"], "processing")
        self.assertEqual(response.data["progress"], 0)

    @patch("apis.controller.process_controller.views.Session.objects.get")
    @patch("apis.controller.process_controller.views.Page.objects.filter")
    def test_16_check_tts_status_partial_complete(
        self, mock_page_filter, mock_session_get
    ):
        """16: Test TTS status check with partial completion"""
        mock_session = Mock()
        mock_session_get.return_value = mock_session

        # Create 2 BBs, one with audio and one without
        mock_bb1 = Mock()
        mock_bb1.audio_base64 = ["audio_data"]

        mock_bb2 = Mock()
        mock_bb2.audio_base64 = []

        # Create mock queryset with count
        mock_bbs = Mock()
        mock_bbs.count.return_value = 2
        mock_bbs.__iter__ = Mock(return_value=iter([mock_bb1, mock_bb2]))

        mock_page = Mock()
        mock_page.created_at = timezone.now()
        mock_page.getBBs.return_value = mock_bbs

        mock_page_queryset = Mock()
        mock_page_queryset.order_by.return_value.__getitem__ = Mock(
            return_value=mock_page
        )
        mock_page_filter.return_value = mock_page_queryset

        request = self.factory.get(
            "/process/check_tts/",
            {"session_id": str(uuid.uuid4()), "page_index": 0},
        )
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data["status"], "processing")
        self.assertEqual(response.data["progress"], 50)

    @patch("apis.controller.process_controller.views.Session.objects.get")
    def test_17_check_tts_missing_session_id(self, mock_session_get):
        """17: Test TTS status check without session_id"""
        request = self.factory.get("/process/check_tts/", {"page_index": 0})
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        mock_session_get.assert_not_called()

    @patch("apis.controller.process_controller.views.Session.objects.get")
    def test_18_check_tts_missing_page_index(self, mock_session_get):
        """18: Test TTS status check without page_index"""
        request = self.factory.get(
            "/process/check_tts/", {"session_id": str(uuid.uuid4())}
        )
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        mock_session_get.assert_not_called()

    @patch("apis.controller.process_controller.views.Session.objects.get")
    def test_19_check_tts_session_not_found(self, mock_session_get):
        """19: Test TTS status check with non-existent session"""
        mock_session_get.side_effect = Session.DoesNotExist

        request = self.factory.get(
            "/process/check_tts/",
            {"session_id": str(uuid.uuid4()), "page_index": 0},
        )
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)

    @patch("apis.controller.process_controller.views.Session.objects.get")
    @patch("apis.controller.process_controller.views.Page.objects.filter")
    def test_20_check_tts_page_not_found(self, mock_page_filter, mock_session_get):
        """20: Test TTS status check with invalid page_index"""
        mock_session = Mock()
        mock_session_get.return_value = mock_session

        mock_page_queryset = Mock()
        mock_page_queryset.order_by.return_value.__getitem__ = Mock(
            side_effect=IndexError
        )
        mock_page_filter.return_value = mock_page_queryset

        request = self.factory.get(
            "/process/check_tts/",
            {"session_id": str(uuid.uuid4()), "page_index": 999},
        )
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)

    @patch("apis.controller.process_controller.views.Session.objects.get")
    @patch("apis.controller.process_controller.views.Page.objects.filter")
    def test_21_check_tts_with_json_audio(self, mock_page_filter, mock_session_get):
        """21: Test TTS status check with JSON string audio (covers _has_audio logic)"""
        mock_session = Mock()
        mock_session_get.return_value = mock_session

        # Create mock BB with audio as JSON string
        mock_bb = Mock()
        mock_bb.audio_base64 = json.dumps(["audio_data"])

        # Create mock queryset with count
        mock_bbs = Mock()
        mock_bbs.count.return_value = 1
        mock_bbs.__iter__ = Mock(return_value=iter([mock_bb]))

        mock_page = Mock()
        mock_page.created_at = timezone.now()
        mock_page.getBBs.return_value = mock_bbs

        mock_page_queryset = Mock()
        mock_page_queryset.order_by.return_value.__getitem__ = Mock(
            return_value=mock_page
        )
        mock_page_filter.return_value = mock_page_queryset

        request = self.factory.get(
            "/process/check_tts/",
            {"session_id": str(uuid.uuid4()), "page_index": 0},
        )
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data["status"], "ready")
        self.assertEqual(response.data["progress"], 100)


class TestProcessUploadCoverView(TestCase):
    """Unit tests for ProcessUploadCoverView - testing view logic only"""

    def setUp(self):
        """Set up test fixtures"""
        self.factory = APIRequestFactory()
        self.view = ProcessUploadCoverView.as_view()
        self.test_image_base64 = (
            "/9j/4AAQSkZJRgABAQEAYABgAAD/2wBDAAgGBgcGBQgHBwcJCQgKDBQN"
            "DAsLDBkSEw8UHRofHh0aHBwgJC4nICIsIxwcKDcpLDAxNDQ0Hyc5PTgyPC"
            "4zNDL/2wBDAQkJCQwLDBgNDRgyIRwhMjIyMjIyMjIyMjIyMjIyMjIyMjIy"
            "MjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjL/wAARCAABAAEDASIAAh"
            "EBAxEB/8QAFQABAQAAAAAAAAAAAAAAAAAAAAv/xAAUEAEAAAAAAAAAAAAA"
            "AAAAAAAAAP/EABQBAQAAAAAAAAAAAAAAAAAAAAD/xAAUEQEAAAAAAAAAAAAA"
            "AAAAAAAAAP/aAAwDAQACEQMRAD8AP/gB/9k="
        )

    @patch("apis.controller.process_controller.views.Session.objects.get")
    @patch("apis.controller.process_controller.views.OCRModule")
    @patch("apis.controller.process_controller.views.TTSModule")
    @patch("apis.controller.process_controller.views.Page.objects.create")
    @patch("apis.controller.process_controller.views.BB.objects.create")
    @patch("os.makedirs")
    @patch("builtins.open", create=True)
    def test_21_upload_cover_success(
        self,
        mock_open,
        mock_makedirs,
        mock_bb_create,
        mock_page_create,
        mock_tts_class,
        mock_ocr_class,
        mock_session_get,
    ):
        """21: Test successful cover upload and processing"""
        # Setup session mock
        mock_session = Mock()
        mock_session.id = uuid.uuid4()
        mock_session.totalPages = 0
        mock_session_get.return_value = mock_session

        # Mock OCR result
        mock_ocr_instance = Mock()
        mock_ocr_instance.process_cover_page.return_value = "Test Book Title"
        mock_ocr_class.return_value = mock_ocr_instance

        # Mock TTS result
        mock_tts_instance = Mock()
        mock_tts_instance.translate_and_tts_cover = AsyncMock(
            return_value=("Translated Title", "male_audio", "female_audio")
        )
        mock_tts_class.return_value = mock_tts_instance

        # Mock page creation
        mock_page = Mock()
        mock_page_create.return_value = mock_page

        data = {
            "session_id": str(mock_session.id),
            "lang": "en",
            "image_base64": self.test_image_base64,
        }

        request = self.factory.post("/process/upload_cover/", data, format="json")
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertIn("title", response.data)
        self.assertIn("translated_title", response.data)
        self.assertEqual(response.data["title"], "Test Book Title")

    @patch("apis.controller.process_controller.views.Session.objects.get")
    def test_22_upload_cover_missing_params(self, mock_session_get):
        """22: Test cover upload with missing parameters"""
        data = {"lang": "en"}
        request = self.factory.post("/process/upload_cover/", data, format="json")
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        mock_session_get.assert_not_called()

    @patch("apis.controller.process_controller.views.Session.objects.get")
    def test_23_upload_cover_session_not_found(self, mock_session_get):
        """23: Test cover upload with non-existent session"""
        mock_session_get.side_effect = Session.DoesNotExist

        data = {
            "session_id": str(uuid.uuid4()),
            "lang": "en",
            "image_base64": self.test_image_base64,
        }
        request = self.factory.post("/process/upload_cover/", data, format="json")
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)

    @patch("apis.controller.process_controller.views.Session.objects.get")
    @patch("apis.controller.process_controller.views.OCRModule")
    @patch("os.makedirs")
    @patch("builtins.open", create=True)
    def test_24_upload_cover_ocr_failure(
        self, mock_open, mock_makedirs, mock_ocr_class, mock_session_get
    ):
        """24: Test cover upload when OCR fails"""
        mock_session = Mock()
        mock_session.id = uuid.uuid4()
        mock_session_get.return_value = mock_session

        # Mock OCR to return empty result
        mock_ocr_instance = Mock()
        mock_ocr_instance.process_cover_page.return_value = None
        mock_ocr_class.return_value = mock_ocr_instance

        data = {
            "session_id": str(mock_session.id),
            "lang": "en",
            "image_base64": self.test_image_base64,
        }
        request = self.factory.post("/process/upload_cover/", data, format="json")
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_422_UNPROCESSABLE_ENTITY)
        self.assertEqual(response.data["error_code"], 422)
        self.assertEqual(response.data["message"], "PROCESS__UNABLE_TO_PROCESS_IMAGE")

    @patch("apis.controller.process_controller.views.Session.objects.get")
    @patch("apis.controller.process_controller.views.OCRModule")
    @patch("apis.controller.process_controller.views.TTSModule")
    @patch("apis.controller.process_controller.views.Page.objects.create")
    @patch("apis.controller.process_controller.views.BB.objects.create")
    @patch("os.makedirs")
    @patch("builtins.open", create=True)
    def test_25_upload_cover_translation_failure(
        self,
        mock_open,
        mock_makedirs,
        mock_bb_create,
        mock_page_create,
        mock_tts_class,
        mock_ocr_class,
        mock_session_get,
    ):
        """25: Test cover upload when translation fails (covers line 483)"""
        mock_session = Mock()
        mock_session.id = uuid.uuid4()
        mock_session.totalPages = 0
        mock_session_get.return_value = mock_session

        # Mock OCR result
        mock_ocr_instance = Mock()
        mock_ocr_instance.process_cover_page.return_value = "Test Book Title"
        mock_ocr_class.return_value = mock_ocr_instance

        # Mock TTS to return translation with failed status
        mock_tts_instance = Mock()
        mock_tts_instance.translate_and_tts_cover = AsyncMock(
            return_value=("", None, None)
        )
        mock_tts_class.return_value = mock_tts_instance

        # Mock page creation
        mock_page = Mock()
        mock_page_create.return_value = mock_page

        data = {
            "session_id": str(mock_session.id),
            "lang": "en",
            "image_base64": self.test_image_base64,
        }

        request = self.factory.post("/process/upload_cover/", data, format="json")
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        # BB should be created with empty translated_text
        mock_bb_create.assert_called_once()

    @patch("apis.controller.process_controller.views.Session.objects.get")
    @patch("apis.controller.process_controller.views.OCRModule")
    @patch("apis.controller.process_controller.views.TTSModule")
    @patch("apis.controller.process_controller.views.Page.objects.create")
    @patch("apis.controller.process_controller.views.BB.objects.create")
    @patch("os.makedirs")
    @patch("builtins.open", create=True)
    def test_26_upload_cover_exception_in_run_async(
        self,
        mock_open,
        mock_makedirs,
        mock_bb_create,
        mock_page_create,
        mock_tts_class,
        mock_ocr_class,
        mock_session_get,
    ):
        """26: Test cover upload when _run_async raises exception (covers lines 516-520)"""
        mock_session = Mock()
        mock_session.id = uuid.uuid4()
        mock_session.totalPages = 0
        mock_session_get.return_value = mock_session

        # Mock OCR result
        mock_ocr_instance = Mock()
        mock_ocr_instance.process_cover_page.return_value = "Test Book Title"
        mock_ocr_class.return_value = mock_ocr_instance

        # Mock TTS to raise exception
        mock_tts_instance = Mock()
        mock_tts_instance.translate_and_tts_cover = AsyncMock(
            side_effect=Exception("Translation service error")
        )
        mock_tts_class.return_value = mock_tts_instance

        # Mock page creation
        mock_page = Mock()
        mock_page_create.return_value = mock_page

        data = {
            "session_id": str(mock_session.id),
            "lang": "en",
            "image_base64": self.test_image_base64,
        }

        request = self.factory.post("/process/upload_cover/", data, format="json")
        response = self.view(request)

        # Should still return 200 with empty translation (exception handler returns ("", "", ""))
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data["translated_title"], "")
        # The exception handler returns ("", "", "") so both tts fields will be empty
        self.assertEqual(response.data["tts_male"], "")
        self.assertEqual(response.data["tts_female"], "")

    @patch("apis.controller.process_controller.views.TTSModule")
    def test_27_cover_get_all_translations_method(self, mock_tts_class):
        """27: Test _get_all_translations method directly (covers lines 443-460)"""
        # Create a real view instance
        view_instance = ProcessUploadCoverView()

        # Mock TTS instance
        mock_tts_instance = Mock()
        mock_tts_instance.get_translations_only = AsyncMock(
            return_value={
                "status": "ok",
                "sentences": [{"translation": "Test translation"}],
            }
        )

        # Create OCR result with multiple paragraphs
        ocr_result = [
            {"text": "First paragraph"},
            {"text": "Second paragraph"},
        ]

        # Call _get_all_translations directly
        result = view_instance._get_all_translations(
            mock_tts_instance, ocr_result, "test_session_id", 0
        )

        # Verify results
        self.assertEqual(len(result), 2)
        self.assertEqual(result[0]["status"], "ok")
        self.assertEqual(result[1]["status"], "ok")

        # Verify get_translations_only was called for each paragraph
        self.assertEqual(mock_tts_instance.get_translations_only.call_count, 2)

    @patch("apis.controller.process_controller.views.Session.objects.get")
    @patch("apis.controller.process_controller.views.OCRModule")
    @patch("apis.controller.process_controller.views.TTSModule")
    @patch("apis.controller.process_controller.views.Page.objects.create")
    @patch("apis.controller.process_controller.views.BB.objects.create")
    @patch("os.makedirs")
    @patch("builtins.open", create=True)
    def test_28_upload_cover_with_empty_translation(
        self,
        mock_open,
        mock_makedirs,
        mock_bb_create,
        mock_page_create,
        mock_tts_class,
        mock_ocr_class,
        mock_session_get,
    ):
        """28: Test cover upload with empty translation (covers line 483)"""
        mock_session = Mock()
        mock_session.id = uuid.uuid4()
        mock_session.totalPages = 0
        mock_session_get.return_value = mock_session

        # Mock OCR result
        mock_ocr_instance = Mock()
        mock_ocr_instance.process_cover_page.return_value = "Test Book Title"
        mock_ocr_class.return_value = mock_ocr_instance

        # Mock TTS to return empty translation (failed translation case)
        mock_tts_instance = Mock()
        mock_tts_instance.translate_and_tts_cover = AsyncMock(
            return_value=("", None, None)
        )
        mock_tts_class.return_value = mock_tts_instance

        # Mock page creation
        mock_page = Mock()
        mock_page_create.return_value = mock_page

        # Create a real view instance to access _create_page_and_bbs
        view_instance = ProcessUploadCoverView()

        # Call _create_page_and_bbs with error scenario (status != "ok")
        result = view_instance._create_page_and_bbs(
            mock_session,
            "test_path.jpg",
            [{"text": "Test Book Title"}],
            [{"status": "error", "sentences": []}],  # Error status triggers line 483
        )

        # Verify BB was created with empty translated_text
        mock_bb_create.assert_called_once()
        call_args = mock_bb_create.call_args
        self.assertEqual(call_args[1]["translated_text"], "")
        self.assertEqual(call_args[1]["original_text"], "Test Book Title")
