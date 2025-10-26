from django.urls import path
from .views import ProcessUploadFrontPageView, ProcessUploadView, CheckOCRStatusView, CheckTTSStatusView

urlpatterns = [
    path("upload_front/", ProcessUploadFrontPageView.as_view()),
    path("upload/", ProcessUploadView.as_view()),
    path("check_ocr/", CheckOCRStatusView.as_view()),
    path("check_tts/", CheckTTSStatusView.as_view()),
]
