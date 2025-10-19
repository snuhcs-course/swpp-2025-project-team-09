from django.db import models
from apis.models.page_model import Page

class BB(models.Model):
    """
    Bounding Box (텍스트 박스 단위)
    - OCR 결과를 세분화하여 문장별/문단별 관리
    """

    page = models.ForeignKey(Page, on_delete=models.CASCADE, related_name="bbs")
    original_text = models.TextField()
    translated_text = models.TextField(null=True, blank=True)
    audio_base64 = models.JSONField(default=list, blank=True)
    coordinates = models.JSONField(default=dict, blank=True)

    def __str__(self):
        return f"BB of Page {self.page.id}"

    def updateTranslation(self, new_text):
        self.translated_text = new_text
        self.save()

    def updateAudio(self, new_audio_base64):
        self.audio_base64 = new_audio_base64
        self.save()

    def updatePosition(self, new_position):
        if not isinstance(self.coordinates, dict):
            self.coordinates = {}
        self.coordinates.update(new_position)
        self.save()

    @property
    def points(self):
        return [
            (self.coordinates.get("x1"), self.coordinates.get("y1")),
            (self.coordinates.get("x2"), self.coordinates.get("y2")),
            (self.coordinates.get("x3"), self.coordinates.get("y3")),
            (self.coordinates.get("x4"), self.coordinates.get("y4")),
        ]
