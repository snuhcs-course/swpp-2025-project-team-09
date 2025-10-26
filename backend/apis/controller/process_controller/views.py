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
import json
import uuid
import os
import asyncio
import threading

class ProcessUploadFrontPageView(APIView):
    """
    [POST] /process/upload_front
    - 표지에 대해 OCR → Translation → TTS 순서로 실행, Translation과 TTS는 Title(가장 큰 BB) 영역만 처리

    Endpoint: /process/upload_front

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
        print("OCR Result:", ocr_result)
        def bbox_area(bbox):
            x1, y1 = bbox.get("x1", 0), bbox.get("y1", 0)
            x3, y3 = bbox.get("x3", 0), bbox.get("y3", 0)
            return abs(x3 - x1) * abs(y3 - y1)

        if isinstance(ocr_result, list) and len(ocr_result) > 0:
            ocr_result = [max(ocr_result, key=lambda bb: bbox_area(bb.get("bbox", {})))]
        print("Filtered OCR Result (Title only):", ocr_result)
        tasks = []
        tts = TTSModule()

        for i, para in enumerate(ocr_result, 1):
            page = {
                "fileName": f"{session_id}_{page_index}_{i}.jpg",
                "text": para.get("text", "")
            }
        tasks.append(tts.process_paragraph(page, log_csv=True, check_latency=True, voice="verse", split_sentences=False))    # 남성
        tasks.append(tts.process_paragraph(page, log_csv=True, check_latency=True, voice="shimmer", split_sentences=False))  # 여성

        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)
        tasks_results = loop.run_until_complete(asyncio.gather(*tasks))
        loop.close()

        tts_audio_male = []
        tts_audio_female = []
        paras_translations = []

        male_result, female_result = tasks_results
        
        if male_result.get("status") == "ok":
            para_translated = " ".join(
                sentence_data["translation"]
                for sentence_data in male_result.get("details", [])
                if "translation" in sentence_data
            )
            paras_translations.append(para_translated)
        else:
            paras_translations.append("")

        # 남성 오디오
        if male_result.get("status") == "ok":
            para_audio_male = [
                sentence_data["tts_result"]
                for sentence_data in male_result.get("details", [])
                if "tts_result" in sentence_data
            ]
            tts_audio_male.append(para_audio_male)
        else:
            tts_audio_male.append([])

        # 여성 오디오
        if female_result.get("status") == "ok":
            para_audio_female = [
                sentence_data["tts_result"]
                for sentence_data in female_result.get("details", [])
                if "tts_result" in sentence_data
            ]
            tts_audio_female.append(para_audio_female)
        else:
            tts_audio_female.append([])


        # 페이지 생성
        page = Page.objects.create(
            session=session,
            img_url=image_path,
            bbox_json=json.dumps(ocr_result),
            created_at=timezone.now(),
            isFrontPage=True
        )

        # BB 데이터 저장
        for i, para in enumerate(ocr_result):
            para_text = para.get("text", "")
            bbox = para.get("bbox", {})
            para_translated = paras_translations[i] if i < len(paras_translations) else ""
            para_audio_male = tts_audio_male[i] if i < len(tts_audio_male) else []
            para_audio_female = tts_audio_female[i] if i < len(tts_audio_female) else []
            
            BB.objects.create(
                page=page,
                original_text=para_text,
                audio_base64=[
                    base64.b64encode(para_audio_male[0]).decode("utf-8") if para_audio_male else None,
                    base64.b64encode(para_audio_female[0]).decode("utf-8") if para_audio_female else None
                ],
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


class ProcessUploadView(APIView):
    """
    POST /process/upload
    
    Handles image upload, OCR, translation, and initiates background TTS.
    Returns immediately after translation completes.
    """

    def post(self, request):
        # Validate request
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
        
        # Save image
        image_path = self._save_image(image_base64, session_id, page_index)
        
        # Run OCR
        ocr_result = OCRModule().process_page(image_path)
        if not ocr_result:
            return Response({
                "error_code": 422,
                "message": "PROCESS__UNABLE_TO_PROCESS_IMAGE"
            }, status=status.HTTP_422_UNPROCESSABLE_ENTITY)

        # Run translation synchronously (fast, ~2-3s per paragraph)
        tts_module = TTSModule()
        translation_data = self._get_all_translations(tts_module, ocr_result, session_id, page_index)
        
        # Create page and bounding boxes
        page = self._create_page_and_bbs(session, image_path, ocr_result, translation_data)
        
        # Start background TTS
        self._start_background_tts(tts_module, ocr_result, translation_data, page, session_id, page_index)
        
        # Update session
        session.totalPages += 1
        session.save()
        
        return Response({
            "session_id": session_id,
            "page_index": page_index,
            "status": "ready",
            "submitted_at": timezone.now(),
        }, status=status.HTTP_200_OK)

    def _save_image(self, image_base64: str, session_id: str, page_index: int) -> str:
        """Decode and save uploaded image."""
        image_bytes = base64.b64decode(image_base64)
        image_filename = f"{uuid.uuid4().hex}.jpg"
        image_path = f"media/images/{session_id}_{page_index}_{image_filename}"
        
        os.makedirs(os.path.dirname(image_path), exist_ok=True)
        with open(image_path, "wb") as f:
            f.write(image_bytes)
        
        return image_path

    def _get_all_translations(self, tts_module: TTSModule, ocr_result: list, session_id: str, page_index: int) -> list:
        """
        Get translations and sentiment for all paragraphs (no TTS yet).
        Runs ALL paragraphs in parallel.
        Returns list of translation data per paragraph.
        """
        async def get_para_translation(i: int, para: dict):
            page_data = {
                "fileName": f"{session_id}_{page_index}_{i}.jpg",
                "text": para.get("text", "")
            }
            return await tts_module.get_translations_only(page_data)
        
        # Run ALL paragraphs in parallel
        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)
        translation_data = loop.run_until_complete(
            asyncio.gather(*[
                get_para_translation(i, para) for i, para in enumerate(ocr_result)
            ])
        )
        loop.close()
        
        return translation_data

    def _create_page_and_bbs(self, session: Session, image_path: str, ocr_result: list, translation_data: list) -> Page:
        """Create Page and BoundingBox objects with translations."""
        page = Page.objects.create(
            session=session,
            img_url=image_path,
            bbox_json=json.dumps(ocr_result),
            created_at=timezone.now()
        )
        
        for i, para in enumerate(ocr_result):
            # Extract translation text
            if i < len(translation_data) and translation_data[i]["status"] == "ok":
                sentences = translation_data[i]["sentences"]
                translated_text = " ".join([s["translation"] for s in sentences])
            else:
                translated_text = ""
            
            # Create BB with translation but no audio yet
            BB.objects.create(
                page=page,
                original_text=para.get("text", ""),
                audio_base64=[],
                translated_text=translated_text,
                coordinates=para.get("bbox", {})
            )
        
        return page

    def _start_background_tts(self, tts_module: TTSModule, ocr_result: list, translation_data: list, page: Page, session_id: str, page_index: int):
        """Start background thread to run TTS using pre-computed translations."""
        
        def run_tts():
            print(f"[TTS Background] Starting TTS for page {page.id}")
            
            try:
                for i, para in enumerate(ocr_result):
                    if i >= len(translation_data) or translation_data[i]["status"] != "ok":
                        continue
                    
                    # Run TTS with pre-computed translations
                    loop = asyncio.new_event_loop()
                    asyncio.set_event_loop(loop)
                    audio_results = loop.run_until_complete(
                        tts_module.run_tts_only(translation_data[i], session_id, page_index, i)
                    )
                    loop.close()
                    
                    # Update BB with audio
                    if audio_results:
                        bb = page.getBBs()[i]
                        bb.audio_base64 = audio_results
                        bb.save()
                        print(f"[TTS Background] BB {bb.id} ready ({len(audio_results)} clips)")
                
                print(f"[TTS Background] Completed TTS for page {page.id}")
            
            except Exception as e:
                print(f"[TTS Background] Error: {e}")
                import traceback
                traceback.print_exc()
        
        thread = threading.Thread(target=run_tts, daemon=True)
        thread.start()


class CheckOCRStatusView(APIView):
    """GET /process/check_ocr - Check OCR/translation status."""

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
    """GET /process/check_tts - Check TTS progress."""

    def get(self, request):
        session_id = request.query_params.get("session_id")
        page_index = request.query_params.get("page_index")

        if not session_id or page_index is None:
            return Response(status=status.HTTP_400_BAD_REQUEST)

        try:
            session = Session.objects.get(id=session_id)
            page = Page.objects.filter(session=session).order_by("id")[int(page_index)]
            bbs = page.getBBs()
            
            total_bbs = bbs.count()
            if total_bbs == 0:
                return Response({
                    "session_id": session_id,
                    "page_index": int(page_index),
                    "status": "ready",
                    "progress": 100,
                    "submitted_at": page.created_at,
                    "processed_at": page.created_at
                }, status=status.HTTP_200_OK)
            
            # Count BBs with audio
            completed_bbs = sum(1 for bb in bbs if self._has_audio(bb))
            progress = int((completed_bbs / total_bbs) * 100)
            status_str = "ready" if completed_bbs == total_bbs else "processing"

            return Response({
                "session_id": session_id,
                "page_index": int(page_index),
                "status": status_str,
                "progress": progress,
                "submitted_at": page.created_at,
                "processed_at": page.created_at if status_str == "ready" else None
            }, status=status.HTTP_200_OK)

        except (Session.DoesNotExist, IndexError):
            return Response(status=status.HTTP_404_NOT_FOUND)

    def _has_audio(self, bb) -> bool:
        """Check if a bounding box has audio."""
        audio_list = (
            json.loads(bb.audio_base64)
            if isinstance(bb.audio_base64, str)
            else bb.audio_base64
        )
        return bool(audio_list and len(audio_list) > 0)