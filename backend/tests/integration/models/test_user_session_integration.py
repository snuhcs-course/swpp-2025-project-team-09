from django.test import TestCase
from apis.models.user_model import User
from apis.models.session_model import Session
import uuid
from django.db.utils import IntegrityError


class TestUserSessionIntegration(TestCase):
    """Integration tests for User and Session model interactions"""

    def setUp(self):
        """Create a default test user before each test"""
        self.default_user = User.objects.create(device_info="default-user")

    # -----------------------
    # Basic get/create session tests
    # -----------------------

    def test_01_user_get_sessions_empty(self):
        """01: getSessions returns an empty queryset when user has no sessions"""
        user = User.objects.create(device_info="test-empty-sessions")
        sessions = user.getSessions()
        self.assertEqual(sessions.count(), 0)
        self.assertFalse(sessions.exists())

    def test_02_user_get_sessions_with_sessions(self):
        """02: getSessions returns all sessions for a user"""
        user = self.default_user
        session1 = Session.objects.create(user=user, title="Book 1")
        session2 = Session.objects.create(user=user, title="Book 2")

        session_set = user.getSessions()
        self.assertEqual(session_set.count(), 2)
        self.assertIn(session1, session_set)
        self.assertIn(session2, session_set)

    # -----------------------
    # Session deletion tests
    # -----------------------

    def test_03_user_delete_session_success(self):
        """03: deleteSession removes the correct session"""
        user = self.default_user
        session1 = Session.objects.create(user=user, title="Book to Delete")
        session2 = Session.objects.create(user=user, title="Book to Keep")

        result = user.deleteSession(session1.id)

        # Verify deletion
        self.assertEqual(result[0], 1)
        remaining_sessions = user.getSessions()
        self.assertEqual(remaining_sessions.count(), 1)
        self.assertNotIn(session1.id, [s.id for s in remaining_sessions])
        self.assertIn(session2.id, [s.id for s in remaining_sessions])

    def test_04_user_delete_session_nonexistent(self):
        """04: deleteSession with a non-existent session ID returns 0 deletions"""
        fake_uuid = uuid.uuid4()
        result = self.default_user.deleteSession(fake_uuid)
        self.assertEqual(result[0], 0)

    # -----------------------
    # User deletion and cascade tests
    # -----------------------

    def test_05_user_cascade_delete_sessions(self):
        """05: Deleting a user cascades and deletes all their sessions"""
        user = User.objects.create(device_info="cascade-user")
        Session.objects.create(user=user, title="Session 1")
        Session.objects.create(user=user, title="Session 2")

        self.assertEqual(Session.objects.filter(user=user).count(), 2)

        user.delete()

        self.assertEqual(
            Session.objects.filter(user__device_info="cascade-user").count(), 0
        )

    def test_06_user_deletion_cascade_comprehensive(self):
        """06: Verify cascading deletion of user and its sessions"""
        user = User.objects.create(device_info="cascade-delete-test")
        Session.objects.create(user=user, title="Test Session")
        self.assertEqual(Session.objects.filter(user=user).count(), 1)

        user_id = user.uid
        user.delete()

        # Verify user deletion
        with self.assertRaises(User.DoesNotExist):
            User.objects.get(uid=user_id)

        # Verify sessions cascade deleted
        self.assertEqual(
            Session.objects.filter(user__device_info="cascade-delete-test").count(), 0
        )

    # -----------------------
    # Relationship integrity tests
    # -----------------------

    def test_07_user_session_relationship(self):
        """07: Sessions created via user relationship belong to the correct user"""
        user = self.default_user
        session1 = user.sessions.create(title="Book 1")
        session2 = user.sessions.create(title="Book 2")

        self.assertEqual(session1.user, user)
        self.assertEqual(session2.user, user)
        self.assertEqual(user.sessions.count(), 2)

    def test_08_multiple_users_multiple_sessions(self):
        """08: Multiple users maintain their own sessions independently"""
        user1 = User.objects.create(device_info="user-1", language_preference="en")
        user2 = User.objects.create(device_info="user-2", language_preference="ko")

        Session.objects.create(user=user1, title="User1 Book1")
        Session.objects.create(user=user1, title="User1 Book2")
        Session.objects.create(user=user2, title="User2 Book1")

        self.assertEqual(user1.sessions.count(), 2)
        self.assertEqual(user2.sessions.count(), 1)

        # Deleting user1 should not affect user2
        user1.delete()
        self.assertEqual(user2.sessions.count(), 1)

    # -----------------------
    # Foreign key and integrity tests
    # -----------------------

    def test_09_session_foreign_key_constraint(self):
        """09: Session requires a valid User"""
        user = self.default_user

        # Valid session
        session = Session.objects.create(user=user, title="Valid Session")
        self.assertEqual(session.user, user)
        self.assertIn(session, user.sessions.all())

        # Invalid session: user=None should raise IntegrityError
        with self.assertRaises(IntegrityError):
            Session.objects.create(user=None, title="Invalid Session")
