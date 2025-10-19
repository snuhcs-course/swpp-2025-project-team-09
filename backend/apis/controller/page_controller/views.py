from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework import status
from apis.models.page_model import Page
from apis.models.bb_model import BB
import base64
import os
import json

class PageGetImageView(APIView):
    """
    [GET] /page/get_image
    API endpoint for retrieving page img
    
    Endpoint: /page/get_image

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
        "image_base64": "string",        // Base64-encoded image
        "stored_at": "string (datetime)" // when image was uploaded/saved
        }
    """

    def get(self, request):
        """
        Args:
            request (rest_framework.request.Request): The incoming HTTP GET request.
        
        Returns:
            - rest_framework.response.Response
                - Status: 200 OK, JSON list including page image
                
        """
        
        session_id = request.query_params.get("session_id")
        page_index = request.query_params.get("page_index")

        if not session_id or page_index is None:
            return Response(status=status.HTTP_400_BAD_REQUEST)

        try:
            page = Page.objects.filter(session__id=session_id).order_by("id")[int(page_index)]
            img_path = page.img_url

            if not img_path or not os.path.exists(img_path):
                return Response(status=status.HTTP_404_NOT_FOUND)

            with open(img_path, "rb") as img_file:
                img_base64 = base64.b64encode(img_file.read()).decode("utf-8")

            return Response({
                "session_id": session_id,
                "page_index": int(page_index),
                "image_base64": img_base64,
                "stored_at": page.created_at
            }, status=status.HTTP_200_OK)

        except (Page.DoesNotExist, IndexError):
            return Response(status=status.HTTP_404_NOT_FOUND)
        except Exception as e:
            return Response({"error": str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)


class PageGetOCRView(APIView):
    """
    [GET] /page/get_ocr
    - OCR 결과 (bbox + 원문 + 번역문) 반환
    
    - Endpoint: /page/get_ocr_translation

    - Request (GET)

        {
        "session_id": "string",
        "page_index": integer,
        }
        
    - Response

        Status: 200 OK

        {
        "session_id": "string",
        "page_index": integer,
        "ocr_results": [ # can be empty
            {
            "bbox": {
                "x": integer,
                "y": integer,
                "width": integer,
                "height": integer
            },
            "original_txt": "string",
            "translation_txt": "string"
            },
            {
            "bbox": {
                "x": integer,
                "y": integer,
                "width": integer,
                "height": integer
            },
            "original_txt": "string",
            "translation_txt": "string"
            }
            /* ...more boxes per page */
        ],
        "processed_at": "string (datetime)"
        }
    """

    def get(self, request):
        session_id = request.query_params.get("session_id")
        page_index = request.query_params.get("page_index")
        if not session_id or page_index is None:
            return Response(status=status.HTTP_400_BAD_REQUEST)
        try:
            page = Page.objects.filter(session_id=session_id).order_by("id")[int(page_index)]

            bbs = page.getBBs()

            ocr_results = []
            for bb in bbs:
                ocr_results.append({
                    "bbox": bb.coordinates,
                    "original_txt": bb.original_text,
                    "translation_txt": bb.translated_text
                })

            return Response({
                "session_id": session_id,
                "page_index": int(page_index),
                "ocr_results": ocr_results,
                "processed_at": page.created_at
            }, status=status.HTTP_200_OK)

        except (Page.DoesNotExist, IndexError):
            return Response(status=status.HTTP_404_NOT_FOUND)
        except Exception as e:
            return Response({"error": str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)


class PageGetTTSView(APIView):
    """
    [GET] /page/get_tts
    - TTS 오디오(base64 리스트) 반환
    
    Endpoint: /page/get_tts

    - Request (GET)

        {
        "session_id": "string",
        "page_index": Integer,
        }
    
    - Response

        Status: 200 OK

        {
        "session_id": "string",
        "page_index": "integer",
        "audio_results": [
            {
            "bbox_index": "integer",   
            "audio_base64_list": "list of string" 
            }
        ],
        "generated_at": "string (datetime)"
        }
    """

    def get(self, request):
        session_id = request.query_params.get("session_id")
        page_index = request.query_params.get("page_index")

        if not session_id or page_index is None:
            return Response(status=status.HTTP_400_BAD_REQUEST)

        try:
            page = Page.objects.filter(session__id=session_id).order_by("id")[int(page_index)]
            bbs = page.getBBs()

            audio_results = []
            for i, bb in enumerate(bbs):
                audio_list = (
                    json.loads(bb.audio_base64)
                    if isinstance(bb.audio_base64, str)
                    else bb.audio_base64
                )

                audio_results.append({
                    "bbox_index": i,
                    "audio_base64_list": audio_list
                })

            return Response({
                "session_id": session_id,
                "page_index": int(page_index),
                "audio_results": audio_results,
                "generated_at": page.created_at
            }, status=status.HTTP_200_OK)

        except (Page.DoesNotExist, IndexError):
            return Response(status=status.HTTP_404_NOT_FOUND)
        except Exception as e:
            return Response({"error": str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)
