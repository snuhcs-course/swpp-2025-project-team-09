from django.test import TestCase
from django.utils import timezone
from apis.models.user_model import User
from apis.models.session_model import Session
from apis.models.page_model import Page


class TestPageModel(TestCase):
    """Unit tests for Page model - testing Page model with minimal Session dependency"""

    def setUp(self):
        """Set up test session (required for Page foreign key)"""
        test_user = User.objects.create(
            device_info="test-page-device",
            language_preference="en",
        )
        self.test_session = Session.objects.create(
            user=test_user,
            title="Harry Potter and the Chamber of Secrets",
        )

    # ====================
    # Basic CRUD Operations
    # ====================

    def test_01_create_page_with_all_fields(self):
        """01: Test creating page with all fields"""
        page = Page.objects.create(
            session=self.test_session,
            img_url="pages/harry_potter_page1.jpg",
            audio_url="audio/harry_potter_page1.mp3",
            translation_text="Mr. and Mrs. Dursley, of number four, Privet Drive...",
            bbox_json={"boxes": [{"x": 10, "y": 20, "w": 100, "h": 50}]},
        )

        self.assertIsNotNone(page.id)
        self.assertEqual(page.session, self.test_session)
        self.assertEqual(page.img_url, "pages/harry_potter_page1.jpg")
        self.assertEqual(page.audio_url, "audio/harry_potter_page1.mp3")
        self.assertEqual(
            page.translation_text,
            "Mr. and Mrs. Dursley, of number four, Privet Drive...",
        )
        self.assertEqual(page.bbox_json, {"boxes": [{"x": 10, "y": 20, "w": 100, "h": 50}]})
        self.assertIsNotNone(page.created_at)

    def test_02_create_page_with_minimal_fields(self):
        """02: Test creating page with only required fields"""
        page = Page.objects.create(
            session=self.test_session,
            img_url="pages/minimal_page.jpg",
        )

        self.assertIsNotNone(page.id)
        self.assertEqual(page.img_url, "pages/minimal_page.jpg")
        # Check defaults
        self.assertIsNone(page.audio_url)
        self.assertIsNone(page.translation_text)
        self.assertEqual(page.bbox_json, {})

    def test_03_update_page_fields(self):
        """03: Test updating page fields"""
        page = Page.objects.create(
            session=self.test_session,
            img_url="pages/original.jpg",
        )

        page.img_url = "pages/updated.jpg"
        page.audio_url = "audio/updated.mp3"
        page.translation_text = "Updated translation"
        page.save()

        page.refresh_from_db()
        self.assertEqual(page.img_url, "pages/updated.jpg")
        self.assertEqual(page.audio_url, "audio/updated.mp3")
        self.assertEqual(page.translation_text, "Updated translation")

    def test_04_delete_page(self):
        """04: Test deleting a page"""
        page = Page.objects.create(
            session=self.test_session,
            img_url="pages/to_delete.jpg",
        )
        page_id = page.id

        page.delete()

        with self.assertRaises(Page.DoesNotExist):
            Page.objects.get(id=page_id)

    # ====================
    # Field Validation & Defaults
    # ====================

    def test_05_page_id_auto_generated(self):
        """05: Test page ID is auto-generated and unique"""
        page1 = Page.objects.create(session=self.test_session, img_url="page1.jpg")
        page2 = Page.objects.create(session=self.test_session, img_url="page2.jpg")

        self.assertIsNotNone(page1.id)
        self.assertIsNotNone(page2.id)
        self.assertNotEqual(page1.id, page2.id)

    def test_06_img_url_can_be_null(self):
        """06: Test img_url can be null"""
        page = Page.objects.create(
            session=self.test_session,
            img_url=None,
        )

        self.assertIsNone(page.img_url)

    def test_07_long_text_field_support(self):
        """07: Test TextField supports long content"""
        long_url = "https://storage.example.com/buckets/" + "a" * 500 + "/image.jpg"
        long_text = "Chapter One. " + "The Boy Who Lived. " * 200

        page = Page.objects.create(
            session=self.test_session,
            img_url=long_url,
            translation_text=long_text,
        )

        self.assertEqual(page.img_url, long_url)
        self.assertEqual(page.translation_text, long_text)
        self.assertGreater(len(page.img_url), 255)
        self.assertGreater(len(page.translation_text), 1000)

    # ====================
    # BBox JSON Field
    # ====================

    def test_08_set_bbox_json_simple(self):
        """08: Test setting simple bbox_json"""
        page = Page.objects.create(
            session=self.test_session,
            img_url="pages/bbox_simple.jpg",
        )

        bbox_data = {"x": 10, "y": 20, "width": 100, "height": 50}
        page.bbox_json = bbox_data
        page.save()

        page.refresh_from_db()
        self.assertEqual(page.bbox_json, bbox_data)

    def test_09_set_bbox_json_complex(self):
        """09: Test setting complex bbox_json with multiple boxes"""
        page = Page.objects.create(
            session=self.test_session,
            img_url="pages/bbox_complex.jpg",
        )

        bbox_data = {
            "boxes": [
                {"x": 10, "y": 20, "width": 100, "height": 50, "text": "Hello"},
                {"x": 150, "y": 80, "width": 200, "height": 60, "text": "World"},
                {"x": 50, "y": 200, "width": 300, "height": 100, "text": "OCR Result"},
            ]
        }
        page.bbox_json = bbox_data
        page.save()

        page.refresh_from_db()
        self.assertEqual(page.bbox_json, bbox_data)
        self.assertEqual(len(page.bbox_json["boxes"]), 3)

    # ====================
    # Timestamp Behavior
    # ====================

    def test_10_created_at_auto_set_on_creation(self):
        """10: Test created_at is automatically set when page is created"""
        before_create = timezone.now()
        page = Page.objects.create(
            session=self.test_session,
            img_url="pages/timestamp.jpg",
        )
        after_create = timezone.now()

        self.assertIsNotNone(page.created_at)
        self.assertGreaterEqual(page.created_at, before_create)
        self.assertLessEqual(page.created_at, after_create)

    # ====================
    # String Representation
    # ====================

    def test_11_page_str_representation(self):
        """11: Test __str__ returns 'Page {id} of Session {session_id}'"""
        page = Page.objects.create(
            session=self.test_session,
            img_url="pages/str_test.jpg",
        )

        expected_str = f"Page {page.id} of Session {self.test_session.id}"
        self.assertEqual(str(page), expected_str)

    # ====================
    # Querying & Filtering
    # ====================

    def test_12_filter_pages_by_content_availability(self):
        """12: Test filtering pages by content availability"""
        Page.objects.create(
            session=self.test_session,
            img_url="page1.jpg",
            audio_url="audio1.mp3",
            translation_text="Translated",
        )
        Page.objects.create(
            session=self.test_session,
            img_url="page2.jpg",
            audio_url=None,
            translation_text=None,
        )

        pages_with_audio = Page.objects.filter(audio_url__isnull=False)
        pages_with_translation = Page.objects.filter(translation_text__isnull=False)

        self.assertEqual(pages_with_audio.count(), 1)
        self.assertEqual(pages_with_translation.count(), 1)
