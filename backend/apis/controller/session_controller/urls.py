from django.urls import path
from .views import (
    StartSessionView,
    SelectVoiceView,
    EndSessionView,
    GetSessionInfoView,
    GetSessionStatsView,
    SessionReviewView,
    SessionReloadView,
    SessionReloadAllView,
    DiscardSessionView,
)

urlpatterns = [
    path("start", StartSessionView.as_view()),
    path("voice", SelectVoiceView.as_view()),
    path("end", EndSessionView.as_view()),
    path("info", GetSessionInfoView.as_view()),
    path("stats", GetSessionStatsView.as_view()),
    path("review", SessionReviewView.as_view()),
    path("reload", SessionReloadView.as_view()),
    path("reload_all", SessionReloadAllView.as_view()),
    path("discard", DiscardSessionView.as_view()),
]
