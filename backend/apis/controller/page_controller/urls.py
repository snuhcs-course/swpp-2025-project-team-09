from django.urls import path
from .views import PageGetImageView, PageGetOCRView, PageGetTTSView

urlpatterns = [
    path("get_image", PageGetImageView.as_view()),
    path("get_ocr", PageGetOCRView.as_view()),
    path("get_tts", PageGetTTSView.as_view()),
]
