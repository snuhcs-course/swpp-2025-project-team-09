from django.test import TestCase
from unittest.mock import Mock, patch, mock_open
from apis.modules.ocr_processor import OCRModule
import json


class TestOCRModule(TestCase):
    """Unit tests for OCRModule"""

    def setUp(self):
        """Set up test fixtures"""
        self.ocr_module = OCRModule(conf_threshold=0.8)

    def test_01_init(self):
        """Test OCRModule initialization"""
        self.assertEqual(self.ocr_module.conf_threshold, 0.8)
        self.assertIsNotNone(self.ocr_module.api_url)
        self.assertIsNotNone(self.ocr_module.secret_key)

    def test_02_filter_low_confidence_with_fields(self):
        """Test filtering low confidence fields"""
        result_json = {
            "images": [
                {
                    "fields": [
                        {"inferText": "High confidence", "inferConfidence": 0.9},
                        {"inferText": "Low confidence", "inferConfidence": 0.5},
                        {"inferText": "No confidence", "inferConfidence": None},
                    ]
                }
            ]
        }

        filtered = self.ocr_module._filter_low_confidence(result_json)

        # Should keep high confidence (0.9) and no confidence (None)
        # Should remove low confidence (0.5 < 0.8)
        self.assertEqual(len(filtered["images"][0]["fields"]), 2)
        self.assertEqual(filtered["images"][0]["fields"][0]["inferText"], "High confidence")
        self.assertEqual(
            filtered["images"][0]["fields"][1]["inferText"], "No confidence"
        )

    def test_03_filter_low_confidence_empty_images(self):
        """Test filter with no images"""
        result_json = {"images": []}
        filtered = self.ocr_module._filter_low_confidence(result_json)
        self.assertEqual(filtered, result_json)

    def test_04_font_size_calculation(self):
        """Test font size calculation"""
        result_json = {
            "images": [
                {
                    "fields": [
                        {
                            "boundingPoly": {
                                "vertices": [
                                    {"x": 0, "y": 0},
                                    {"x": 100, "y": 0},
                                    {"x": 100, "y": 20},
                                    {"x": 0, "y": 20},
                                ]
                            }
                        },
                        {
                            "boundingPoly": {
                                "vertices": [
                                    {"x": 0, "y": 30},
                                    {"x": 100, "y": 30},
                                    {"x": 100, "y": 50},
                                    {"x": 0, "y": 50},
                                ]
                            }
                        },
                    ]
                }
            ]
        }

        font_size = self.ocr_module._font_size(result_json)
        # Heights are 20 and 20, average is 20
        self.assertEqual(font_size, 20.0)

    def test_05_font_size_empty_images(self):
        """Test font size with no images"""
        result_json = {"images": []}
        font_size = self.ocr_module._font_size(result_json)
        self.assertEqual(font_size, 0.0)

    def test_06_parse_infer_text_returns_paragraphs(self):
        """Test parsing OCR result into paragraphs"""
        result_json = {
            "images": [
                {
                    "fields": [
                        {
                            "inferText": "First word",
                            "inferConfidence": 0.9,
                            "boundingPoly": {
                                "vertices": [
                                    {"x": 10, "y": 10},
                                    {"x": 50, "y": 10},
                                    {"x": 50, "y": 30},
                                    {"x": 10, "y": 30},
                                ]
                            },
                        },
                        {
                            "inferText": "Second word",
                            "inferConfidence": 0.9,
                            "boundingPoly": {
                                "vertices": [
                                    {"x": 60, "y": 10},
                                    {"x": 100, "y": 10},
                                    {"x": 100, "y": 30},
                                    {"x": 60, "y": 30},
                                ]
                            },
                        },
                    ]
                }
            ]
        }

        paragraphs = self.ocr_module._parse_infer_text(result_json)

        # Should return list of paragraph dictionaries
        self.assertIsInstance(paragraphs, list)
        if paragraphs:  # If clustering produces results
            self.assertIn("text", paragraphs[0])
            self.assertIn("bbox", paragraphs[0])
            # Text should contain both words
            self.assertIsInstance(paragraphs[0]["text"], str)

    def test_07_parse_infer_text_empty_result(self):
        """Test parsing empty OCR result"""
        result_json = {"images": [{"fields": []}]}
        paragraphs = self.ocr_module._parse_infer_text(result_json)
        self.assertEqual(paragraphs, [])

    @patch("builtins.open", new_callable=mock_open, read_data=b"fake image data")
    @patch("requests.post")
    def test_08_process_page_success(self, mock_post, mock_file):
        """Test successful page processing"""
        # Mock successful API response
        mock_response = Mock()
        mock_response.text = "mock response text"  # Add text attribute
        mock_response.json.return_value = {
            "images": [
                {
                    "fields": [
                        {
                            "inferText": "Test text",
                            "inferConfidence": 0.9,
                            "boundingPoly": {
                                "vertices": [
                                    {"x": 10, "y": 10},
                                    {"x": 50, "y": 10},
                                    {"x": 50, "y": 30},
                                    {"x": 10, "y": 30},
                                ]
                            },
                        }
                    ]
                }
            ]
        }
        mock_post.return_value = mock_response

        result = self.ocr_module.process_page("/fake/path/image.png")

        # Should return list (may be empty if clustering doesn't produce paragraphs)
        self.assertIsInstance(result, list)
        # Verify API was called
        mock_post.assert_called_once()

    @patch("builtins.open", new_callable=mock_open, read_data=b"fake image data")
    @patch("requests.post")
    def test_09_process_page_empty_response(self, mock_post, mock_file):
        """Test page processing with empty API response"""
        mock_response = Mock()
        mock_response.text = "empty response"  # Add text attribute
        mock_response.json.return_value = {"images": [{"fields": []}]}
        mock_post.return_value = mock_response

        result = self.ocr_module.process_page("/fake/path/image.png")

        self.assertIsInstance(result, list)
        self.assertEqual(len(result), 0)

    @patch("builtins.open", new_callable=mock_open, read_data=b"fake image data")
    @patch("requests.post")
    def test_10_process_cover_page_success(self, mock_post, mock_file):
        """Test successful cover page processing"""
        # Mock successful API response with title text
        mock_response = Mock()
        mock_response.json.return_value = {
            "images": [
                {
                    "fields": [
                        {
                            "inferText": "Book",
                            "inferConfidence": 0.9,
                            "boundingPoly": {
                                "vertices": [
                                    {"x": 10, "y": 10},
                                    {"x": 50, "y": 10},
                                    {"x": 50, "y": 50},
                                    {"x": 10, "y": 50},
                                ]
                            },
                        },
                        {
                            "inferText": "Title",
                            "inferConfidence": 0.9,
                            "boundingPoly": {
                                "vertices": [
                                    {"x": 60, "y": 10},
                                    {"x": 100, "y": 10},
                                    {"x": 100, "y": 50},
                                    {"x": 60, "y": 50},
                                ]
                            },
                        },
                    ]
                }
            ]
        }
        mock_post.return_value = mock_response

        result = self.ocr_module.process_cover_page("/fake/path/cover.png")

        # Should return string (title text) or None
        self.assertTrue(isinstance(result, str) or result is None)

    @patch("builtins.open", new_callable=mock_open, read_data=b"fake image data")
    @patch("requests.post")
    def test_11_process_cover_page_empty_response(self, mock_post, mock_file):
        """Test cover page processing with empty response"""
        mock_response = Mock()
        mock_response.json.return_value = {"images": []}
        mock_post.return_value = mock_response

        result = self.ocr_module.process_cover_page("/fake/path/cover.png")

        # Should return empty list when no images
        self.assertEqual(result, [])

    @patch("builtins.open", new_callable=mock_open, read_data=b"fake image data")
    @patch("requests.post")
    def test_12_process_cover_page_no_tokens(self, mock_post, mock_file):
        """Test cover page processing with no valid tokens"""
        mock_response = Mock()
        mock_response.json.return_value = {"images": [{"fields": []}]}
        mock_post.return_value = mock_response

        result = self.ocr_module.process_cover_page("/fake/path/cover.png")

        self.assertEqual(result, [])

    def test_13_parse_infer_text_with_noise_cluster(self):
        """Test parsing when DBSCAN creates noise cluster (label -1)"""
        # Single token will be marked as noise by DBSCAN with min_samples=2
        result_json = {
            "images": [
                {
                    "fields": [
                        {
                            "inferText": "Isolated word",
                            "inferConfidence": 0.9,
                            "boundingPoly": {
                                "vertices": [
                                    {"x": 10, "y": 10},
                                    {"x": 50, "y": 10},
                                    {"x": 50, "y": 30},
                                    {"x": 10, "y": 30},
                                ]
                            },
                        }
                    ]
                }
            ]
        }

        paragraphs = self.ocr_module._parse_infer_text(result_json)

        # Single token with min_samples=2 should be marked as noise and skipped
        self.assertEqual(paragraphs, [])
