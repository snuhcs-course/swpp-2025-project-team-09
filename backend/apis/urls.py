from django.urls import path
from .views import ProcessImageView, BookListView, PageListView

urlpatterns = [
    # /apis/process_image/로 들어오면 .views 내부 process_image() 실행
    path("process_image/", ProcessImageView.as_view(), name="process_image"), 
    path("books/<str:user_uid>/", BookListView.as_view(), name="book_list"),
    path("pages/<int:book_id>/", PageListView.as_view(), name="page_list"),
]
