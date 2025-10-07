from django.db import models


class User(models.Model):
    """앱 사용자 식별용 (로그인 없이 UUID로만 관리 가능)"""
    uid = models.CharField(max_length=64, unique=True)
    nickname = models.CharField(max_length=100, blank=True)
    language = models.CharField(max_length=10, default="en")
    created_at = models.DateTimeField(auto_now_add=True)

    def __str__(self):
        return self.nickname or self.uid


class Book(models.Model):
    """책 단위 (한 사용자에 속함)"""
    user = models.ForeignKey(User, on_delete=models.CASCADE, related_name="books")
    title = models.CharField(max_length=255)
    cover_image = models.CharField(max_length=255, blank=True, null=True)
    created_at = models.DateTimeField(auto_now_add=True)

    def __str__(self):
        return f"{self.title} ({self.user.uid})"

    def get_pages(self):
        return self.pages.all()


class Page(models.Model):
    """OCR + TTS 결과 저장용"""
    book = models.ForeignKey(Book, on_delete=models.CASCADE, related_name="pages")
    image_url = models.CharField(max_length=255)
    audio_url = models.CharField(max_length=255)
    translation_text = models.TextField()
    bbox_json = models.JSONField(default=list)
    created_at = models.DateTimeField(auto_now_add=True)

    def __str__(self):
        return f"Page {self.id} of {self.book.title}"

    def update_translation(self, text):
        self.translation_text = text
        self.save(update_fields=["translation_text"])

    def update_audio(self, url):
        self.audio_url = url
        self.save(update_fields=["audio_url"])
