from django.db import models
from apis.models.page_model import Page


class BB(models.Model):
    """
    Bounding Box (Text Box Unit)
    - OCR results are saved at the sentence/paragraph level.
    """

    page = models.ForeignKey(Page, on_delete=models.CASCADE, related_name="bbs")
    original_text = models.TextField()
    translated_text = models.TextField(null=True, blank=True)
    audio_base64 = models.JSONField(default=list, blank=True)
    coordinates = models.JSONField(default=dict, blank=True)
    tts_status = models.CharField(
        max_length=20,
        default="pending",
        choices=[
            ("pending", "Pending"),
            ("processing", "Processing"),
            ("ready", "Ready"),
            ("failed", "Failed"),
        ],
    )

    def __str__(self):
        return f"BB of Page {self.page.id}"
