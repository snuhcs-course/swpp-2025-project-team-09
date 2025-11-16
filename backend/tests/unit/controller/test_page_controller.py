from django.test import TestCase
from unittest.mock import Mock, patch, MagicMock
from rest_framework import status
from rest_framework.test import APIRequestFactory
from apis.controller.page_controller.views import (
    PageGetImageView,
    PageGetOCRView,
    PageGetTTSView,
)
from apis.models.page_model import Page
from django.utils import timezone
import uuid
import json


class TestPageGetImageView(TestCase):
    """Unit tests for PageGetImageView - testing view logic only"""

    def setUp(self):
        """Set up test fixtures"""
        self.factory = APIRequestFactory()
        self.view = PageGetImageView.as_view()

    @patch("apis.controller.page_controller.views.Page.objects.filter")
    @patch("os.path.exists")
    @patch("builtins.open", create=True)
    @patch("base64.b64encode")
    def test_01_get_image_success(
        self, mock_b64encode, mock_open, mock_exists, mock_page_filter
    ):
        """01: Test successful image retrieval"""
        # Setup mocks
        mock_page = Mock()
        mock_page.img_url = "/fake/path/image.jpg"
        mock_page.created_at = timezone.now()

        mock_page_queryset = Mock()
        mock_page_queryset.order_by.return_value.__getitem__ = Mock(
            return_value=mock_page
        )
        mock_page_filter.return_value = mock_page_queryset

        mock_exists.return_value = True

        mock_file = MagicMock()
        mock_file.read.return_value = b"fake image content"
        mock_open.return_value.__enter__.return_value = mock_file

        mock_b64encode.return_value = b"ZmFrZSBpbWFnZSBjb250ZW50"

        request = self.factory.get(
            "/page/get_image/",
            {"session_id": str(uuid.uuid4()), "page_index": 0},
        )
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data["page_index"], 0)
        self.assertIn("image_base64", response.data)
        self.assertIsNotNone(response.data["image_base64"])

    @patch("apis.controller.page_controller.views.Page.objects.filter")
    def test_02_get_image_missing_session_id(self, mock_page_filter):
        """02: Test image retrieval without session_id"""
        request = self.factory.get("/page/get_image/", {"page_index": 0})
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        mock_page_filter.assert_not_called()

    @patch("apis.controller.page_controller.views.Page.objects.filter")
    def test_03_get_image_missing_page_index(self, mock_page_filter):
        """03: Test image retrieval without page_index"""
        request = self.factory.get(
            "/page/get_image/", {"session_id": str(uuid.uuid4())}
        )
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        mock_page_filter.assert_not_called()

    @patch("apis.controller.page_controller.views.Page.objects.filter")
    def test_04_get_image_session_not_found(self, mock_page_filter):
        """04: Test image retrieval with non-existent session"""
        mock_page_queryset = Mock()
        mock_page_queryset.order_by.return_value.__getitem__ = Mock(
            side_effect=IndexError
        )
        mock_page_filter.return_value = mock_page_queryset

        request = self.factory.get(
            "/page/get_image/",
            {"session_id": str(uuid.uuid4()), "page_index": 0},
        )
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)

    @patch("apis.controller.page_controller.views.Page.objects.filter")
    def test_05_get_image_page_not_found(self, mock_page_filter):
        """05: Test image retrieval with invalid page_index"""
        mock_page_queryset = Mock()
        mock_page_queryset.order_by.return_value.__getitem__ = Mock(
            side_effect=IndexError
        )
        mock_page_filter.return_value = mock_page_queryset

        request = self.factory.get(
            "/page/get_image/",
            {"session_id": str(uuid.uuid4()), "page_index": 999},
        )
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)

    @patch("apis.controller.page_controller.views.Page.objects.filter")
    @patch("os.path.exists")
    def test_06_get_image_file_not_found(self, mock_exists, mock_page_filter):
        """06: Test image retrieval when file doesn't exist"""
        mock_page = Mock()
        mock_page.img_url = "/fake/path/image.jpg"

        mock_page_queryset = Mock()
        mock_page_queryset.order_by.return_value.__getitem__ = Mock(
            return_value=mock_page
        )
        mock_page_filter.return_value = mock_page_queryset

        mock_exists.return_value = False

        request = self.factory.get(
            "/page/get_image/",
            {"session_id": str(uuid.uuid4()), "page_index": 0},
        )
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)

    @patch("apis.controller.page_controller.views.Page.objects.filter")
    @patch("os.path.exists")
    @patch("builtins.open", create=True)
    def test_07_get_image_file_read_error(
        self, mock_open, mock_exists, mock_page_filter
    ):
        """07: Test image retrieval with file read error"""
        mock_page = Mock()
        mock_page.img_url = "/fake/path/image.jpg"

        mock_page_queryset = Mock()
        mock_page_queryset.order_by.return_value.__getitem__ = Mock(
            return_value=mock_page
        )
        mock_page_filter.return_value = mock_page_queryset

        mock_exists.return_value = True
        mock_open.side_effect = IOError("Permission denied")

        request = self.factory.get(
            "/page/get_image/",
            {"session_id": str(uuid.uuid4()), "page_index": 0},
        )
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_500_INTERNAL_SERVER_ERROR)
        self.assertIn("error", response.data)

    @patch("apis.controller.page_controller.views.Page.objects.filter")
    def test_08_get_image_negative_page_index(self, mock_page_filter):
        """08: Test image retrieval with negative page_index"""
        mock_page = Mock()
        mock_page.img_url = "/fake/path/image.jpg"

        mock_page_queryset = Mock()
        mock_page_queryset.order_by.return_value.__getitem__ = Mock(
            return_value=mock_page
        )
        mock_page_filter.return_value = mock_page_queryset

        request = self.factory.get(
            "/page/get_image/",
            {"session_id": str(uuid.uuid4()), "page_index": -1},
        )

        with patch("os.path.exists") as mock_exists:
            with patch("builtins.open", create=True) as mock_open:
                with patch("base64.b64encode") as mock_b64encode:
                    mock_exists.return_value = True
                    mock_file = MagicMock()
                    mock_file.read.return_value = b"fake image"
                    mock_open.return_value.__enter__.return_value = mock_file
                    mock_b64encode.return_value = b"ZmFrZQ=="

                    response = self.view(request)

        # Should handle negative index via Python's list indexing
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        mock_page_queryset.order_by.return_value.__getitem__.assert_called_with(-1)

    @patch("apis.controller.page_controller.views.Page.objects.filter")
    def test_09_get_image_empty_img_url(self, mock_page_filter):
        """09: Test image retrieval when img_url is empty"""
        mock_page = Mock()
        mock_page.img_url = ""

        mock_page_queryset = Mock()
        mock_page_queryset.order_by.return_value.__getitem__ = Mock(
            return_value=mock_page
        )
        mock_page_filter.return_value = mock_page_queryset

        request = self.factory.get(
            "/page/get_image/",
            {"session_id": str(uuid.uuid4()), "page_index": 0},
        )
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)

    @patch("apis.controller.page_controller.views.Page.objects.filter")
    def test_10_get_image_null_img_url(self, mock_page_filter):
        """10: Test image retrieval when img_url is None"""
        mock_page = Mock()
        mock_page.img_url = None

        mock_page_queryset = Mock()
        mock_page_queryset.order_by.return_value.__getitem__ = Mock(
            return_value=mock_page
        )
        mock_page_filter.return_value = mock_page_queryset

        request = self.factory.get(
            "/page/get_image/",
            {"session_id": str(uuid.uuid4()), "page_index": 0},
        )
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)


class TestPageGetOCRView(TestCase):
    """Unit tests for PageGetOCRView - testing view logic only"""

    def setUp(self):
        """Set up test fixtures"""
        self.factory = APIRequestFactory()
        self.view = PageGetOCRView.as_view()

    @patch("apis.controller.page_controller.views.Page.objects.filter")
    def test_11_get_ocr_success_with_bbs(self, mock_page_filter):
        """11: Test successful OCR retrieval with bounding boxes"""
        # Create mock BBs
        mock_bb1 = Mock()
        mock_bb1.original_text = "Original text 1"
        mock_bb1.translated_text = "Translated text 1"
        mock_bb1.coordinates = {"x": 10, "y": 20, "width": 100, "height": 50}

        mock_bb2 = Mock()
        mock_bb2.original_text = "Original text 2"
        mock_bb2.translated_text = "Translated text 2"
        mock_bb2.coordinates = {"x": 30, "y": 80, "width": 120, "height": 60}

        mock_page = Mock()
        mock_page.created_at = timezone.now()
        mock_page.getBBs.return_value = [mock_bb1, mock_bb2]

        mock_page_queryset = Mock()
        mock_page_queryset.order_by.return_value.__getitem__ = Mock(
            return_value=mock_page
        )
        mock_page_filter.return_value = mock_page_queryset

        request = self.factory.get(
            "/page/get_ocr/", {"session_id": str(uuid.uuid4()), "page_index": 0}
        )
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data["page_index"], 0)
        self.assertEqual(len(response.data["ocr_results"]), 2)
        self.assertEqual(
            response.data["ocr_results"][0]["original_txt"], "Original text 1"
        )
        self.assertEqual(
            response.data["ocr_results"][0]["translation_txt"], "Translated text 1"
        )

    @patch("apis.controller.page_controller.views.Page.objects.filter")
    def test_12_get_ocr_success_no_bbs(self, mock_page_filter):
        """12: Test successful OCR retrieval with no bounding boxes"""
        mock_page = Mock()
        mock_page.created_at = timezone.now()
        mock_page.getBBs.return_value = []

        mock_page_queryset = Mock()
        mock_page_queryset.order_by.return_value.__getitem__ = Mock(
            return_value=mock_page
        )
        mock_page_filter.return_value = mock_page_queryset

        request = self.factory.get(
            "/page/get_ocr/", {"session_id": str(uuid.uuid4()), "page_index": 0}
        )
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(len(response.data["ocr_results"]), 0)

    @patch("apis.controller.page_controller.views.Page.objects.filter")
    def test_13_get_ocr_missing_session_id(self, mock_page_filter):
        """13: Test OCR retrieval without session_id"""
        request = self.factory.get("/page/get_ocr/", {"page_index": 0})
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        mock_page_filter.assert_not_called()

    @patch("apis.controller.page_controller.views.Page.objects.filter")
    def test_14_get_ocr_missing_page_index(self, mock_page_filter):
        """14: Test OCR retrieval without page_index"""
        request = self.factory.get("/page/get_ocr/", {"session_id": str(uuid.uuid4())})
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        mock_page_filter.assert_not_called()

    @patch("apis.controller.page_controller.views.Page.objects.filter")
    def test_15_get_ocr_session_not_found(self, mock_page_filter):
        """15: Test OCR retrieval with non-existent session"""
        mock_page_queryset = Mock()
        mock_page_queryset.order_by.return_value.__getitem__ = Mock(
            side_effect=IndexError
        )
        mock_page_filter.return_value = mock_page_queryset

        request = self.factory.get(
            "/page/get_ocr/",
            {"session_id": str(uuid.uuid4()), "page_index": 0},
        )
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)

    @patch("apis.controller.page_controller.views.Page.objects.filter")
    def test_16_get_ocr_page_not_found(self, mock_page_filter):
        """16: Test OCR retrieval with invalid page_index"""
        mock_page_queryset = Mock()
        mock_page_queryset.order_by.return_value.__getitem__ = Mock(
            side_effect=IndexError
        )
        mock_page_filter.return_value = mock_page_queryset

        request = self.factory.get(
            "/page/get_ocr/",
            {"session_id": str(uuid.uuid4()), "page_index": 999},
        )
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)


class TestPageGetTTSView(TestCase):
    """Unit tests for PageGetTTSView - testing view logic only"""

    def setUp(self):
        """Set up test fixtures"""
        self.factory = APIRequestFactory()
        self.view = PageGetTTSView.as_view()

    @patch("apis.controller.page_controller.views.Page.objects.filter")
    def test_17_get_tts_success_with_audio(self, mock_page_filter):
        """17: Test successful TTS retrieval with audio"""
        # Create mock BBs with audio
        mock_bb1 = Mock()
        mock_bb1.audio_base64 = ["audio_clip_1", "audio_clip_2"]

        mock_bb2 = Mock()
        mock_bb2.audio_base64 = ["audio_clip_3"]

        mock_page = Mock()
        mock_page.created_at = timezone.now()
        mock_page.getBBs.return_value = [mock_bb1, mock_bb2]

        mock_page_queryset = Mock()
        mock_page_queryset.order_by.return_value.__getitem__ = Mock(
            return_value=mock_page
        )
        mock_page_filter.return_value = mock_page_queryset

        request = self.factory.get(
            "/page/get_tts/", {"session_id": str(uuid.uuid4()), "page_index": 0}
        )
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data["page_index"], 0)
        self.assertEqual(len(response.data["audio_results"]), 2)
        self.assertEqual(response.data["audio_results"][0]["bbox_index"], 0)
        self.assertEqual(len(response.data["audio_results"][0]["audio_base64_list"]), 2)

    @patch("apis.controller.page_controller.views.Page.objects.filter")
    def test_18_get_tts_success_no_audio(self, mock_page_filter):
        """18: Test successful TTS retrieval with no audio"""
        # Create mock BB without audio
        mock_bb = Mock()
        mock_bb.audio_base64 = []

        mock_page = Mock()
        mock_page.created_at = timezone.now()
        mock_page.getBBs.return_value = [mock_bb]

        mock_page_queryset = Mock()
        mock_page_queryset.order_by.return_value.__getitem__ = Mock(
            return_value=mock_page
        )
        mock_page_filter.return_value = mock_page_queryset

        request = self.factory.get(
            "/page/get_tts/", {"session_id": str(uuid.uuid4()), "page_index": 0}
        )
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        # BBs without audio should not be included in results
        self.assertEqual(len(response.data["audio_results"]), 0)

    @patch("apis.controller.page_controller.views.Page.objects.filter")
    def test_19_get_tts_success_partial_audio(self, mock_page_filter):
        """19: Test TTS retrieval with partial audio completion"""
        # Create 3 BBs, only 2 have audio
        mock_bb1 = Mock()
        mock_bb1.audio_base64 = ["audio_1"]

        mock_bb2 = Mock()
        mock_bb2.audio_base64 = []

        mock_bb3 = Mock()
        mock_bb3.audio_base64 = ["audio_3"]

        mock_page = Mock()
        mock_page.created_at = timezone.now()
        mock_page.getBBs.return_value = [mock_bb1, mock_bb2, mock_bb3]

        mock_page_queryset = Mock()
        mock_page_queryset.order_by.return_value.__getitem__ = Mock(
            return_value=mock_page
        )
        mock_page_filter.return_value = mock_page_queryset

        request = self.factory.get(
            "/page/get_tts/", {"session_id": str(uuid.uuid4()), "page_index": 0}
        )
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        # Only 2 BBs with audio should be returned
        self.assertEqual(len(response.data["audio_results"]), 2)
        self.assertEqual(response.data["audio_results"][0]["bbox_index"], 0)
        self.assertEqual(response.data["audio_results"][1]["bbox_index"], 2)

    @patch("apis.controller.page_controller.views.Page.objects.filter")
    def test_20_get_tts_missing_session_id(self, mock_page_filter):
        """20: Test TTS retrieval without session_id"""
        request = self.factory.get("/page/get_tts/", {"page_index": 0})
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        mock_page_filter.assert_not_called()

    @patch("apis.controller.page_controller.views.Page.objects.filter")
    def test_21_get_tts_missing_page_index(self, mock_page_filter):
        """21: Test TTS retrieval without page_index"""
        request = self.factory.get("/page/get_tts/", {"session_id": str(uuid.uuid4())})
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        mock_page_filter.assert_not_called()

    @patch("apis.controller.page_controller.views.Page.objects.filter")
    def test_22_get_tts_session_not_found(self, mock_page_filter):
        """22: Test TTS retrieval with non-existent session"""
        mock_page_queryset = Mock()
        mock_page_queryset.order_by.return_value.__getitem__ = Mock(
            side_effect=IndexError
        )
        mock_page_filter.return_value = mock_page_queryset

        request = self.factory.get(
            "/page/get_tts/",
            {"session_id": str(uuid.uuid4()), "page_index": 0},
        )
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)

    @patch("apis.controller.page_controller.views.Page.objects.filter")
    def test_23_get_tts_page_not_found(self, mock_page_filter):
        """23: Test TTS retrieval with invalid page_index"""
        mock_page_queryset = Mock()
        mock_page_queryset.order_by.return_value.__getitem__ = Mock(
            side_effect=IndexError
        )
        mock_page_filter.return_value = mock_page_queryset

        request = self.factory.get(
            "/page/get_tts/",
            {"session_id": str(uuid.uuid4()), "page_index": 999},
        )
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)

    @patch("apis.controller.page_controller.views.Page.objects.filter")
    def test_24_get_tts_large_audio_list(self, mock_page_filter):
        """24: Test TTS retrieval with large number of audio clips"""
        # Create mock BB with many audio clips (100)
        mock_bb = Mock()
        mock_bb.audio_base64 = [f"audio_clip_{i}" for i in range(100)]

        mock_page = Mock()
        mock_page.created_at = timezone.now()
        mock_page.getBBs.return_value = [mock_bb]

        mock_page_queryset = Mock()
        mock_page_queryset.order_by.return_value.__getitem__ = Mock(
            return_value=mock_page
        )
        mock_page_filter.return_value = mock_page_queryset

        request = self.factory.get(
            "/page/get_tts/", {"session_id": str(uuid.uuid4()), "page_index": 0}
        )
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(
            len(response.data["audio_results"][0]["audio_base64_list"]), 100
        )

    @patch("apis.controller.page_controller.views.Page.objects.filter")
    def test_25_get_tts_json_parsing_audio(self, mock_page_filter):
        """25: Test TTS retrieval with JSON string audio (edge case)"""
        # Create mock BB with audio as JSON string
        mock_bb = Mock()
        mock_bb.audio_base64 = json.dumps(["audio_1", "audio_2"])

        mock_page = Mock()
        mock_page.created_at = timezone.now()
        mock_page.getBBs.return_value = [mock_bb]

        mock_page_queryset = Mock()
        mock_page_queryset.order_by.return_value.__getitem__ = Mock(
            return_value=mock_page
        )
        mock_page_filter.return_value = mock_page_queryset

        request = self.factory.get(
            "/page/get_tts/", {"session_id": str(uuid.uuid4()), "page_index": 0}
        )
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(len(response.data["audio_results"]), 1)

    @patch("apis.controller.page_controller.views.Page.objects.filter")
    def test_26_get_tts_database_error(self, mock_page_filter):
        """26: Test TTS retrieval with database error (500 error handling)"""
        mock_page_queryset = Mock()
        mock_page_queryset.order_by.return_value.__getitem__ = Mock(
            side_effect=Exception("Database connection error")
        )
        mock_page_filter.return_value = mock_page_queryset

        request = self.factory.get(
            "/page/get_tts/", {"session_id": str(uuid.uuid4()), "page_index": 0}
        )
        response = self.view(request)

        self.assertEqual(response.status_code, status.HTTP_500_INTERNAL_SERVER_ERROR)
        self.assertIn("error", response.data)
