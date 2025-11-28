from django.test import TestCase
from django.utils import timezone
from apis.models.user_model import User
from apis.models.session_model import Session
from apis.models.page_model import Page
from apis.models.bb_model import BB


class TestPageModel(TestCase):
    """Unit tests for Page model"""

    def setUp(self):
        """Set up test user and session for page tests"""
        self.test_user = User.objects.create(
            device_info="test-page-device",
            language_preference="en",
            created_at=timezone.now(),
        )
        self.test_session = Session.objects.create(
            user=self.test_user, title="Test Session", created_at=timezone.now()
        )

    def test_01_create_page_success(self):
        """Test successful page creation"""
        page = Page.objects.create(
            session=self.test_session,
            img_url="test_image.jpg",
            created_at=timezone.now(),
        )

        self.assertIsNotNone(page.id)
        self.assertEqual(page.session, self.test_session)
        self.assertEqual(page.img_url, "test_image.jpg")
        self.assertIsNotNone(page.created_at)

    def test_02_page_default_values(self):
        """Test page default values"""
        page = Page.objects.create(session=self.test_session, img_url="test.jpg")

        self.assertIsNone(page.audio_url)
        self.assertIsNone(page.translation_text)
        self.assertEqual(page.bbox_json, {})

    def test_03_page_str_representation(self):
        """Test __str__ method"""
        page = Page.objects.create(session=self.test_session, img_url="test.jpg")

        expected_str = f"Page {page.id} of Session {self.test_session.id}"
        self.assertEqual(str(page), expected_str)

    def test_04_page_session_relationship(self):
        """Test page-session relationship"""
        page = Page.objects.create(
            session=self.test_session, img_url="relationship_test.jpg"
        )

        self.assertEqual(page.session, self.test_session)
        self.assertIn(page, self.test_session.pages.all())

    def test_05_get_bbs_empty(self):
        """Test getBBs with no bounding boxes"""
        page = Page.objects.create(session=self.test_session, img_url="empty_bbs.jpg")

        bbs = page.getBBs()
        self.assertEqual(bbs.count(), 0)

    def test_06_get_bbs_with_bbs(self):
        """Test getBBs with multiple bounding boxes"""
        page = Page.objects.create(session=self.test_session, img_url="with_bbs.jpg")

        # Create bounding boxes
        bb1 = BB.objects.create(
            page=page,
            original_text="Text 1",
            translated_text="Translation 1",
            coordinates={"x1": 0, "y1": 0, "x2": 100, "y2": 100},
        )
        bb2 = BB.objects.create(
            page=page,
            original_text="Text 2",
            translated_text="Translation 2",
            coordinates={"x1": 200, "y1": 200, "x2": 300, "y2": 300},
        )

        bbs = page.getBBs()
        self.assertEqual(bbs.count(), 2)
        self.assertIn(bb1, bbs)
        self.assertIn(bb2, bbs)

    def test_07_add_bb_success(self):
        """Test creating BB directly using Django ORM"""
        page = Page.objects.create(session=self.test_session, img_url="create_bb.jpg")

        bb = BB.objects.create(
            page=page,
            original_text="Original text",
            translated_text="Translated text",
            audio_base64=["base64_audio_data"],
            coordinates={
                "x1": 10,
                "y1": 20,
                "x2": 110,
                "y2": 20,
                "x3": 110,
                "y3": 70,
                "x4": 10,
                "y4": 70,
            },
        )

        bbs = page.getBBs()
        self.assertEqual(bbs.count(), 1)
        self.assertEqual(bbs[0].original_text, "Original text")
        self.assertEqual(bbs[0].translated_text, "Translated text")
        self.assertEqual(bbs[0].audio_base64, ["base64_audio_data"])

    def test_08_add_multiple_bbs(self):
        """Test creating multiple bounding boxes"""
        page = Page.objects.create(
            session=self.test_session, img_url="multiple_bbs.jpg"
        )

        BB.objects.create(
            page=page,
            original_text="Text 1",
            translated_text="Trans 1",
            audio_base64=["audio1"],
            coordinates={
                "x1": 10,
                "y1": 10,
                "x2": 50,
                "y2": 10,
                "x3": 50,
                "y3": 30,
                "x4": 10,
                "y4": 30,
            },
        )
        BB.objects.create(
            page=page,
            original_text="Text 2",
            translated_text="Trans 2",
            audio_base64=["audio2"],
            coordinates={
                "x1": 60,
                "y1": 10,
                "x2": 100,
                "y2": 10,
                "x3": 100,
                "y3": 30,
                "x4": 60,
                "y4": 30,
            },
        )
        BB.objects.create(
            page=page,
            original_text="Text 3",
            translated_text="Trans 3",
            audio_base64=["audio3"],
            coordinates={
                "x1": 10,
                "y1": 40,
                "x2": 50,
                "y2": 40,
                "x3": 50,
                "y3": 60,
                "x4": 10,
                "y4": 60,
            },
        )

        bbs = page.getBBs()
        self.assertEqual(bbs.count(), 3)

    def test_09_add_bb_with_missing_translations(self):
        """Test creating BB with empty translation"""
        page = Page.objects.create(session=self.test_session, img_url="empty_trans.jpg")

        bb = BB.objects.create(
            page=page,
            original_text="Text without translation",
            translated_text="",
            coordinates={
                "x1": 0,
                "y1": 0,
                "x2": 10,
                "y2": 10,
                "x3": 10,
                "y3": 20,
                "x4": 0,
                "y4": 20,
            },
        )

        self.assertEqual(bb.translated_text, "")

    def test_10_set_bbox_json(self):
        """Test setting bbox_json field"""
        page = Page.objects.create(session=self.test_session, img_url="bbox_json.jpg")

        bbox_data = {
            "boxes": [
                {"x": 10, "y": 20, "width": 100, "height": 50},
                {"x": 200, "y": 300, "width": 150, "height": 80},
            ]
        }
        page.bbox_json = bbox_data
        page.save()

        page.refresh_from_db()
        self.assertEqual(page.bbox_json, bbox_data)
        self.assertEqual(len(page.bbox_json["boxes"]), 2)

    def test_11_set_audio_url(self):
        """Test setting audio_url"""
        page = Page.objects.create(session=self.test_session, img_url="audio_test.jpg")

        page.audio_url = "audio/page1.mp3"
        page.save()

        page.refresh_from_db()
        self.assertEqual(page.audio_url, "audio/page1.mp3")

    def test_12_set_translation_text(self):
        """Test setting translation_text"""
        page = Page.objects.create(session=self.test_session, img_url="translation.jpg")

        page.translation_text = "This is a translated page"
        page.save()

        page.refresh_from_db()
        self.assertEqual(page.translation_text, "This is a translated page")

    def test_13_cascade_delete_with_session(self):
        """Test that deleting session cascades to pages"""
        page = Page.objects.create(
            session=self.test_session, img_url="cascade_test.jpg"
        )
        page_id = page.id

        # Delete session
        self.test_session.delete()

        # Verify page was deleted
        with self.assertRaises(Page.DoesNotExist):
            Page.objects.get(id=page_id)

    def test_14_cascade_delete_bbs(self):
        """Test that deleting page cascades to bounding boxes"""
        page = Page.objects.create(session=self.test_session, img_url="cascade_bbs.jpg")

        # Create bounding boxes
        bb1 = BB.objects.create(page=page, original_text="Text 1", coordinates={})
        bb2 = BB.objects.create(page=page, original_text="Text 2", coordinates={})

        bb1_id = bb1.id
        bb2_id = bb2.id

        # Delete page
        page.delete()

        # Verify BBs were deleted
        self.assertEqual(BB.objects.filter(id=bb1_id).count(), 0)
        self.assertEqual(BB.objects.filter(id=bb2_id).count(), 0)

    def test_15_multiple_pages_per_session(self):
        """Test creating multiple pages for one session"""
        page1 = Page.objects.create(session=self.test_session, img_url="page1.jpg")
        page2 = Page.objects.create(session=self.test_session, img_url="page2.jpg")
        page3 = Page.objects.create(session=self.test_session, img_url="page3.jpg")

        pages = self.test_session.pages.all()
        self.assertEqual(pages.count(), 3)
        self.assertIn(page1, pages)
        self.assertIn(page2, pages)
        self.assertIn(page3, pages)

    def test_16_page_created_at_auto_set(self):
        """Test created_at is automatically set"""
        before_create = timezone.now()
        page = Page.objects.create(session=self.test_session, img_url="created_at.jpg")
        after_create = timezone.now()

        self.assertIsNotNone(page.created_at)
        self.assertGreaterEqual(page.created_at, before_create)
        self.assertLessEqual(page.created_at, after_create)

    def test_17_page_query_by_session(self):
        """Test querying pages by session"""
        Page.objects.create(session=self.test_session, img_url="query_page1.jpg")
        Page.objects.create(session=self.test_session, img_url="query_page2.jpg")

        # Create another session with pages
        other_session = Session.objects.create(
            user=self.test_user, title="Other Session"
        )
        Page.objects.create(session=other_session, img_url="other_page.jpg")

        # Query pages for test_session
        session_pages = Page.objects.filter(session=self.test_session)
        self.assertEqual(session_pages.count(), 2)

        # Verify they belong to the correct session
        for page in session_pages:
            self.assertEqual(page.session, self.test_session)
