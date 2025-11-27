from django.db import models
from django.utils import timezone
from apis.models.user_model import User
import uuid


class Session(models.Model):
    """
    Session entity
    - User's reading session (unit: book)
    """

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    user = models.ForeignKey(User, on_delete=models.CASCADE, related_name="sessions")
    title = models.CharField(max_length=255)
    translated_title = models.CharField(max_length=255, null=True, blank=True)
    cover_img_url = models.TextField(null=True, blank=True)
    created_at = models.DateTimeField(default=timezone.now)
    started_at = models.DateTimeField(default=timezone.now)
    ended_at = models.DateTimeField(null=True, blank=True)
    isOngoing = models.BooleanField(default=True)
    totalPages = models.IntegerField(default=0)
    totalWords = models.IntegerField(default=0)
    voicePreference = models.CharField(
        max_length=50, default=None, null=True, blank=True
    )

    def __str__(self):
        return f"{self.user.device_info} - {self.title}"

    def getPages(self):
        """Returns all pages belonging to the current session"""
        return self.pages.all()