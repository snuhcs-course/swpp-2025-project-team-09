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

    def addBB(self, bbox_list, translated_list, audio_list):
        """
        Add bounding boxes to this page
        
        Args:
            bbox_list: List of bbox dicts with 'text' and coordinate keys
            translated_list: List of translated text strings
            audio_list: List of audio base64 strings
        """
        from apis.models.bb_model import BB
        
        for i, bbox in enumerate(bbox_list):
            original_text = bbox.get('text', '')
            translated_text = translated_list[i] if i < len(translated_list) else ''
            audio_base64 = audio_list[i] if i < len(audio_list) else ''
            
            # Extract coordinates from bbox
            coordinates = {
                'x1': bbox.get('x1', 0),
                'y1': bbox.get('y1', 0),
                'x2': bbox.get('x2', 0),
                'y2': bbox.get('y2', 0),
                'x3': bbox.get('x3', 0),
                'y3': bbox.get('y3', 0),
                'x4': bbox.get('x4', 0),
                'y4': bbox.get('y4', 0),
            }
            
            BB.objects.create(
                page=self,
                original_text=original_text,
                translated_text=translated_text,
                audio_base64=audio_base64,
                coordinates=coordinates
            )