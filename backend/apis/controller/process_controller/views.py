from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework import status
from django.utils import timezone
from apis.models.session_model import Session
from apis.models.page_model import Page
from apis.models.bb_model import BB
from apis.modules.ocr_processor import OCRModule
from apis.modules.tts_processor import TTSModule
import base64
import io
import json
import uuid
import os
import asyncio

class ProcessUploadView(APIView):
    """
    [POST] /process/upload
    - OCR → Translation → TTS 순서로 실행
    
    Endpoint: /process/upload

    - Request (POST)

        {
        "session_id": "string",
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
        lang = request.data.get("lang")
        image_base64 = request.data.get("image_base64")
        if not all([session_id, lang, image_base64]):
            return Response(status=status.HTTP_400_BAD_REQUEST)

        try:
            session = Session.objects.get(id=session_id)
        except Session.DoesNotExist:
            return Response(status=status.HTTP_404_NOT_FOUND)
        
        page_index = session.getPages().count()
        # 이미지 디코딩 및 저장. 
        image_bytes = base64.b64decode(image_base64)
        image_filename = f"{uuid.uuid4().hex}.jpg"
        image_path = f"media/images/{session_id}_{page_index}_{image_filename}"
        os.makedirs(os.path.dirname(image_path), exist_ok=True)
        with open(image_path, "wb") as f:
            f.write(image_bytes)

        # OCR 단계
        ocr = OCRModule()
        ocr_result = ocr.process_page(image_path) # paragraph
        if not ocr_result:
            return Response({
                "error_code": 422,
                "message": "PROCESS__UNABLE_TO_PROCESS_IMAGE"
            }, status=status.HTTP_422_UNPROCESSABLE_ENTITY)


        tasks = []
        tts = TTSModule()

        for i, para in enumerate(ocr_result, 1):
            page = {
                "fileName": f"{session_id}_{page_index}_{i}.jpg",
                "text": para.get("text", "")
            }
            tasks.append(tts.process_paragraph(page, log_csv=True, check_latency=True))

        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)
        tasks_results = loop.run_until_complete(asyncio.gather(*tasks))
        loop.close()

        tts_audio = []
        paras_translations = []

        for result in tasks_results:
            if result.get("status") == "ok":
                para_audio = [
                    sentence_data["tts_result"]
                    for sentence_data in result.get("details", [])
                    if "tts_result" in sentence_data
                ]
                tts_audio.append(para_audio)

                para_translated = " ".join(
                    [
                        sentence_data["translation"]
                        for sentence_data in result.get("details", [])
                        if "translation" in sentence_data
                    ]
                )
                paras_translations.append(para_translated)


        # 페이지 생성
        page = Page.objects.create(
            session=session,
            img_url=image_path,
            bbox_json=json.dumps(ocr_result),
            created_at=timezone.now()
        )
        print(len(ocr_result), len(tts_audio))
        # BB 데이터 저장
        for i, para in enumerate(ocr_result):
            para_text = para.get("text", "")
            bbox = para.get("bbox", {})
            para_audio = tts_audio[i] if i < len(tts_audio) else []
            para_translated = paras_translations[i] if i < len(paras_translations) else ""
            BB.objects.create(
                page=page,
                original_text=para_text,
                audio_base64=[base64.b64encode(a).decode("utf-8") for a in para_audio],
                translated_text=para_translated,
                coordinates=bbox
            )
        session.totalPages += 1
        session.save()
        print("session id:", session.id, "page_index: ", page_index, "page:", page)
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
