from django.test import TestCase
from apis.models.user_model import User
from apis.models.session_model import Session
from apis.models.page_model import Page
from apis.models.bb_model import BB


class TestBBModel(TestCase):
    """Unit tests for BB model - testing BB model with minimal Page dependency"""

    def setUp(self):
        """Set up test page (required for BB foreign key)"""
        test_user = User.objects.create(
            device_info="test-bb-device",
            language_preference="en",
        )
        test_session = Session.objects.create(
            user=test_user,
            title="Animal Farm",
        )
        self.test_page = Page.objects.create(
            session=test_session,
            img_url="pages/animal_farm_page1.jpg",
        )

    # ====================
    # Basic CRUD Operations
    # ====================

    def test_01_create_bb_with_all_fields(self):
        """01: Test creating BB with all fields"""
        bb = BB.objects.create(
            page=self.test_page,
            original_text="All animals are equal",
            translated_text="모든 동물은 평등하다",
            audio_base64=["audio_clip_1", "audio_clip_2"],
            coordinates={
                "x1": 50,
                "y1": 100,
                "x2": 350,
                "y2": 100,
                "x3": 350,
                "y3": 140,
                "x4": 50,
                "y4": 140,
            },
            tts_status="ready",
        )

        self.assertIsNotNone(bb.id)
        self.assertEqual(bb.page, self.test_page)
        self.assertEqual(bb.original_text, "All animals are equal")
        self.assertEqual(bb.translated_text, "모든 동물은 평등하다")
        self.assertEqual(bb.audio_base64, ["audio_clip_1", "audio_clip_2"])
        self.assertEqual(bb.coordinates["x1"], 50)
        self.assertEqual(bb.tts_status, "ready")

    def test_02_create_bb_with_minimal_fields(self):
        """02: Test creating BB with only required fields"""
        bb = BB.objects.create(
            page=self.test_page,
            original_text="but some animals are more equal than others",
        )

        self.assertIsNotNone(bb.id)
        self.assertEqual(
            bb.original_text, "but some animals are more equal than others"
        )
        # Check defaults
        self.assertIsNone(bb.translated_text)
        self.assertEqual(bb.audio_base64, [])
        self.assertEqual(bb.coordinates, {})
        self.assertEqual(bb.tts_status, "pending")

    def test_03_update_bb_fields(self):
        """03: Test updating BB fields"""
        bb = BB.objects.create(
            page=self.test_page,
            original_text="Original version",
        )

        bb.original_text = "Updated version"
        bb.translated_text = "번역된 버전"
        bb.audio_base64 = ["audio1"]
        bb.tts_status = "processing"
        bb.save()

        bb.refresh_from_db()
        self.assertEqual(bb.original_text, "Updated version")
        self.assertEqual(bb.translated_text, "번역된 버전")
        self.assertEqual(bb.audio_base64, ["audio1"])
        self.assertEqual(bb.tts_status, "processing")

    def test_04_delete_bb(self):
        """04: Test deleting a bounding box"""
        bb = BB.objects.create(
            page=self.test_page,
            original_text="To be deleted",
        )
        bb_id = bb.id

        bb.delete()

        with self.assertRaises(BB.DoesNotExist):
            BB.objects.get(id=bb_id)

    # ====================
    # Field Validation & Defaults
    # ====================

    def test_05_bb_id_auto_generated(self):
        """05: Test BB ID is auto-generated and unique"""
        bb1 = BB.objects.create(page=self.test_page, original_text="Text 1")
        bb2 = BB.objects.create(page=self.test_page, original_text="Text 2")

        self.assertIsNotNone(bb1.id)
        self.assertIsNotNone(bb2.id)
        self.assertNotEqual(bb1.id, bb2.id)

    def test_06_long_text_and_unicode_support(self):
        """06: Test long text and Unicode support"""
        long_text = (
            "Napoleon had commanded that once a week there should be held something called a Spontaneous Demonstration. "
            * 50
        )
        bb = BB.objects.create(
            page=self.test_page,
            original_text=long_text,
            translated_text="네 발은 좋고, 두 발은 나쁘다! 🐷🐴",
        )

        self.assertEqual(bb.original_text, long_text)
        self.assertEqual(bb.translated_text, "네 발은 좋고, 두 발은 나쁘다! 🐷🐴")
        self.assertGreater(len(bb.original_text), 1000)

    # ====================
    # Translation & Audio
    # ====================

    def test_07_update_translation_using_method(self):
        """07: Test updateTranslation helper method"""
        bb = BB.objects.create(
            page=self.test_page,
            original_text="Whatever goes upon two legs is an enemy",
            translated_text="Initial translation",
        )

        bb.updateTranslation("두 발로 걷는 것은 모두 적이다")

        bb.refresh_from_db()
        self.assertEqual(bb.translated_text, "두 발로 걷는 것은 모두 적이다")

    def test_08_update_audio_using_method(self):
        """08: Test updateAudio helper method"""
        bb = BB.objects.create(
            page=self.test_page,
            original_text="Audio update test",
            audio_base64=[],
        )

        new_audio = ["new_audio_clip_1", "new_audio_clip_2"]
        bb.updateAudio(new_audio)

        bb.refresh_from_db()
        self.assertEqual(bb.audio_base64, new_audio)

    # ====================
    # Coordinates & Position
    # ====================

    def test_09_set_coordinates_complex(self):
        """09: Test setting complex coordinates"""
        coords = {
            "x1": 50,
            "y1": 100,
            "x2": 350,
            "y2": 100,
            "x3": 350,
            "y3": 140,
            "x4": 50,
            "y4": 140,
            "width": 300,
            "height": 40,
        }
        bb = BB.objects.create(
            page=self.test_page,
            original_text="Complex coordinates",
            coordinates=coords,
        )

        bb.refresh_from_db()
        self.assertEqual(bb.coordinates, coords)
        self.assertEqual(bb.coordinates["width"], 300)

    def test_10_update_position_using_method(self):
        """10: Test updatePosition helper method"""
        bb = BB.objects.create(
            page=self.test_page,
            original_text="Position update test",
            coordinates={"x1": 0, "y1": 0},
        )

        new_position = {"x2": 200, "y2": 150}
        bb.updatePosition(new_position)

        bb.refresh_from_db()
        self.assertEqual(bb.coordinates["x1"], 0)  # Original values preserved
        self.assertEqual(bb.coordinates["y1"], 0)
        self.assertEqual(bb.coordinates["x2"], 200)  # New values added
        self.assertEqual(bb.coordinates["y2"], 150)

    def test_11_update_position_with_non_dict_coordinates(self):
        """11: Test updatePosition handles case when coordinates is not a dict"""
        bb = BB.objects.create(
            page=self.test_page,
            original_text="Non-dict coordinates test",
            coordinates=[],  # Simulate invalid type
        )

        # Apply new position
        new_position = {"x1": 100, "y1": 200}
        bb.updatePosition(new_position)

        bb.refresh_from_db()
        self.assertEqual(bb.coordinates["x1"], 100)
        self.assertEqual(bb.coordinates["y1"], 200)

    def test_12_points_property(self):
        """12: Test points property returns tuple list"""
        bb = BB.objects.create(
            page=self.test_page,
            original_text="Points property test",
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

        expected_points = [(10, 20), (110, 20), (110, 70), (10, 70)]
        self.assertEqual(bb.points, expected_points)

    # ====================
    # TTS Status
    # ====================

    def test_13_tts_status_choices_and_workflow(self):
        """13: Test TTS status choices and workflow"""
        # Test all choices
        bb1 = BB.objects.create(
            page=self.test_page, original_text="Pending", tts_status="pending"
        )
        bb2 = BB.objects.create(
            page=self.test_page, original_text="Processing", tts_status="processing"
        )
        bb3 = BB.objects.create(
            page=self.test_page, original_text="Ready", tts_status="ready"
        )
        bb4 = BB.objects.create(
            page=self.test_page, original_text="Failed", tts_status="failed"
        )

        self.assertEqual(bb1.tts_status, "pending")
        self.assertEqual(bb2.tts_status, "processing")
        self.assertEqual(bb3.tts_status, "ready")
        self.assertEqual(bb4.tts_status, "failed")

        # Test workflow transition
        bb1.tts_status = "processing"
        bb1.save()
        bb1.refresh_from_db()
        self.assertEqual(bb1.tts_status, "processing")

        bb1.tts_status = "ready"
        bb1.save()
        bb1.refresh_from_db()
        self.assertEqual(bb1.tts_status, "ready")

    # ====================
    # String Representation
    # ====================

    def test_14_bb_str_representation(self):
        """14: Test __str__ returns 'BB of Page {page_id}'"""
        bb = BB.objects.create(
            page=self.test_page,
            original_text="String representation test",
        )

        expected_str = f"BB of Page {self.test_page.id}"
        self.assertEqual(str(bb), expected_str)

    # ====================
    # Querying & Filtering
    # ====================

    def test_15_filter_bbs_by_status_and_translation(self):
        """15: Test filtering BBs by TTS status and translation"""
        BB.objects.create(
            page=self.test_page,
            original_text="Pending 1",
            tts_status="pending",
            translated_text=None,
        )
        BB.objects.create(
            page=self.test_page,
            original_text="Ready 1",
            tts_status="ready",
            translated_text="Translation 1",
        )

        pending_bbs = BB.objects.filter(tts_status="pending")
        ready_bbs = BB.objects.filter(tts_status="ready")
        translated_bbs = BB.objects.filter(translated_text__isnull=False)

        self.assertEqual(pending_bbs.count(), 1)
        self.assertEqual(ready_bbs.count(), 1)
        self.assertEqual(translated_bbs.count(), 1)

    # ====================
    # Complete Workflow
    # ====================

    def test_16_bb_complete_workflow(self):
        """16: Test complete BB workflow from creation to completion"""
        # Create BB
        bb = BB.objects.create(
            page=self.test_page,
            original_text="The windmill was Napoleon's own creation",
        )

        self.assertEqual(bb.tts_status, "pending")
        self.assertIsNone(bb.translated_text)

        # Add translation
        bb.updateTranslation("풍차는 나폴레옹 자신의 창조물이었다")
        bb.refresh_from_db()
        self.assertEqual(bb.translated_text, "풍차는 나폴레옹 자신의 창조물이었다")

        # Add position
        bb.updatePosition({"x1": 10, "y1": 10, "x2": 300, "y2": 50})
        bb.refresh_from_db()
        self.assertEqual(bb.coordinates["x1"], 10)

        # Set status to processing
        bb.tts_status = "processing"
        bb.save()

        # Add audio
        bb.updateAudio(["audio_part1", "audio_part2"])
        bb.refresh_from_db()
        self.assertEqual(bb.audio_base64, ["audio_part1", "audio_part2"])

        # Set status to ready
        bb.tts_status = "ready"
        bb.save()
        bb.refresh_from_db()

        # Verify final state
        self.assertEqual(bb.original_text, "The windmill was Napoleon's own creation")
        self.assertEqual(bb.translated_text, "풍차는 나폴레옹 자신의 창조물이었다")
        self.assertEqual(bb.audio_base64, ["audio_part1", "audio_part2"])
        self.assertEqual(bb.coordinates["x2"], 300)
        self.assertEqual(bb.tts_status, "ready")
