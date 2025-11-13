from django.db import models
from django.utils import timezone
from apis.models.user_model import User
import uuid


class Session(models.Model):
    """
    Session entity
    - 사용자의 읽기 세션(책 단위)
    """

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    user = models.ForeignKey(User, on_delete=models.CASCADE, related_name="sessions")
    title = models.CharField(max_length=255)
    translated_title = models.CharField(max_length=255, null=True, blank=True)
    cover_img_url = models.TextField(null=True, blank=True)
    created_at = models.DateTimeField(default=timezone.now)
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
        """현재 세션에 속한 모든 페이지를 반환"""
        return self.pages.all()

    def addPage(self, image_url, index):
        """페이지 추가용 헬퍼 메서드"""
        from apis.models.page_model import Page

        Page.objects.create(session=self, img_url=image_url)
