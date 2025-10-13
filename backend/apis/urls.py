from django.urls import path
from .views import ProcessImageView as process_image_view
from .views import BookListView as book_list_view
from .views import PageListView as page_list_view

# URL routing for StoryBridge API endpoints
urlpatterns = [
    # POST /api/process_image/ : OCR → Translation → TTS pipeline for uploaded image
    path('api/process_image/', process_image_view.as_view(), name='process_image'),

    # GET /api/books/<str:user_uid>/ : Retrieve list of books for a given user
    path('api/books/<str:user_uid>/', book_list_view.as_view(), name='book_list'),

    # GET /api/pages/<int:book_id>/ : Retrieve list of pages belonging to a book
    path('api/pages/<int:book_id>/', page_list_view.as_view(), name='page_list'),
]