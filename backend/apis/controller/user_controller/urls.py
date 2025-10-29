from django.urls import path
from .views import UserRegisterView, UserLoginView, UserChangeLangView, UserInfoView

urlpatterns = [
    path("register", UserRegisterView.as_view()),
    path("login", UserLoginView.as_view()),
    path("lang", UserChangeLangView.as_view()),
    path("info", UserInfoView.as_view()),
]
