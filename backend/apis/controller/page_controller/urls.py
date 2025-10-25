from django.urls import path
from .views import PageGetImageView, PageGetOCRView, PageGetTTSView , PageGetTTSFrontView

urlpatterns = [
    path("get_image/", PageGetImageView.as_view()),
    path("get_ocr/", PageGetOCRView.as_view()),
    path("get_tts/", PageGetTTSView.as_view()),
    path("get_tts_front/", PageGetTTSFrontView.as_view()),
]
