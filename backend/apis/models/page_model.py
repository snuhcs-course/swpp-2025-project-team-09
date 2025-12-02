from django.db import models
from django.utils import timezone
from apis.models.session_model import Session


class Page(models.Model):
    """
    Page entity
    - Manages OCR, Translation, and TTS results per page
    """

    id = models.AutoField(primary_key=True)
    session = models.ForeignKey(Session, on_delete=models.CASCADE, related_name="pages")
    img_url = models.TextField(null=True, blank=True)
    audio_url = models.TextField(null=True, blank=True)
    translation_text = models.TextField(null=True, blank=True)
    bbox_json = models.JSONField(default=dict, blank=True)
    created_at = models.DateTimeField(default=timezone.now)

    def __str__(self):
        return f"Page {self.id} of Session {self.session.id}"

    def getBBs(self):
        """Returns all bounding boxes for this page"""
        return self.bbs.all()