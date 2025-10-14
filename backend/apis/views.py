from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework import status
from django.core.files.storage import default_storage
from django.core.files.base import ContentFile
from django.db import transaction
from .models import User, Book, Page
from .ocr_tts import OCR2TTS
import os
import uuid

# TODO: Separate classes into files if necessary for code brevity (e.g. apis/process_image/views, apis/book_list/views, etc.)

class ProcessImageView(APIView):
    """
    API endpoint for OCR and TTS generation from uploaded images.

    This view handles:
    - Image upload and local storage.
    - Validation of user ID and book title.
    - OCR2TTS pipeline for text extraction, translation, and audio generation.
    - Saving processed results (image, audio, bounding boxes) to the database.
    - Returning processed data to the client as JSON response.
    """

    @transaction.atomic
    def post(self, request):
        """
        Process a single uploaded image to generate translated text and TTS audio.

        Attributes
        ----------
        request : rest_framework.request.Request
            - The incoming HTTP request containing:
                - uid (str): Unique user ID.
                - lang (str): Target translation language (e.g., 'en', 'vi').
                - book_title (str): Title of the associated book.
                - image_file (File): Uploaded image file to process.

        Returns
        -------
        rest_framework.response.Response
        - HTTP 201 : Success. Returns JSON containing book info, image/audio URLs, translation text, bbox data, and timestamp.
        - HTTP 400 : Missing or invalid parameters.
        - HTTP 404 : User not found.
        - HTTP 500 : OCR/TTS processing failed or unexpected server error.
        """
        try:
            uid = request.data.get('uid')
            lang = request.data.get('lang')
            book_title = request.data.get('book_title')
            image_file = request.data.get('image_file')

            
            ''' temporary delete for API test ----------------------------
            
            missing_params = []
            if not uid:
                missing_params.append('uid')
            if not lang:
                missing_params.append('lang')
            if not book_title:
                missing_params.append('book_title')
            if not image_file:
                missing_params.append('image_file')

            if missing_params:
                return Response(
                    {"detail": f"Missing required parameters: {', '.join(missing_params)}."},
                    status=status.HTTP_400_BAD_REQUEST
                )
            ---------------------------------------------------------- ''' 

            try:
                user = User.objects.get(uid=uid)
            except User.DoesNotExist:
                return Response(
                    {"detail": f"User with uid '{uid}' not found."},
                    status=status.HTTP_404_NOT_FOUND
                )
            
            book, _ = Book.objects.get_or_create(
                user=user,
                title=book_title
            )

            # Save image that user scanned
            file_ext = os.path.splitext(image_file.name)[1]
            image_filename = f"{uuid.uuid4().hex}{file_ext}"
            image_path = default_storage.save(f"images/{user.uid}/{image_filename}", image_file)
            abs_image_path = default_storage.path(image_path)
            print(">>> Saved image path:", abs_image_path)
            print(">>> File exists:", os.path.exists(abs_image_path))

            # Do ocr and make translated text and tts audio file
            print(">>> entered OCR2TTS")
            orctts = OCR2TTS()
            print(">>> OCR2TTS created")
            result = orctts.process_image(abs_image_path, target_lang=lang)
            print(">>> OCR done")

            # If there is no result, response is regarded as error
            if not result or "translation_text" not in result or "audio_file" not in result:
                default_storage.delete(image_path)
                return Response(
                    {"detail": "OCR and TTS processing failed to produce required results."},
                    status=status.HTTP_500_INTERNAL_SERVER_ERROR
                )

            # save audio file and get audio file path
            print(">>> entered audio file save ")
            audio_filename = f"{uuid.uuid4().hex}.mp3"
            audio_buffer = result["audio_file"]
            audio_buffer.seek(0)
            audio_content = ContentFile(audio_buffer.read())
            audio_filename = f"{uuid.uuid4().hex}.mp3"
            audio_path = default_storage.save(f"audio/{user.uid}/{audio_filename}", audio_content)
            print(">>> Audio save done")

            # Create page object and 
            page = Page.objects.create(
                book=book,
                image_url=default_storage.url(image_path),
                audio_url=default_storage.url(audio_path),
                translation_text=result["translation_text"],
                bbox_json=result.get("bbox_results", "{}")
            )
            
            # Only retrieve bounding box informations from original bbox_json
            raw_bbox = page.bbox_json
            if isinstance(raw_bbox, str):
                try: bbox_full = json.loads(raw_bbox)
                except json.JSONDecodeError: bbox_full = {}
            else: bbox_full = raw_bbox
            
            bbox_data = []
            try:
                images = bbox_full.get("images", [])
                if isinstance(images, list) and len(images) > 0:
                    bbox_data = images[0].get("fields", [])
            except Exception: bbox_data = []

            # give response including image url, audio url, and translation text
            return Response({
                "book_title": book.title,
                "image_url": page.image_url,
                "audio_url": page.audio_url,
                "translation_text": page.translation_text,
                "bbox_json": bbox_data,
                "created_at": page.created_at,
            }, status=status.HTTP_201_CREATED)

        # If image processing is not suceed, show an error massage
        except Exception as e:
            return Response(
                {"detail": f"Unexpected server error: {str(e)}"},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR
            )

class BookListView(APIView):
    """
    API endpoint for retrieving a user's book list.
    """

    def get(self, request, user_uid):
        """
        Retrieve all books associated with a specific user.

        Args:
            request (rest_framework.request.Request): The incoming HTTP GET request.
            user_uid (str): The unique ID of the user.

        Returns:
            rest_framework.response.Response:
                - HTTP 200: JSON list of books for the user.
                - HTTP 404: User not found or no books available.
        """
        # TODO: Write get function
        pass

class PageListView(APIView):
    """
    API endpoint for retrieving all pages in a given book.
    """

    def get(self, request, book_id):
        """
        Retrieve all pages belonging to a specific book.

        Args:
            request (rest_framework.request.Request): The incoming HTTP GET request.
            book_id (int): The ID of the book.

        Returns:
            rest_framework.response.Response:
                - HTTP 200: JSON list of pages for the given book.
                - HTTP 404: Book not found or no pages available.
        """
        # TODO: Write get function
        pass
