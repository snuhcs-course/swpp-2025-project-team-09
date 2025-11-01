from django.test import TestCase
from django.utils import timezone
from apis.models.user_model import User
from apis.models.session_model import Session
from apis.models.page_model import Page
import uuid


class TestSessionModel(TestCase):
    """Unit tests for Session model"""

    def setUp(self):
        """Set up test user for session tests"""
        self.test_user = User.objects.create(
            device_info="test-session-device",
            language_preference="en",
            created_at=timezone.now(),
        )

    def test_01_create_session_success(self):
        """Test successful session creation"""
        session = Session.objects.create(
            user=self.test_user, title="Test Book", created_at=timezone.now()
        )

        self.assertIsNotNone(session.id)
        self.assertEqual(session.user, self.test_user)
        self.assertEqual(session.title, "Test Book")
        self.assertIsNotNone(session.created_at)
        self.assertTrue(session.isOngoing)
        self.assertEqual(session.totalPages, 0)

    def test_02_session_id_auto_generation(self):
        """Test that session ID is automatically generated as UUID"""
        session = Session.objects.create(user=self.test_user, title="Test Book")

        self.assertIsInstance(session.id, uuid.UUID)
        self.assertIsNotNone(session.id)

    def test_03_session_default_values(self):
        """Test session default values"""
        session = Session.objects.create(
            user=self.test_user, title="Default Values Test"
        )

        self.assertTrue(session.isOngoing)
        self.assertEqual(session.totalPages, 0)
        self.assertIsNone(session.ended_at)
        self.assertIsNone(session.cover_img_url)
        self.assertIsNone(session.voicePreference)

    def test_04_session_str_representation(self):
        """Test __str__ method"""
        session = Session.objects.create(user=self.test_user, title="String Test Book")

        expected_str = f"{self.test_user.device_info} - String Test Book"
        self.assertEqual(str(session), expected_str)

    def test_05_session_user_relationship(self):
        """Test session-user relationship"""
        session = Session.objects.create(user=self.test_user, title="Relationship Test")

        self.assertEqual(session.user.uid, self.test_user.uid)
        self.assertIn(session, self.test_user.sessions.all())

    def test_06_get_pages_empty(self):
        """Test getPages with no pages"""
        session = Session.objects.create(user=self.test_user, title="Empty Pages Test")

        pages = session.getPages()
        self.assertEqual(pages.count(), 0)

    def test_07_get_pages_with_pages(self):
        """Test getPages with multiple pages"""
        session = Session.objects.create(user=self.test_user, title="Pages Test")

        # Create pages
        page1 = Page.objects.create(
            session=session, img_url="image1.jpg", created_at=timezone.now()
        )
        page2 = Page.objects.create(
            session=session, img_url="image2.jpg", created_at=timezone.now()
        )

        pages = session.getPages()
        self.assertEqual(pages.count(), 2)
        self.assertIn(page1, pages)
        self.assertIn(page2, pages)

    def test_08_add_page_success(self):
        """Test addPage method"""
        session = Session.objects.create(user=self.test_user, title="Add Page Test")

        session.addPage("test_image.jpg", 0)

        pages = session.getPages()
        self.assertEqual(pages.count(), 1)
        self.assertEqual(pages[0].img_url, "test_image.jpg")

    def test_09_add_multiple_pages(self):
        """Test adding multiple pages"""
        session = Session.objects.create(
            user=self.test_user, title="Multiple Pages Test"
        )

        session.addPage("page1.jpg", 0)
        session.addPage("page2.jpg", 1)
        session.addPage("page3.jpg", 2)

        pages = session.getPages()
        self.assertEqual(pages.count(), 3)

    def test_10_session_end(self):
        """Test ending a session"""
        session = Session.objects.create(user=self.test_user, title="End Session Test")

        self.assertTrue(session.isOngoing)
        self.assertIsNone(session.ended_at)

        # End session
        session.isOngoing = False
        session.ended_at = timezone.now()
        session.save()

        session.refresh_from_db()
        self.assertFalse(session.isOngoing)
        self.assertIsNotNone(session.ended_at)

    def test_11_update_total_pages(self):
        """Test updating totalPages"""
        session = Session.objects.create(user=self.test_user, title="Total Pages Test")

        self.assertEqual(session.totalPages, 0)

        session.totalPages = 10
        session.save()

        session.refresh_from_db()
        self.assertEqual(session.totalPages, 10)

    def test_12_set_voice_preference(self):
        """Test setting voicePreference"""
        session = Session.objects.create(
            user=self.test_user, title="Voice Preference Test"
        )

        self.assertIsNone(session.voicePreference)

        session.voicePreference = "male"
        session.save()

        session.refresh_from_db()
        self.assertEqual(session.voicePreference, "male")

    def test_13_set_cover_image(self):
        """Test setting cover_img_url"""
        session = Session.objects.create(user=self.test_user, title="Cover Image Test")

        session.cover_img_url = "cover.jpg"
        session.save()

        session.refresh_from_db()
        self.assertEqual(session.cover_img_url, "cover.jpg")

    def test_14_cascade_delete_with_user(self):
        """Test that deleting user cascades to sessions"""
        session = Session.objects.create(user=self.test_user, title="Cascade Test")
        session_id = session.id

        # Delete user
        self.test_user.delete()

        # Verify session was deleted
        with self.assertRaises(Session.DoesNotExist):
            Session.objects.get(id=session_id)

    def test_15_multiple_sessions_per_user(self):
        """Test creating multiple sessions for one user"""
        session1 = Session.objects.create(user=self.test_user, title="Book 1")
        session2 = Session.objects.create(user=self.test_user, title="Book 2")
        session3 = Session.objects.create(user=self.test_user, title="Book 3")

        sessions = self.test_user.sessions.all()
        self.assertEqual(sessions.count(), 3)
        self.assertIn(session1, sessions)
        self.assertIn(session2, sessions)
        self.assertIn(session3, sessions)

    def test_16_session_query_by_user(self):
        """Test querying sessions by user"""
        Session.objects.create(user=self.test_user, title="Queryable Session 1")
        Session.objects.create(user=self.test_user, title="Queryable Session 2")

        # Create another user with sessions
        other_user = User.objects.create(
            device_info="other-device", language_preference="ko"
        )
        Session.objects.create(user=other_user, title="Other User Session")

        # Query sessions for test_user
        user_sessions = Session.objects.filter(user=self.test_user)
        self.assertEqual(user_sessions.count(), 2)

        # Verify they belong to the correct user
        for session in user_sessions:
            self.assertEqual(session.user, self.test_user)

    def test_17_session_created_at_auto_set(self):
        """Test created_at is automatically set"""
        before_create = timezone.now()
        session = Session.objects.create(user=self.test_user, title="Created At Test")
        after_create = timezone.now()

        self.assertIsNotNone(session.created_at)
        self.assertGreaterEqual(session.created_at, before_create)
        self.assertLessEqual(session.created_at, after_create)
