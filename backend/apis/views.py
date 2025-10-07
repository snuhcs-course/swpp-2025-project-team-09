from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework import status
from django.core.files.storage import default_storage
from .models import User, Book, Page
from .ocr_tts import OCR2TTS

class ProcessImageView(APIView):
    def post(self, request):
        try:
            # 요청 데이터 받기: uid, lang, (book_title), image_file
                # image file 없으면 error
            
            # 사용자 및 책 확인
            
            # 이미지 저장
            image_path = default_storage.save(f"images/{image_file.name}", image_file)
            abs_image_path = default_storage.path(image_path)
            
            # OCR2TTS
            orctts = OCR2TTS
            
            # 결과 json
            result = orctts.process_image(abs_image_path, target_lang = lang)
            
            # 오디오 파일 저장
            image_path = default_storage.save(f"audio/{image_file.name}.mp3", result["audio_file"])
            
            # 페이지 생성
            page = Page.objects.create(
                book=book,
                image_url=default_storage.url(image_path),
                audio_url=default_storage.url(audio_path),
                translation_text=result["translation_text"],
                bbox_json=result["bbox_results"]
            )
            
            return Response({
                "book_title": book.book_title,
                "image_url": page.image_url,
                "audio_url": page.audio_url,
                "translation_txt": page.translation_txt,
                "bbox_results": page.bbox_results,
                "created_at": page.created_at,
            }, status = status.HTTP_201_CREATED)
            
        except Exception as e:
            # 에러 처리
            pass

class BookListView(APIView): #TBD
    def get(self, request, user_uid):
        # 사용자별 책 목록 조회
        pass
        
class PageListView(APIView): #TBD
    def get(self, request, book_id):
        # 특정 책의 페이지 리스트 조회회
        pass