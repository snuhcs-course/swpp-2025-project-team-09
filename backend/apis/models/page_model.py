from django.db import models
from django.utils import timezone
from apis.models.session_model import Session
import json

class Page(models.Model):
    """
    Page entity
    - OCR, Translation, TTS 결과를 한 페이지 단위로 관리
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
        """해당 페이지의 BoundingBox 리스트 반환"""
        return self.bbs.all()

    def addBB(self, bbox_list, translated_list, audio_list):
        """
        Bounding Box 데이터 추가
        bbox_list: [{x, y, width, height, text}, ...]
        translated_list: 번역문 리스트 (동일 인덱스 기준)
        audio_list: base64 또는 경로 리스트
        """
        from apis.models.bb_model import BB
        for i, bbox in enumerate(bbox_list):
            BB.objects.create(
                page=self,
                original_text=bbox.get("text", ""),
                translated_text=translated_list[i] if i < len(translated_list) else "",
                audio_base64=audio_list[i] if i < len(audio_list) else "",
                coordinates={
                    "x1": bbox.get("x1"),
                    "y1": bbox.get("y1"),
                    "x2": bbox.get("x2"),
                    "y2": bbox.get("y2"),
                    "x3": bbox.get("x3"),
                    "y3": bbox.get("y3"),
                    "x4": bbox.get("x4"),
                    "y4": bbox.get("y4"),
                }
            )
