from django.test import TestCase
from apis.models.user_model import User
from apis.models.session_model import Session
from apis.models.page_model import Page
from apis.models.bb_model import BB


class TestPageBBIntegration(TestCase):
    """Integration tests for Page and BB model interactions"""

    def setUp(self):
        """Set up test user, session, and page"""
        self.test_user = User.objects.create(
            device_info="test-integration-device",
            language_preference="en",
        )
        self.test_session = Session.objects.create(
            user=self.test_user,
            title="The Little Prince",
        )
        self.test_page = Page.objects.create(
            session=self.test_session,
            img_url="pages/little_prince_page1.jpg",
        )

    # -----------------------
    # Basic BB retrieval tests
    # -----------------------

    def test_01_get_bbs_empty(self):
        """Return empty queryset when no BBs exist"""
        bbs = self.test_page.getBBs()
        self.assertEqual(bbs.count(), 0)
        self.assertFalse(bbs.exists())

    def test_02_get_bbs_with_bbs(self):
        """Return all BBs added to a page"""
        bb1 = BB.objects.create(
            page=self.test_page,
            original_text="All grown-ups were once children",
            translated_text="모든 어른은 한때 어린이였다",
            coordinates={"x1": 10, "y1": 20, "x2": 200, "y2": 40},
        )
        bb2 = BB.objects.create(
            page=self.test_page,
            original_text="but only few of them remember it",
            translated_text="하지만 그것을 기억하는 사람은 거의 없다",
            coordinates={"x1": 10, "y1": 50, "x2": 200, "y2": 70},
        )
        bb3 = BB.objects.create(
            page=self.test_page,
            original_text="- Antoine de Saint-Exupéry",
            translated_text="- 앙투안 드 생텍쥐페리",
            coordinates={"x1": 10, "y1": 80, "x2": 200, "y2": 100},
        )

        bbs = self.test_page.getBBs()
        self.assertEqual(bbs.count(), 3)
        self.assertEqual(list(bbs), [bb1, bb2, bb3])  # preserve insertion order

    # -----------------------
    # BB creation tests
    # -----------------------

    def test_03_add_bb_single_box(self):
        """Add a single BB using addBB helper"""
        bbox_list = [
            {"text": "Once when I was six years old", "x1": 50, "y1": 100, "x2": 250, "y2": 120, 
             "x3": 250, "y3": 120, "x4": 50, "y4": 120}
        ]
        translated_list = ["내가 여섯 살이었을 때"]
        audio_list = ["audio_base64_data_1"]

        self.test_page.addBB(bbox_list, translated_list, audio_list)
        bb = self.test_page.getBBs()[0]

        self.assertEqual(bb.original_text, "Once when I was six years old")
        self.assertEqual(bb.translated_text, "내가 여섯 살이었을 때")
        self.assertEqual(bb.audio_base64, "audio_base64_data_1")

    def test_04_add_bb_multiple_boxes(self):
        """Add multiple BBs at once and verify page reference"""
        bbox_list = [
            {"text": "I saw a magnificent picture", "x1": 10, "y1": 10, "x2": 100, "y2": 30, 
             "x3": 100, "y3": 30, "x4": 10, "y4": 30},
            {"text": "in a book called", "x1": 10, "y1": 40, "x2": 100, "y2": 60, 
             "x3": 100, "y3": 60, "x4": 10, "y4": 60},
            {"text": "True Stories from Nature", "x1": 10, "y1": 70, "x2": 100, "y2": 90, 
             "x3": 100, "y3": 90, "x4": 10, "y4": 90},
        ]
        translated_list = ["멋진 그림을 보았다", "책에서", "자연의 진실한 이야기"]
        audio_list = ["audio1", "audio2", "audio3"]

        self.test_page.addBB(bbox_list, translated_list, audio_list)
        bbs = self.test_page.getBBs()
        self.assertEqual(bbs.count(), 3)
        for bb in bbs:
            self.assertEqual(bb.page, self.test_page)

    def test_05_add_bb_with_missing_translations(self):
        """Add BBs with fewer translations than boxes"""
        bbox_list = [
            {"text": "Text 1", "x1": 0, "y1": 0, "x2": 10, "y2": 20, "x3": 10, "y3": 20, "x4": 0, "y4": 20},
            {"text": "Text 2", "x1": 20, "y1": 0, "x2": 30, "y2": 20, "x3": 30, "y3": 20, "x4": 20, "y4": 20},
        ]
        translated_list = ["Translation 1"]  # only one translation
        audio_list = []

        self.test_page.addBB(bbox_list, translated_list, audio_list)
        bbs = self.test_page.getBBs()
        self.assertEqual(bbs.count(), 2)
        self.assertEqual(bbs[0].translated_text, "Translation 1")
        self.assertEqual(bbs[1].translated_text, "")
        self.assertEqual(bbs[0].audio_base64, "")
        self.assertEqual(bbs[1].audio_base64, "")

    def test_06_add_bb_with_missing_audio(self):
        """Add BBs with fewer audio entries than boxes"""
        bbox_list = [
            {"text": "Hello", "x1": 0, "y1": 0, "x2": 10, "y2": 20, "x3": 10, "y3": 20, "x4": 0, "y4": 20},
            {"text": "World", "x1": 20, "y1": 0, "x2": 30, "y2": 20, "x3": 30, "y3": 20, "x4": 20, "y4": 20},
        ]
        translated_list = ["안녕", "세상"]
        audio_list = ["audio1"]

        self.test_page.addBB(bbox_list, translated_list, audio_list)
        bbs = self.test_page.getBBs()
        self.assertEqual(bbs[0].audio_base64, "audio1")
        self.assertEqual(bbs[1].audio_base64, "")

    # -----------------------
    # Relationship and cascade tests
    # -----------------------

    def test_07_bb_page_relationship(self):
        """BB correctly references its page"""
        bb = BB.objects.create(page=self.test_page, original_text="Relationship test", coordinates={})
        self.assertEqual(bb.page, self.test_page)
        self.assertIn(bb, self.test_page.bbs.all())

    def test_08_cascade_delete_page_deletes_bbs(self):
        """Deleting a page cascades to its BBs"""
        for i in range(3):
            BB.objects.create(page=self.test_page, original_text=f"BB {i}", coordinates={})

        page_id = self.test_page.id
        self.test_page.delete()

        with self.assertRaises(Page.DoesNotExist):
            Page.objects.get(id=page_id)
        self.assertEqual(BB.objects.filter(page_id=page_id).count(), 0)

    # -----------------------
    # Multiple pages / query tests
    # -----------------------

    def test_09_multiple_pages_with_bbs(self):
        """Multiple pages maintain separate BBs correctly"""
        user2 = User.objects.create(device_info="test-user2", language_preference="ko")
        session2 = Session.objects.create(user=user2, title="1984")
        page2 = Page.objects.create(session=session2, img_url="pages/1984_page1.jpg")

        # Add BBs to each page
        self.test_page.addBB([{"text": "Page1 BB1", "x1":0,"y1":0,"x2":10,"y2":10,"x3":10,"y3":10,"x4":0,"y4":10}],
                             ["Translation1"], ["audio1"])
        self.test_page.addBB([{"text": "Page1 BB2", "x1":0,"y1":0,"x2":10,"y2":10,"x3":10,"y3":10,"x4":0,"y4":10}],
                             ["Translation2"], ["audio2"])
        page2.addBB([{"text": "Page2 BB1", "x1":0,"y1":0,"x2":10,"y2":10,"x3":10,"y3":10,"x4":0,"y4":10}],
                    ["Translation3"], ["audio3"])

        self.assertEqual(self.test_page.getBBs().count(), 2)
        self.assertEqual(page2.getBBs().count(), 1)

    def test_10_query_bbs_by_page(self):
        """Query BBs filtered by page"""
        user3 = User.objects.create(device_info="test-user3", language_preference="en")
        session3 = Session.objects.create(user=user3, title="Other Book")
        other_page = Page.objects.create(session=session3, img_url="pages/other_page.jpg")

        BB.objects.create(page=self.test_page, original_text="Main BB1", coordinates={})
        BB.objects.create(page=self.test_page, original_text="Main BB2", coordinates={})
        BB.objects.create(page=other_page, original_text="Other BB1", coordinates={})

        main_bbs = BB.objects.filter(page=self.test_page)
        other_bbs = BB.objects.filter(page=other_page)

        self.assertEqual(main_bbs.count(), 2)
        self.assertEqual(other_bbs.count(), 1)
