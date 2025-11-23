from django.test import TestCase
from django.utils import timezone
from apis.models.user_model import User
from apis.models.session_model import Session
from apis.models.page_model import Page
from apis.models.bb_model import BB


class TestBBModel(TestCase):
    """Unit tests for BB (Bounding Box) model"""

    def setUp(self):
        """Set up test user, session, and page for BB tests"""
        self.test_user = User.objects.create(
            device_info="test-bb-device",
            language_preference="en",
            created_at=timezone.now(),
        )
        self.test_session = Session.objects.create(
            user=self.test_user, title="Test Session", created_at=timezone.now()
        )
        self.test_page = Page.objects.create(
            session=self.test_session,
            img_url="test_page.jpg",
            created_at=timezone.now(),
        )

    def test_01_create_bb_success(self):
        """Test successful BB creation"""
        bb = BB.objects.create(
            page=self.test_page,
            original_text="Test text",
            translated_text="Translated text",
            coordinates={"x1": 0, "y1": 0, "x2": 100, "y2": 100},
        )

        self.assertIsNotNone(bb.id)
        self.assertEqual(bb.page, self.test_page)
        self.assertEqual(bb.original_text, "Test text")
        self.assertEqual(bb.translated_text, "Translated text")
        self.assertEqual(bb.coordinates, {"x1": 0, "y1": 0, "x2": 100, "y2": 100})

    def test_02_bb_default_values(self):
        """Test BB default values"""
        bb = BB.objects.create(page=self.test_page, original_text="Default test")

        self.assertIsNone(bb.translated_text)
        self.assertEqual(bb.audio_base64, [])
        self.assertEqual(bb.coordinates, {})
        self.assertEqual(bb.tts_status, "pending")

    def test_03_bb_str_representation(self):
        """Test __str__ method"""
        bb = BB.objects.create(page=self.test_page, original_text="String test")

        expected_str = f"BB of Page {self.test_page.id}"
        self.assertEqual(str(bb), expected_str)

    def test_04_bb_page_relationship(self):
        """Test BB-page relationship"""
        bb = BB.objects.create(page=self.test_page, original_text="Relationship test")

        self.assertEqual(bb.page, self.test_page)
        self.assertIn(bb, self.test_page.bbs.all())

    def test_5_tts_status_choices(self):
        """Test tts_status choices"""
        # Test pending (default)
        bb1 = BB.objects.create(page=self.test_page, original_text="Pending")
        self.assertEqual(bb1.tts_status, "pending")

        # Test processing
        bb2 = BB.objects.create(
            page=self.test_page, original_text="Processing", tts_status="processing"
        )
        self.assertEqual(bb2.tts_status, "processing")

        # Test ready
        bb3 = BB.objects.create(
            page=self.test_page, original_text="Ready", tts_status="ready"
        )
        self.assertEqual(bb3.tts_status, "ready")

        # Test failed
        bb4 = BB.objects.create(
            page=self.test_page, original_text="Failed", tts_status="failed"
        )
        self.assertEqual(bb4.tts_status, "failed")

    def test_6_update_tts_status(self):
        """Test updating tts_status"""
        bb = BB.objects.create(page=self.test_page, original_text="Status update test")

        self.assertEqual(bb.tts_status, "pending")

        bb.tts_status = "processing"
        bb.save()
        bb.refresh_from_db()
        self.assertEqual(bb.tts_status, "processing")

        bb.tts_status = "ready"
        bb.save()
        bb.refresh_from_db()
        self.assertEqual(bb.tts_status, "ready")

    def test_7_cascade_delete_with_page(self):
        """Test that deleting page cascades to BBs"""
        bb1 = BB.objects.create(page=self.test_page, original_text="BB 1")
        bb2 = BB.objects.create(page=self.test_page, original_text="BB 2")

        bb1_id = bb1.id
        bb2_id = bb2.id

        # Delete page
        self.test_page.delete()

        # Verify BBs were deleted
        self.assertEqual(BB.objects.filter(id=bb1_id).count(), 0)
        self.assertEqual(BB.objects.filter(id=bb2_id).count(), 0)

    def test_8_multiple_bbs_per_page(self):
        """Test creating multiple BBs for one page"""
        bb1 = BB.objects.create(page=self.test_page, original_text="Text 1")
        bb2 = BB.objects.create(page=self.test_page, original_text="Text 2")
        bb3 = BB.objects.create(page=self.test_page, original_text="Text 3")

        bbs = self.test_page.bbs.all()
        self.assertEqual(bbs.count(), 3)
        self.assertIn(bb1, bbs)
        self.assertIn(bb2, bbs)
        self.assertIn(bb3, bbs)

    def test_9_audio_base64_as_list(self):
        """Test audio_base64 stored as list"""
        audio_data = ["clip1_base64", "clip2_base64", "clip3_base64"]
        bb = BB.objects.create(
            page=self.test_page,
            original_text="Audio list test",
            audio_base64=audio_data,
        )

        bb.refresh_from_db()
        self.assertEqual(bb.audio_base64, audio_data)
        self.assertEqual(len(bb.audio_base64), 3)

    def test_10_coordinates_as_dict(self):
        """Test coordinates stored as dict"""
        coords = {
            "x1": 10,
            "y1": 20,
            "x2": 110,
            "y2": 20,
            "x3": 110,
            "y3": 70,
            "x4": 10,
            "y4": 70,
            "width": 100,
            "height": 50,
        }
        bb = BB.objects.create(
            page=self.test_page, original_text="Coords dict test", coordinates=coords
        )

        bb.refresh_from_db()
        self.assertEqual(bb.coordinates, coords)
        self.assertEqual(bb.coordinates["width"], 100)
        self.assertEqual(bb.coordinates["height"], 50)

    def test_11_bb_query_by_page(self):
        """Test querying BBs by page"""
        BB.objects.create(page=self.test_page, original_text="Query BB 1")
        BB.objects.create(page=self.test_page, original_text="Query BB 2")

        # Create another page with BBs
        other_page = Page.objects.create(
            session=self.test_session, img_url="other_page.jpg"
        )
        BB.objects.create(page=other_page, original_text="Other page BB")

        # Query BBs for test_page
        page_bbs = BB.objects.filter(page=self.test_page)
        self.assertEqual(page_bbs.count(), 2)

        # Verify they belong to the correct page
        for bb in page_bbs:
            self.assertEqual(bb.page, self.test_page)

    def test_12_empty_translation(self):
        """Test BB with empty translation"""
        bb = BB.objects.create(page=self.test_page, original_text="No translation")

        self.assertEqual(bb.original_text, "No translation")
        self.assertIsNone(bb.translated_text)

    def test_13_update_multiple_fields(self):
        """Test updating multiple fields at once"""
        bb = BB.objects.create(page=self.test_page, original_text="Multi update test")

        bb.translated_text = "New translation"
        bb.audio_base64 = ["audio1", "audio2"]
        bb.tts_status = "ready"
        bb.coordinates = {"x1": 0, "y1": 0, "x2": 100, "y2": 100}
        bb.save()

        bb.refresh_from_db()
        self.assertEqual(bb.translated_text, "New translation")
        self.assertEqual(bb.audio_base64, ["audio1", "audio2"])
        self.assertEqual(bb.tts_status, "ready")
        self.assertEqual(bb.coordinates["x2"], 100)
