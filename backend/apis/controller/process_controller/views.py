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
            return Response(
                {"error_code": 422, "message": "PROCESS__UNABLE_TO_PROCESS_IMAGE"},
                status=status.HTTP_422_UNPROCESSABLE_ENTITY,
            )
        
        # Count words from OCR and add to session.totalWords
        total_words = sum(
            len((para.get("text", "") or "").split()) for para in ocr_result
        )
        session.totalWords = session.totalWords + total_words
        print(f"[DEBUG] OCR words in page {page_index}: {total_words}")

        # Map language codes to full names for TTS
        lang_map = {"en": "English", "zh": "Chinese"}
        target_lang = lang_map.get(lang, "English")

        # Run translation synchronously (fast, ~2-3s per paragraph)
        tts_module = TTSModule(target_lang=target_lang)
        translation_data = self._get_all_translations(
            tts_module, ocr_result, session_id, page_index
        )

        # Create page and bounding boxes
        page = self._create_page_and_bbs(
            session, image_path, ocr_result, translation_data
        )
        print("[DEBUG] voice preference:", session.voicePreference)
        # Start background TTS
        self._start_background_tts(
            tts_module,
            ocr_result,
            translation_data,
            page,
            session_id,
            page_index,
            para_voice=session.voicePreference,
        )

        # Update session
        session.totalPages += 1
        session.save()
        print("[DEBUG] Page index after upload:", page_index)
        return Response(
            {
                "session_id": session_id,
                "page_index": page_index,
                "status": "ready",
                "submitted_at": timezone.now(),
            },
            status=status.HTTP_200_OK,
        )

    def _save_image(self, image_base64: str, session_id: str, page_index: int) -> str:
        """Decode and save uploaded image."""
        image_bytes = base64.b64decode(image_base64)
        image_filename = f"{uuid.uuid4().hex}.jpg"
        fname = f"{session_id}_{page_index}_{image_filename}"
        image_path = f"media/images/{fname}"

        os.makedirs(os.path.dirname(image_path), exist_ok=True)
        with open(image_path, "wb") as f:
            f.write(image_bytes)

        return image_path

    def _get_all_translations(
        self, tts_module: TTSModule, ocr_result: list, session_id: str, page_index: int
    ) -> list:
        """
        Get translations and sentiment for all paragraphs (no TTS yet).
        Runs ALL paragraphs in parallel.
        Returns list of translation data per paragraph.
        """

        async def get_para_translation(i: int, para: dict):
            page_data = {
                "fileName": f"{session_id}_{page_index}_{i}.jpg",
                "text": para.get("text", ""),
            }
            return await tts_module.get_translations_only(page_data)

        # Run ALL paragraphs in parallel
        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)
        try:
            translation_data = loop.run_until_complete(
                asyncio.gather(
                    *[
                        get_para_translation(i, para)
                        for i, para in enumerate(ocr_result)
                    ]
                )
            )
            return translation_data
        finally:
            # Properly cleanup async resources before closing the loop
            loop.run_until_complete(loop.shutdown_asyncgens())
            loop.close()

    def _create_page_and_bbs(
        self,
        session: Session,
        image_path: str,
        ocr_result: list,
        translation_data: list,
    ) -> Page:
        """Create Page and BoundingBox objects with translations."""
        page = Page.objects.create(
            session=session,
            img_url=image_path,
            bbox_json=json.dumps(ocr_result),
            created_at=timezone.now(),
        )

        for i, para in enumerate(ocr_result):
            # Extract translation text
            ok_status = (
                i < len(translation_data) and translation_data[i]["status"] == "ok"
            )
            if ok_status:
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
                coordinates=para.get("bbox", {}),
            )

        return page

    def _start_background_tts(
        self,
        tts_module: TTSModule,
        ocr_result: list,
        translation_data: list,
        page: Page,
        session_id: str,
        page_index: int,
        para_voice: str,
    ):
        """Start background thread to run TTS using pre-computed translations."""

        def run_tts():
            print(f"[TTS Background] Starting TTS for page {page_index}")
            print(f"[TTS Background] Using voice preference: {para_voice}")
            try:
                for i, _ in enumerate(ocr_result):
                    not_ok = (
                        i >= len(translation_data)
                        or translation_data[i]["status"] != "ok"
                    )
                    if not_ok:
                        continue

                    # Run TTS with pre-computed translations
                    loop = asyncio.new_event_loop()
                    asyncio.set_event_loop(loop)
                    try:
                        audio_results = loop.run_until_complete(
                            tts_module.run_tts_only(
                                translation_data[i],
                                session_id,
                                page_index,
                                i,
                                para_voice,
                            )
                        )
                    finally:
                        # Properly cleanup async resources before closing the loop
                        loop.run_until_complete(loop.shutdown_asyncgens())
                        loop.close()

                    # Update BB with audio
                    if audio_results:
                        bb = page.getBBs()[i]
                        bb.audio_base64 = audio_results
                        bb.save()
                        print(
                            f"[TTS Background] BB {bb.id} ready ({len(audio_results)} clips)"
                        )

                print(f"[TTS Background] Completed TTS for page {page_index}")

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
            pages = Page.objects.filter(session=session)
            page = pages.order_by("id")[int(page_index)]

            return Response(
                {
                    "session_id": session_id,
                    "page_index": int(page_index),
                    "status": "ready",
                    "progress": 100,
                    "submitted_at": page.created_at,
                    "processed_at": page.created_at,
                },
                status=status.HTTP_200_OK,
            )

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
            pages = Page.objects.filter(session=session)
            page = pages.order_by("id")[int(page_index)]
            bbs = page.getBBs()

            total_bbs = bbs.count()
            if total_bbs == 0:
                return Response(
                    {
                        "session_id": session_id,
                        "page_index": int(page_index),
                        "status": "ready",
                        "progress": 100,
                        "submitted_at": page.created_at,
                        "processed_at": page.created_at,
                    },
                    status=status.HTTP_200_OK,
                )

            # Count BBs with audio
            completed_bbs = sum(1 for bb in bbs if self._has_audio(bb))
            progress = int((completed_bbs / total_bbs) * 100)
            is_ready = completed_bbs == total_bbs
            status_str = "ready" if is_ready else "processing"

            return Response(
                {
                    "session_id": session_id,
                    "page_index": int(page_index),
                    "status": status_str,
                    "progress": progress,
                    "submitted_at": page.created_at,
                    "processed_at": (
                        page.created_at if status_str == "ready" else None
                    ),
                },
                status=status.HTTP_200_OK,
            )

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


class ProcessUploadCoverView(APIView):
    """
    POST /process/upload_cover

    Handles cover image upload, OCR, translation without TTS.
    Additionally, extracts the book title from the OCR result and saves it in the session.
    Creates a Page and BB with translated text only.
    """

    """
    Input (POST JSON body):
        - session_id (string): session UUID
        - lang (string): language code (e.g. "en" or "ko")
        - image_base64 (string): base64-encoded cover image

    Output (JSON response):
        - session_id (string): same as input
        - page_index (int): always 0 for cover
        - status (string): "ready"
        - submitted_at (string, ISO8601): submission timestamp
        - title (string): extracted book title
        - translated_title (string): translated book title
        - tts_male (string): null (no TTS for cover)
        - tts_female (string): null (no TTS for cover)
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

        page_index = 0  # Cover page is always index 0

        # Save cover image (to a different path)
        image_path = self._save_image(image_base64, session_id, page_index)

        # Run OCR to get title
        title = OCRModule().process_cover_page(image_path)
        print(f"[DEBUG] OCR Result for cover: {title}")
        if not title:
            return Response(
                {"error_code": 422, "message": "PROCESS__UNABLE_TO_PROCESS_IMAGE"},
                status=status.HTTP_422_UNPROCESSABLE_ENTITY,
            )

        # Map language codes to full names for TTS
        lang_map = {"en": "English", "zh": "Chinese"}
        target_lang = lang_map.get(lang, "English")

        # Run translation for title synchronously
        translated_text, tts_male, tts_female = self._run_async(
            TTSModule(target_lang=target_lang).translate_and_tts_cover(
                title, session_id, page_index
            )
        )

        page = self._create_page_and_bbs(
            session,
            image_path,
            [{"text": title}],
            [{
                "status": "ok",
                "sentences": [{"translation": translated_text}]
            }]
        )

        # Update session
        session.title = title
        session.translated_title = translated_text
        print(f"[debug]{session.translated_title}")
        session.totalPages += 1
        session.save()

        return Response(
            {
                "session_id": session_id,
                "page_index": page_index,
                "status": "ready",
                "submitted_at": timezone.now().isoformat(),
                "title": session.title,
                "translated_title": session.translated_title,
                "tts_male": tts_male,
                "tts_female": tts_female,
            },
            status=status.HTTP_200_OK,
        )

    def _get_all_translations(
        self, tts_module: TTSModule, ocr_result: list, session_id: str, page_index: int
    ) -> list:
        """
        Get translations and sentiment for all paragraphs (no TTS yet).
        Runs ALL paragraphs in parallel.
        Returns list of translation data per paragraph.
        """

        async def get_para_translation(i: int, para: dict):
            page_data = {
                "fileName": f"{session_id}_{page_index}_{i}.jpg",
                "text": para.get("text", ""),
            }
            return await tts_module.get_translations_only(page_data)

        # Run ALL paragraphs in parallel
        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)
        translation_data = loop.run_until_complete(
            asyncio.gather(
                *[get_para_translation(i, para) for i, para in enumerate(ocr_result)]
            )
        )
        loop.close()

        return translation_data

    def _create_page_and_bbs(
        self,
        session: Session,
        image_path: str,
        ocr_result: list,
        translation_data: list,
    ) -> Page:
        """Create Page and BoundingBox objects with translations."""
        page = Page.objects.create(
            session=session,
            img_url=image_path,
            bbox_json=json.dumps(ocr_result),
            created_at=timezone.now(),
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
                coordinates=para.get("bbox", {}),
            )

        return page

    def _save_image(self, image_base64: str, session_id: str, page_index: int) -> str:
        """Decode and save uploaded image."""
        image_bytes = base64.b64decode(image_base64)
        image_filename = f"{uuid.uuid4().hex}.jpg"
        image_path = f"media/images/{session_id}_{page_index}_{image_filename}"

        os.makedirs(os.path.dirname(image_path), exist_ok=True)
        with open(image_path, "wb") as f:
            f.write(image_bytes)

        return image_path

    def _run_async(self, coroutine):
        """Helper to run async code in sync context."""
        try:
            loop = asyncio.new_event_loop()
            asyncio.set_event_loop(loop)
            result = loop.run_until_complete(coroutine)
            loop.close()
            return result
        except Exception as e:
            import traceback

            traceback.print_exc()
            return "", ""
