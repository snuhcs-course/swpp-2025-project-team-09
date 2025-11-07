from django.db import models
import uuid
from django.utils import timezone


class User(models.Model):
    """
    User entity
    - 앱 최초 실행 시 단말 등록
    - 언어 설정 관리
    - 세션/책 관계 연결
    """

    uid = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    device_info = models.CharField(max_length=255, unique=True)
    language_preference = models.CharField(max_length=20, default="en")
    created_at = models.DateTimeField(default=timezone.now)
    updated_at = models.DateTimeField(auto_now=True)

    def __str__(self):
        return f"{self.device_info} ({self.language_preference})"

    # 관계 기반 메서드
    def getSessions(self):
        """현재 유저가 생성한 모든 세션 반환"""
        return self.sessions.all()

    def deleteSession(self, session_id: int):
        """특정 세션 삭제"""
        return self.sessions.filter(id=session_id).delete()

    class Meta:
        # Use the table name that exists in the current sqlite database.
        # Migration files show the DB contains `apis_user`, so align the model
        # to avoid "no such table: user" OperationalError at runtime.
        db_table = "apis_user"
