from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework import status
from apis.models.page_model import Page
import base64
import os
import json


class PageGetImageView(APIView):
    """
    Retrieve page image as base64
    
    [GET] /page/get_image?session_id={session_id}&page_index={page_index}
    
    Query Parameters:
        session_id: Session identifier
        page_index: Page number
    
    Response (200 OK):
        {
            "session_id": "string",
            "page_index": 0,
            "image_base64": "string",
            "stored_at": "datetime"
        }
    """

    def get(self, request):
        session_id = request.query_params.get("session_id")
        page_index = request.query_params.get("page_index")

        if not session_id or page_index is None:
            return Response(status=status.HTTP_400_BAD_REQUEST)

        try:
            pages = Page.objects.filter(session__id=session_id)
            page = pages.order_by("id")[int(page_index)]
            img_path = page.img_url

            if not img_path or not os.path.exists(img_path):
                return Response(status=status.HTTP_404_NOT_FOUND)

            with open(img_path, "rb") as img_file:
                encoded = base64.b64encode(img_file.read())
                img_base64 = encoded.decode("utf-8")

            return Response(
                {
                    "session_id": session_id,
                    "page_index": int(page_index),
                    "image_base64": img_base64,
                    "stored_at": page.created_at,
                },
                status=status.HTTP_200_OK,
            )

        except (Page.DoesNotExist, IndexError):
            return Response(status=status.HTTP_404_NOT_FOUND)
        except Exception as e:
            return Response(
                {"error": str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR
            )


class PageGetOCRView(APIView):
    """
    Retrieve OCR results with bounding boxes and translations
    
    [GET] /page/get_ocr?session_id={session_id}&page_index={page_index}
    
    Query Parameters:
        session_id: Session identifier
        page_index: Page number
    
    Response (200 OK):
        {
            "session_id": "string",
            "page_index": 0,
            "ocr_results": [
                {
                    "bbox": {"x": 0, "y": 0, "width": 100, "height": 50},
                    "original_txt": "string",
                    "translation_txt": "string"
                }
            ],
            "processed_at": "datetime"
        }
    """

    def get(self, request):
        session_id = request.query_params.get("session_id")
        page_index = request.query_params.get("page_index")
        if not session_id or page_index is None:
            return Response(status=status.HTTP_400_BAD_REQUEST)
        try:
            pages = Page.objects.filter(session_id=session_id)
            page = pages.order_by("id")[int(page_index)]

            bbs = page.getBBs()

            ocr_results = []
            for bb in bbs:
                ocr_results.append(
                    {
                        "bbox": bb.coordinates,
                        "original_txt": bb.original_text,
                        "translation_txt": bb.translated_text,
                    }
                )

            return Response(
                {
                    "session_id": session_id,
                    "page_index": int(page_index),
                    "ocr_results": ocr_results,
                    "processed_at": page.created_at,
                },
                status=status.HTTP_200_OK,
            )

        except (Page.DoesNotExist, IndexError):
            return Response(status=status.HTTP_404_NOT_FOUND)
        except Exception as e:
            return Response(
                {"error": str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR
            )

class PageGetTTSView(APIView):
    """
    Retrieve TTS audio data for a page
    
    [GET] /page/get_tts?session_id={session_id}&page_index={page_index}
    
    Query Parameters:
        session_id: Session identifier
        page_index: Page number
    
    Response (200 OK):
        {
            "session_id": "string",
            "page_index": 0,
            "audio_results": [
                {
                    "bbox_index": 0,
                    "audio_base64_list": ["string", "string"]
                }
            ],
            "generated_at": "datetime"
        }
    """

    def get(self, request):
        session_id = request.query_params.get("session_id")
        page_index = request.query_params.get("page_index")

        if not session_id or page_index is None:
            return Response(status=status.HTTP_400_BAD_REQUEST)

        try:
            pages = Page.objects.filter(session__id=session_id)
            page = pages.order_by("id")[int(page_index)]
            bbs = page.getBBs()

            audio_results = []
            for i, bb in enumerate(bbs):
                audio_list = (
                    json.loads(bb.audio_base64)
                    if isinstance(bb.audio_base64, str)
                    else bb.audio_base64
                )

                # Only include boxes that have audio
                if audio_list and len(audio_list) > 0:
                    audio_results.append(
                        {"bbox_index": i, "audio_base64_list": audio_list}
                    )

            return Response(
                {
                    "session_id": session_id,
                    "page_index": int(page_index),
                    "audio_results": audio_results,
                    "generated_at": page.created_at,
                },
                status=status.HTTP_200_OK,
            )

        except (Page.DoesNotExist, IndexError):
            return Response(status=status.HTTP_404_NOT_FOUND)
        except Exception as e:
            return Response(
                {"error": str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR
            )
