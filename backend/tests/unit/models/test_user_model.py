from django.test import TestCase
from django.utils import timezone
from django.db import IntegrityError
from apis.models.user_model import User
from apis.models.session_model import Session
import uuid
import time


class TestUserModel(TestCase):
    """Unit tests for User model"""

    def test_01_create_user_success(self):
        """Test successful user creation"""
        user = User.objects.create(
            device_info="test-device-123",
            language_preference="en",
            created_at=timezone.now(),
        )

        self.assertIsNotNone(user.uid)
        self.assertEqual(user.device_info, "test-device-123")
        self.assertEqual(user.language_preference, "en")
        self.assertIsNotNone(user.created_at)
        self.assertIsNotNone(user.updated_at)

    def test_02_user_uid_auto_generation(self):
        """Test that UUID is automatically generated"""
        user = User.objects.create(
            device_info="test-device-456", language_preference="ko"
        )

        self.assertIsInstance(user.uid, uuid.UUID)
        self.assertIsNotNone(user.uid)

    def test_03_user_language_preference_default(self):
        """Test language_preference default value"""
        user = User.objects.create(device_info="test-device-789")

        self.assertEqual(user.language_preference, "en")

    def test_04_user_device_info_unique_constraint(self):
        """Test device_info unique constraint"""
        User.objects.create(device_info="duplicate-device", language_preference="en")

        with self.assertRaises(IntegrityError):
            User.objects.create(
                device_info="duplicate-device", language_preference="ko"
            )

    def test_05_user_created_at_auto_set(self):
        """Test created_at is automatically set"""
        before_create = timezone.now()
        user = User.objects.create(device_info="test-device-time")
        after_create = timezone.now()

        self.assertIsNotNone(user.created_at)
        self.assertGreaterEqual(user.created_at, before_create)
        self.assertLessEqual(user.created_at, after_create)

    def test_06_user_updated_at_auto_update(self):
        """Test updated_at is automatically updated"""
        user = User.objects.create(device_info="test-device-update")
        initial_updated_at = user.updated_at

        # Wait a bit and update
        time.sleep(0.1)
        user.language_preference = "ko"
        user.save()

        user.refresh_from_db()
        self.assertGreater(user.updated_at, initial_updated_at)

    def test_07_user_str_representation(self):
        """Test __str__ method"""
        user = User.objects.create(
            device_info="test-device-str", language_preference="en"
        )

        expected_str = "test-device-str (en)"
        self.assertEqual(str(user), expected_str)

    def test_08_get_sessions_empty(self):
        """Test getSessions with no sessions"""
        user = User.objects.create(device_info="test-device-no-sessions")

        session_set = user.getSessions()
        self.assertEqual(session_set.count(), 0)

    def test_09_get_sessions_with_sessions(self):
        """Test getSessions with multiple sessions"""
        user = User.objects.create(device_info="test-device-with-sessions")

        # Create sessions
        session1 = Session.objects.create(
            user=user, title="Book 1", created_at=timezone.now()
        )
        session2 = Session.objects.create(
            user=user, title="Book 2", created_at=timezone.now()
        )

        session_set = user.getSessions()
        self.assertEqual(session_set.count(), 2)
        self.assertIn(session1, session_set)
        self.assertIn(session2, session_set)

    def test_10_delete_session_success(self):
        """Test deleteSession method"""
        user = User.objects.create(device_info="test-device-delete-session")

        # Create sessions
        session1 = Session.objects.create(
            user=user, title="Book to Delete", created_at=timezone.now()
        )
        session2 = Session.objects.create(
            user=user, title="Book to Keep", created_at=timezone.now()
        )

        # Delete session1
        result = user.deleteSession(session1.id)

        # Verify deletion
        self.assertEqual(result[0], 1)
        remaining_sessions = user.getSessions()
        self.assertEqual(remaining_sessions.count(), 1)
        self.assertNotIn(session1.id, [s.id for s in remaining_sessions])
        self.assertIn(session2.id, [s.id for s in remaining_sessions])

    def test_11_delete_session_nonexistent(self):
        """Test deleteSession with non-existent session"""
        user = User.objects.create(device_info="test-device-delete-nonexistent")

        # Try to delete non-existent session
        fake_uuid = uuid.uuid4()
        result = user.deleteSession(fake_uuid)

        # Should return 0 deletions
        self.assertEqual(result[0], 0)

    def test_12_user_cascade_delete_sessions(self):
        """Test that deleting user cascades to sessions"""
        user = User.objects.create(device_info="test-device-cascade")

        # Create sessions
        Session.objects.create(user=user, title="Session 1", created_at=timezone.now())
        Session.objects.create(user=user, title="Session 2", created_at=timezone.now())

        session_count_before = Session.objects.filter(user=user).count()
        self.assertEqual(session_count_before, 2)

        # Delete user
        user.delete()

        # Verify sessions were deleted
        session_count_after = Session.objects.filter(
            user__device_info="test-device-cascade"
        ).count()
        self.assertEqual(session_count_after, 0)

    def test_13_multiple_users_creation(self):
        """Test creating multiple users"""
        user1 = User.objects.create(device_info="device-1", language_preference="en")
        user2 = User.objects.create(device_info="device-2", language_preference="ko")
        user3 = User.objects.create(device_info="device-3", language_preference="ja")

        self.assertEqual(User.objects.count(), 3)
        self.assertNotEqual(user1.uid, user2.uid)
        self.assertNotEqual(user2.uid, user3.uid)
        self.assertNotEqual(user1.uid, user3.uid)

    def test_14_user_update_language_preference(self):
        """Test updating language preference"""
        user = User.objects.create(
            device_info="test-device-lang-update", language_preference="en"
        )

        self.assertEqual(user.language_preference, "en")

        user.language_preference = "ko"
        user.save()

        user.refresh_from_db()
        self.assertEqual(user.language_preference, "ko")

    def test_15_user_query_by_device_info(self):
        """Test querying user by device_info"""
        User.objects.create(device_info="queryable-device", language_preference="en")

        user = User.objects.get(device_info="queryable-device")
        self.assertEqual(user.device_info, "queryable-device")
        self.assertEqual(user.language_preference, "en")
