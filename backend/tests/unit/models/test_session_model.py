from django.test import TestCase
from django.utils import timezone
from apis.models.user_model import User
from apis.models.session_model import Session
import uuid


class TestSessionModel(TestCase):
    """Unit tests for Session model - testing Session model with minimal User dependency"""

    def setUp(self):
        """Set up test user (required for Session foreign key)"""
        self.test_user = User.objects.create(
            device_info="test-session-device",
            language_preference="en",
        )

    # ====================
    # Basic CRUD Operations
    # ====================

    def test_01_create_session_with_all_fields(self):
        """01: Test creating session with all required fields"""
        session = Session.objects.create(
            user=self.test_user,
            title="Harry Potter and the Philosopher's Stone",
            translated_title="해리 포터와 마법사의 돌",
            cover_img_url="harry_potter_cover.jpg",
            voicePreference="female",
        )

        self.assertIsNotNone(session.id)
        self.assertIsInstance(session.id, uuid.UUID)
        self.assertEqual(session.user, self.test_user)
        self.assertEqual(session.title, "Harry Potter and the Philosopher's Stone")
        self.assertEqual(session.translated_title, "해리 포터와 마법사의 돌")
        self.assertEqual(session.cover_img_url, "harry_potter_cover.jpg")
        self.assertEqual(session.voicePreference, "female")
        self.assertIsNotNone(session.created_at)

    def test_02_create_session_with_minimal_fields(self):
        """02: Test creating session with only required fields"""
        session = Session.objects.create(
            user=self.test_user,
            title="The Little Prince",
        )

        self.assertIsNotNone(session.id)
        self.assertEqual(session.title, "The Little Prince")
        # Check defaults
        self.assertTrue(session.isOngoing)
        self.assertEqual(session.totalPages, 0)
        self.assertEqual(session.totalWords, 0)
        self.assertIsNone(session.ended_at)
        self.assertIsNone(session.translated_title)
        self.assertIsNone(session.cover_img_url)
        self.assertIsNone(session.voicePreference)

    def test_03_update_session_title(self):
        """03: Test updating session title"""
        session = Session.objects.create(
            user=self.test_user,
            title="Original Title",
        )

        session.title = "Updated Title"
        session.save()

        session.refresh_from_db()
        self.assertEqual(session.title, "Updated Title")

    def test_04_delete_session(self):
        """04: Test deleting a session"""
        session = Session.objects.create(
            user=self.test_user,
            title="Session to Delete",
        )
        session_id = session.id

        session.delete()

        with self.assertRaises(Session.DoesNotExist):
            Session.objects.get(id=session_id)

    # ====================
    # Field Validation & Defaults
    # ====================

    def test_05_session_uuid_auto_generated_and_unique(self):
        """05: Test each session gets unique auto-generated UUID"""
        session1 = Session.objects.create(user=self.test_user, title="Book 1")
        session2 = Session.objects.create(user=self.test_user, title="Book 2")

        self.assertIsInstance(session1.id, uuid.UUID)
        self.assertIsInstance(session2.id, uuid.UUID)
        self.assertNotEqual(session1.id, session2.id)

    def test_06_session_title_with_unicode(self):
        """06: Test title supports international characters"""
        unicode_title = "나미야 잡화점의 기적"
        session = Session.objects.create(
            user=self.test_user,
            title=unicode_title,
        )

        self.assertEqual(session.title, unicode_title)

    # ====================
    # Session Lifecycle
    # ====================

    def test_07_end_reading_session(self):
        """07: Test ending a reading session"""
        session = Session.objects.create(
            user=self.test_user,
            title="Session to End",
        )

        self.assertTrue(session.isOngoing)
        self.assertIsNone(session.ended_at)

        session.isOngoing = False
        session.ended_at = timezone.now()
        session.save()

        session.refresh_from_db()
        self.assertFalse(session.isOngoing)
        self.assertIsNotNone(session.ended_at)

    def test_08_update_reading_progress(self):
        """08: Test updating totalPages and totalWords"""
        session = Session.objects.create(
            user=self.test_user,
            title="Progress Test",
        )

        self.assertEqual(session.totalPages, 0)
        self.assertEqual(session.totalWords, 0)

        session.totalPages = 50
        session.totalWords = 15000
        session.save()

        session.refresh_from_db()
        self.assertEqual(session.totalPages, 50)
        self.assertEqual(session.totalWords, 15000)

    # ====================
    # Optional Fields
    # ====================

    def test_09_set_voice_preference(self):
        """09: Test setting and updating voicePreference"""
        session = Session.objects.create(
            user=self.test_user,
            title="Voice Test",
        )

        self.assertIsNone(session.voicePreference)

        session.voicePreference = "male"
        session.save()

        session.refresh_from_db()
        self.assertEqual(session.voicePreference, "male")

    def test_10_set_cover_image_url(self):
        """10: Test setting cover_img_url"""
        session = Session.objects.create(
            user=self.test_user,
            title="Cover Image Test",
        )

        session.cover_img_url = "covers/book_cover.jpg"
        session.save()

        session.refresh_from_db()
        self.assertEqual(session.cover_img_url, "covers/book_cover.jpg")

    def test_11_set_translated_title(self):
        """11: Test setting translated_title"""
        session = Session.objects.create(
            user=self.test_user,
            title="The Alchemist",
        )

        self.assertIsNone(session.translated_title)

        session.translated_title = "연금술사"
        session.save()

        session.refresh_from_db()
        self.assertEqual(session.translated_title, "연금술사")

    # ====================
    # Timestamp Behavior
    # ====================

    def test_12_created_at_auto_set_on_creation(self):
        """12: Test created_at is automatically set when session is created"""
        before_create = timezone.now()
        session = Session.objects.create(
            user=self.test_user,
            title="Timestamp Test",
        )
        after_create = timezone.now()

        self.assertIsNotNone(session.created_at)
        self.assertGreaterEqual(session.created_at, before_create)
        self.assertLessEqual(session.created_at, after_create)

    # ====================
    # String Representation
    # ====================

    def test_13_session_str_representation(self):
        """13: Test __str__ returns 'device_info - title'"""
        session = Session.objects.create(
            user=self.test_user,
            title="1984",
        )

        expected_str = f"{self.test_user.device_info} - 1984"
        self.assertEqual(str(session), expected_str)

    # ====================
    # Querying & Filtering
    # ====================

    def test_14_filter_ongoing_sessions(self):
        """14: Test filtering sessions by isOngoing status"""
        Session.objects.create(user=self.test_user, title="Ongoing 1", isOngoing=True)
        Session.objects.create(user=self.test_user, title="Ongoing 2", isOngoing=True)
        Session.objects.create(
            user=self.test_user,
            title="Finished",
            isOngoing=False,
            ended_at=timezone.now(),
        )

        ongoing_sessions = Session.objects.filter(isOngoing=True)
        finished_sessions = Session.objects.filter(isOngoing=False)

        self.assertEqual(ongoing_sessions.count(), 2)
        self.assertEqual(finished_sessions.count(), 1)
