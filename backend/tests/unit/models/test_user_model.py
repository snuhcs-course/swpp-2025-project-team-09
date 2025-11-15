from django.test import TestCase
from django.utils import timezone
from django.db import IntegrityError
from apis.models.user_model import User
import uuid
import time


class TestUserModel(TestCase):
    """Unit tests for User model - testing User model in isolation"""

    # ====================
    # Basic CRUD Operations
    # ====================

    def test_01_create_user_with_all_fields(self):
        """01: Test creating user with all required fields"""
        user = User.objects.create(
            device_info="SM-G991N",
            language_preference="ko",
        )

        self.assertIsNotNone(user.uid)
        self.assertIsInstance(user.uid, uuid.UUID)
        self.assertEqual(user.device_info, "SM-G991N")
        self.assertEqual(user.language_preference, "ko")
        self.assertIsNotNone(user.created_at)
        self.assertIsNotNone(user.updated_at)

    def test_02_create_user_with_default_language(self):
        """02: Test that language_preference defaults to 'en'"""
        user = User.objects.create(device_info="iPhone-14-Pro")

        self.assertEqual(user.language_preference, "en")

    def test_03_update_user_language_preference(self):
        """03: Test updating user's language preference"""
        user = User.objects.create(device_info="Galaxy-S23", language_preference="en")

        user.language_preference = "ko"
        user.save()

        user.refresh_from_db()
        self.assertEqual(user.language_preference, "ko")

    def test_04_delete_user(self):
        """04: Test deleting a user"""
        user = User.objects.create(device_info="old-device")
        user_id = user.uid

        user.delete()

        with self.assertRaises(User.DoesNotExist):
            User.objects.get(uid=user_id)

    # ====================
    # Field Validation & Constraints
    # ====================

    def test_05_device_info_unique_constraint(self):
        """05: Test that same device cannot register twice"""
        User.objects.create(device_info="SM-A525F", language_preference="en")

        with self.assertRaises(IntegrityError):
            User.objects.create(device_info="SM-A525F", language_preference="ko")

    def test_06_device_info_cannot_be_null(self):
        """06: Test that device_info is required"""
        with self.assertRaises((IntegrityError, ValueError)):
            User.objects.create(device_info=None, language_preference="en")

    def test_07_device_info_with_unicode_characters(self):
        """07: Test device_info supports international characters"""
        unicode_device = "デバイス-기기-设备"
        user = User.objects.create(
            device_info=unicode_device, language_preference="ja"
        )

        self.assertEqual(user.device_info, unicode_device)

    def test_08_uuid_auto_generated_and_unique(self):
        """08: Test each user gets unique auto-generated UUID"""
        user1 = User.objects.create(device_info="device-1", language_preference="en")
        user2 = User.objects.create(device_info="device-2", language_preference="ko")

        self.assertIsInstance(user1.uid, uuid.UUID)
        self.assertIsInstance(user2.uid, uuid.UUID)
        self.assertNotEqual(user1.uid, user2.uid)

    # ====================
    # Timestamp Behavior
    # ====================

    def test_09_created_at_auto_set_on_creation(self):
        """09: Test created_at is automatically set when user is created"""
        before_create = timezone.now()
        user = User.objects.create(device_info="timestamp-device")
        after_create = timezone.now()

        self.assertIsNotNone(user.created_at)
        self.assertGreaterEqual(user.created_at, before_create)
        self.assertLessEqual(user.created_at, after_create)

    def test_10_updated_at_changes_on_save(self):
        """10: Test updated_at is updated when user is modified"""
        user = User.objects.create(device_info="update-device")
        initial_updated_at = user.updated_at

        time.sleep(0.1)
        user.language_preference = "ko"
        user.save()

        user.refresh_from_db()
        self.assertGreater(user.updated_at, initial_updated_at)

    # ====================
    # String Representation
    # ====================

    def test_11_user_str_representation(self):
        """11: Test __str__ returns 'device_info (language_preference)'"""
        user = User.objects.create(device_info="iPhone-15", language_preference="en")

        expected_str = "iPhone-15 (en)"
        self.assertEqual(str(user), expected_str)

    # ====================
    # Multiple Languages Support
    # ====================

    def test_12_multiple_language_preferences(self):
        """12: Test users can be created with various languages"""
        languages = [
            ("device-en", "en"),
            ("device-ko", "ko"),
            ("device-ja", "ja"),
            ("device-zh", "zh"),
        ]

        for device_info, lang in languages:
            user = User.objects.create(
                device_info=device_info, language_preference=lang
            )
            self.assertEqual(user.language_preference, lang)

    # ====================
    # Querying & Filtering
    # ====================

    def test_13_filter_users_by_language(self):
        """13: Test filtering users by language preference"""
        User.objects.create(device_info="device-en-1", language_preference="en")
        User.objects.create(device_info="device-en-2", language_preference="en")
        User.objects.create(device_info="device-ko-1", language_preference="ko")

        en_users = User.objects.filter(language_preference="en")
        ko_users = User.objects.filter(language_preference="ko")

        self.assertEqual(en_users.count(), 2)
        self.assertEqual(ko_users.count(), 1)
