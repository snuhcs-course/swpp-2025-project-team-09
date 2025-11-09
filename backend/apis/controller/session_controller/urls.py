from django.urls import path
from .views import (
    StartSessionView,
    SelectVoiceView,
    EndSessionView,
    GetSessionInfoView,
    GetSessionStatsView,
    SessionReviewView,
)

urlpatterns = [
    path("start", StartSessionView.as_view()),
    path("voice", SelectVoiceView.as_view()),
    path("end", EndSessionView.as_view()),
    path("info", GetSessionInfoView.as_view()),
    path("stats", GetSessionStatsView.as_view()),
    path("review", SessionReviewView.as_view()),
    path("reload_all", SessionReloadAllView.as_view()),
]
