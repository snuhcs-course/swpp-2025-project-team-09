from django.test import TestCase
from django.db import IntegrityError
from apis.models.user_model import User
from apis.models.session_model import Session
from apis.models.page_model import Page


class TestSessionPageIntegration(TestCase):
    """Integration tests for Session and Page model interactions"""

    def setUp(self):
        """Create a test user and session before each test"""
        self.test_user = User.objects.create(
            device_info="test-integration-device",
            language_preference="en",
        )
        self.test_session = Session.objects.create(
            user=self.test_user,
            title="Integration Test Book",
        )

    # -----------------------
    # Basic CRUD tests
    # -----------------------

    def test_01_get_pages_empty(self):
        """01: getPages returns an empty queryset when no pages exist"""
        pages = self.test_session.getPages()
        self.assertEqual(pages.count(), 0)
        self.assertFalse(pages.exists())

    def test_02_get_pages_with_pages(self):
        """02: getPages returns all pages for the session"""
        page1 = Page.objects.create(session=self.test_session, img_url="p1.jpg")
        page2 = Page.objects.create(session=self.test_session, img_url="p2.jpg")
        page3 = Page.objects.create(session=self.test_session, img_url="p3.jpg")

        pages = self.test_session.getPages()

        self.assertEqual(pages.count(), 3)
        self.assertIn(page1, pages)
        self.assertIn(page2, pages)
        self.assertIn(page3, pages)

    def test_03_add_page_using_helper_method(self):
        """03: addPage helper method successfully creates a page"""
        self.test_session.addPage("new_page.jpg", 0)

        pages = self.test_session.getPages()
        self.assertEqual(pages.count(), 1)
        self.assertEqual(pages[0].img_url, "new_page.jpg")

    def test_04_add_multiple_pages(self):
        """04: Adding multiple pages sequentially works correctly"""
        for i in range(5):
            self.test_session.addPage(f"page{i}.jpg", i)

        pages = self.test_session.getPages()
        self.assertEqual(pages.count(), 5)
        for page in pages:
            self.assertEqual(page.session, self.test_session)

    # -----------------------
    # Index parameter tests
    # -----------------------

    def test_05_index_parameter_is_ignored_but_acceptable(self):
        """05: addPage index parameter is accepted but does not affect page order"""
        self.test_session.addPage("img_test.jpg", 9999)
        pages = self.test_session.getPages()

        self.assertEqual(pages.count(), 1)
        self.assertEqual(pages[0].img_url, "img_test.jpg")

    # -----------------------
    # Page-Session relationship tests
    # -----------------------

    def test_06_page_session_relationship(self):
        """06: Page correctly references its parent session"""
        page = Page.objects.create(session=self.test_session, img_url="rel.jpg")

        self.assertEqual(page.session, self.test_session)
        self.assertIn(page, self.test_session.pages.all())

    def test_07_cascade_delete_session_deletes_pages(self):
        """07: Deleting a session cascades to delete its pages"""
        Page.objects.create(session=self.test_session, img_url="p1.jpg")
        Page.objects.create(session=self.test_session, img_url="p2.jpg")

        session_id = self.test_session.id
        self.assertEqual(Page.objects.filter(session=session_id).count(), 2)

        self.test_session.delete()

        with self.assertRaises(Session.DoesNotExist):
            Session.objects.get(id=session_id)

        self.assertEqual(Page.objects.filter(session_id=session_id).count(), 0)

    def test_08_multiple_sessions_with_pages(self):
        """08: Multiple sessions each maintain their own pages independently"""
        s1 = Session.objects.create(user=self.test_user, title="Book 1")
        s2 = Session.objects.create(user=self.test_user, title="Book 2")

        s1.addPage("b1_p1.jpg", 0)
        s1.addPage("b1_p2.jpg", 1)

        s2.addPage("b2_p1.jpg", 0)
        s2.addPage("b2_p2.jpg", 1)
        s2.addPage("b2_p3.jpg", 2)

        self.assertEqual(s1.getPages().count(), 2)
        self.assertEqual(s2.getPages().count(), 3)

    def test_09_query_pages_by_session(self):
        """09: Querying pages filtered by session returns correct results"""
        other = Session.objects.create(user=self.test_user, title="Other Book")

        Page.objects.create(session=self.test_session, img_url="main1.jpg")
        Page.objects.create(session=self.test_session, img_url="main2.jpg")
        Page.objects.create(session=other, img_url="other1.jpg")

        self.assertEqual(Page.objects.filter(session=self.test_session).count(), 2)
        self.assertEqual(Page.objects.filter(session=other).count(), 1)

    # -----------------------
    # Foreign key integrity tests
    # -----------------------

    def test_10_creating_page_without_session_raises_error(self):
        """10: Creating a Page with session=None should raise IntegrityError"""
        with self.assertRaises(IntegrityError):
            Page.objects.create(session=None, img_url="nope.jpg")

    def test_11_creating_session_without_user_raises_error(self):
        """11: Creating a Session with user=None should raise IntegrityError"""
        with self.assertRaises(IntegrityError):
            Session.objects.create(user=None, title="invalid-session")

    # -----------------------
    # Additional feature tests
    # -----------------------

    def test_12_page_count_reflects_total_pages(self):
        """12: totalPages field is correctly updated to reflect the actual page count"""
        for i in range(3):
            self.test_session.addPage(f"p{i}.jpg", i)

        actual = self.test_session.getPages().count()
        self.test_session.totalPages = actual
        self.test_session.save()

        self.test_session.refresh_from_db()
        self.assertEqual(self.test_session.totalPages, 3)

    def test_13_bulk_page_creation(self):
        """13: Bulk creating pages works and associates them with the correct session"""
        pages_to_create = [
            Page(session=self.test_session, img_url=f"p{i}.jpg")
            for i in range(10)
        ]
        Page.objects.bulk_create(pages_to_create)

        pages = self.test_session.getPages()
        self.assertEqual(pages.count(), 10)
