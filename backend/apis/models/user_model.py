from django.db import models
import uuid
from django.utils import timezone


class User(models.Model):
    """
    User entity
    - Device registration upon initial app launch
    - Language settings management
    - Session/book relationship association
    """

    uid = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    device_info = models.CharField(max_length=255, unique=True)
    language_preference = models.CharField(max_length=20, default="en")
    created_at = models.DateTimeField(default=timezone.now)
    updated_at = models.DateTimeField(auto_now=True)

    def __str__(self):
        return f"{self.device_info} ({self.language_preference})"

    class Meta:
        """
        Use the table name that exists in the current sqlite database.
        - Migration files show the DB contains `apis_user`, so align the model
        - to avoid "no such table: user" OperationalError at runtime.
        """

        db_table = "apis_user"
