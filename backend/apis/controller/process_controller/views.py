from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework import status
from django.utils import timezone
from apis.models.session_model import Session
from apis.models.page_model import Page
from apis.models.bb_model import BB
from apis.modules.OCRprocessor import OCRModule
from apis.modules.tts_module import TTSModule
import base64
import io
import json
import uuid
import os

class ProcessUploadView(APIView):
    """
    [POST] /process/upload
    - OCR → Translation → TTS 순서로 실행
    
    Endpoint: /process/upload

    - Request (POST)

        {
        "session_id": "string",
        "page_index": integer,
        "lang": "string",
        "image_base64": "string"
        }
        
    - Response

        Status: 200 OK

        {
        "session_id": "string",
        "page_index": integer,
        "status": "string",
        "submitted_at": "datetime"
        }
    """

    def post(self, request):
        session_id = request.data.get("session_id")
        page_index = request.data.get("page_index")
        lang = request.data.get("lang")
        image_base64 = request.data.get("image_base64")

        if not all([session_id, page_index, lang, image_base64]):
            return Response(status=status.HTTP_400_BAD_REQUEST)

        try:
            session = Session.objects.get(id=session_id)
        except Session.DoesNotExist:
            return Response(status=status.HTTP_404_NOT_FOUND)

        # 이미지 디코딩 및 저장
        image_bytes = base64.b64decode(image_base64)
        image_filename = f"{uuid.uuid4().hex}.jpg"
        image_path = f"media/images/{session_id}_{page_index}_{image_filename}"
        os.makedirs(os.path.dirname(image_path), exist_ok=True)
        with open(image_path, "wb") as f:
            f.write(image_bytes)

        # OCR 단계
        ocr = OCRModule()
        ocr_result = ocr.processImage(image_path)
        if not ocr_result or "fields" not in ocr_result:
            return Response({
                "error_code": 422,
                "message": "PROCESS__UNABLE_TO_PROCESS_IMAGE"
            }, status=status.HTTP_422_UNPROCESSABLE_ENTITY)

        # Translation 단계
        translator = TranslationModule()
        translated = []
        for field in ocr_result["fields"]:
            original_txt = field.get("inferText", "")
            if original_txt:
                translated_txt = translator.translateText(original_txt, target_lang=lang)
                translated.append(translated_txt)
            else:
                translated.append("")

        # TTS 단계
        tts = TTSModule()
        audio_results = []
        for text in translated:
            if text.strip():
                audio_buffer = tts.generateTTS(text)
                if audio_buffer:
                    audio_base64 = base64.b64encode(audio_buffer.read()).decode("utf-8")
                    audio_results.append(audio_base64)
                else:
                    audio_results.append("")
            else:
                audio_results.append("")

        # 페이지 생성
        page = Page.objects.create(
            session=session,
            img_url=image_path,
            translation_text="\n".join(translated),
            bbox_json=json.dumps(ocr_result),
            created_at=timezone.now()
        )

        # BB 데이터 저장
        for i, field in enumerate(ocr_result["fields"]):
            BB.objects.create(
                page=page,
                original_text=field.get("inferText", ""),
                translated_text=translated[i] if i < len(translated) else "",
                audio_base64=audio_results[i] if i < len(audio_results) else "",
                position={
                    "x": field.get("x", 0),
                    "y": field.get("y", 0),
                    "width": field.get("width", 0),
                    "height": field.get("height", 0),
                }
            )

        session.totalPages += 1
        session.save()

        return Response({
            "session_id": session_id,
            "page_index": int(page_index),
            "status": "ready",
            "submitted_at": timezone.now(),
        }, status=status.HTTP_200_OK)

class CheckOCRStatusView(APIView):
    """
    [GET] /process/check_ocr_translation
    - Check ocr progress
    
    Endpoint: /process/check_ocr_translation

    - Request (GET)

        {
        "session_id": "string",
        "page_index": integer
        }
        
    - Response

        Status: 200 OK

        {
        "session_id": "string",
        "page_index": integer,
        "status": "string",           // "pending", "processing", "ready"
        "progress": integer,          // 0-100, estimated progress percentage
        "submitted_at": "datetime",   // when image was uploaded
        "processed_at": "datetime or null" // when processing finished, null if not ready
        }
    """

    def get(self, request):
        session_id = request.query_params.get("session_id")
        page_index = request.query_params.get("page_index")

        if not session_id or page_index is None:
            return Response(status=status.HTTP_400_BAD_REQUEST)

        try:
            session = Session.objects.get(id=session_id)
            page = Page.objects.filter(session=session).order_by("id")[int(page_index)]

            return Response({
                "session_id": session_id,
                "page_index": int(page_index),
                "status": "ready",
                "progress": 100,
                "submitted_at": page.created_at,
                "processed_at": page.created_at
            }, status=status.HTTP_200_OK)

        except (Session.DoesNotExist, IndexError):
            return Response(status=status.HTTP_404_NOT_FOUND)


class CheckTTSStatusView(APIView):
    """
    [GET] /process/check_tts
    
    Endpoint: /process/check_tts

    - Request (GET)

        {
        "session_id": "string",
        "page_index": integer
        }
    
    - Response

        Status: 200 OK

        {
        "session_id": "string",
        "page_index": integer,
        "status": "string",           // "pending", "processing", "ready"
        "progress": integer,          // 0-100, estimated progress percentage
        "submitted_at": "datetime",   // when image was uploaded
        "processed_at": "datetime or null" // when processing finished, null if not ready
        }
    """

    def get(self, request):
        session_id = request.query_params.get("session_id")
        page_index = request.query_params.get("page_index")

        if not session_id or page_index is None:
            return Response(status=status.HTTP_400_BAD_REQUEST)

        try:
            session = Session.objects.get(id=session_id)
            page = Page.objects.filter(session=session).order_by("id")[int(page_index)]

            return Response({
                "session_id": session_id,
                "page_index": int(page_index),
                "status": "ready",
                "progress": 100,
                "submitted_at": page.created_at,
                "processed_at": page.created_at
            }, status=status.HTTP_200_OK)

        except (Session.DoesNotExist, IndexError):
            return Response(status=status.HTTP_404_NOT_FOUND)
