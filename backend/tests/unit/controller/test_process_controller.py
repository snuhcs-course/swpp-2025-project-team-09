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
