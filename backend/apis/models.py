from django.db import models

class User(models.Model):
    """
    Represents an application user.

    Attributes
    ----------
    uid : CharField
        Unique identifier for the user (maximum length: 64).
    language : CharField
        Target language for translation ('en' for English, 'vi' for Vietnamese).
    created_at : DateTimeField
        Timestamp automatically set when the user is first created.
    """
    uid = models.CharField(max_length=64, unique=True)
    language = models.CharField(max_length=2, default="en")
    created_at = models.DateTimeField(auto_now_add=True)

    def __str__(self):
        return self.uid

class Book(models.Model):
    """
    Represents a single picture book uploaded by a user.

    Attributes
    ----------
    user : ForeignKey
        Reference to the user who owns this book. If the user is deleted,
        all related books are also removed (CASCADE).
    title : CharField
        Title of the book.
    cover_image : CharField
        Optional path or URL to the book's cover image.
    created_at : DateTimeField
        Timestamp automatically set when the book is first created.
    """
    user = models.ForeignKey(User, on_delete=models.CASCADE, related_name="books")
    title = models.CharField(max_length=255)
    cover_image = models.CharField(max_length=255, blank=True, null=True)
    created_at = models.DateTimeField(auto_now_add=True)

    def __str__(self):
        return f"{self.title} ({self.user.uid})"

    def get_pages(self):
        # Return all Page instances associated with this book.
        return self.pages.all()


class Page(models.Model):
    """
    Represents a single page within a book.

    Attributes
    ----------
    book : ForeignKey
        Reference to the parent Book. Deleting the book removes all its pages (CASCADE).
    image_url : CharField
        Path or URL to the page image.
    audio_url : CharField
        Path or URL to the generated TTS audio for this page.
    translation_text : TextField
        Translated text extracted from OCR and translation processing.
    bbox_json : JSONField
        Bounding box information for OCR-detected text regions, stored as JSON.
    created_at : DateTimeField
        Timestamp automatically set when the page is first created.
    """
    book = models.ForeignKey(Book, on_delete=models.CASCADE, related_name="pages")
    image_url = models.CharField(max_length=255)
    audio_url = models.CharField(max_length=255)
    translation_text = models.TextField()
    bbox_json = models.JSONField(default=list)
    created_at = models.DateTimeField(auto_now_add=True)

    def __str__(self):
        return f"Page {self.id} of {self.book.title}"

    def update_translation(self, text):
        # Update and save the translation text for this page.
        self.translation_text = text
        self.save(update_fields=["translation_text"])

    def update_audio(self, url):
        # Update and save the audio URL for this page.
        self.audio_url = url
        self.save(update_fields=["audio_url"])
