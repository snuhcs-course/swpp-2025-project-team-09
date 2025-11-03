from django.urls import path
from .views import (
    ProcessUploadCoverView,
    ProcessUploadView,
    CheckOCRStatusView,
    CheckTTSStatusView,
)

urlpatterns = [
    path("upload_cover/", ProcessUploadCoverView.as_view()),
    path("upload/", ProcessUploadView.as_view()),
    path("check_ocr/", CheckOCRStatusView.as_view()),
    path("check_tts/", CheckTTSStatusView.as_view()),
]
