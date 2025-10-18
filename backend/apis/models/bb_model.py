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
    # TODO: Add position fields (x, y, width, height) as needed
    #position = models.JSONField(default=dict, blank=True)

    def __str__(self):
        return f"BB of Page {self.page.id}"

    def updateTranslation(self, new_text):
        self.translated_text = new_text
        self.save()

    def updateAudio(self, new_audio_base64):
        self.audio_base64 = new_audio_base64
        self.save()

    def updatePosition(self, new_position):
        self.position = new_position
        self.save()
