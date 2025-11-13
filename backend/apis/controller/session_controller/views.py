from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework import status
from django.utils import timezone
from django.utils.dateparse import parse_datetime
from apis.models.session_model import Session
from apis.models.user_model import User
from apis.models.page_model import Page
import base64
import os


class StartSessionView(APIView):
    """
    [POST] /session/start

    Endpoint: /session/start

        - Request (POST)

            {
            "user_id": "string",
            "page_index": 0  # first page (0-indexed)
            }
            Response

        - Status: 200 OK

            {
            "session_id": "string",
            "started_at": "datetime",
            "page_index": 0
            }
    """

    def post(self, request):
        user_id = request.data.get("user_id")
        page_index = request.data.get("page_index", 0)

        if not user_id:
            return Response(status=status.HTTP_400_BAD_REQUEST)

        try:
            user = User.objects.get(device_info=user_id)
            now_str = timezone.now().strftime("%Y-%m-%d %H:%M")
            title = f"Reading Session {now_str}"
            session = Session.objects.create(
                user=user,
                title=title,
                created_at=timezone.now(),
                totalPages=0,
                isOngoing=True,
            )
            return Response(
                {
                    "session_id": str(session.id),
                    "started_at": session.created_at,
                    "page_index": page_index,
                },
                status=status.HTTP_200_OK,
            )
        except User.DoesNotExist:
            return Response(
                {"error_code": 404, "message": "USER__NOT_FOUND"},
                status=status.HTTP_404_NOT_FOUND,
            )
        except Exception as e:
            return Response(
                {"error": str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR
            )


class SelectVoiceView(APIView):
    """
    [POST] /session/voice

    Endpoint: /session/voice

    - Request (POST)

        {
        "session_id": "string",
        "voice_style": "string"
        }

    - Response

        Status: 200 OK

        {
        "session_id": "string",
        "voice_style": "string"
        }
    """

    def post(self, request):
        session_id = request.data.get("session_id")
        voice_style = request.data.get("voice_style")

        if not session_id or not voice_style:
            return Response(status=status.HTTP_400_BAD_REQUEST)

        try:
            session = Session.objects.get(id=session_id)
            session.voicePreference = voice_style
            session.save()
            print("[DEBUG] Voice style set to:", session.voicePreference)
            print("[DEBUG] Session ID:", session_id)
            return Response(
                {"session_id": str(session.id), "voice_style": session.voicePreference},
                status=status.HTTP_200_OK,
            )
        except Session.DoesNotExist:
            return Response(
                {"error_code": 404, "message": "SESSION__NOT_FOUND"},
                status=status.HTTP_404_NOT_FOUND,
            )


class EndSessionView(APIView):
    """
    [POST] /session/end

    Endpoint: /session/end

    - Request (POST)

        {
        "session_id": "string",
        }

    - Response

        Status: 200 OK

        {
        "session_id": "string",
        "ended_at": "datetime",
        "total_pages": 10
        }
    """

    def post(self, request):
        session_id = request.data.get("session_id")

        if not session_id:
            return Response(status=status.HTTP_400_BAD_REQUEST)

        try:
            session = Session.objects.get(id=session_id)
            session.isOngoing = False
            session.ended_at = timezone.now()
            session.save()

            return Response(
                {
                    "session_id": str(session.id),
                    "ended_at": session.ended_at,
                    "total_pages": session.totalPages,
                },
                status=status.HTTP_200_OK,
            )
        except Session.DoesNotExist:
            return Response(
                {"error_code": 404, "message": "SESSION__NOT_FOUND"},
                status=status.HTTP_404_NOT_FOUND,
            )


class GetSessionInfoView(APIView):
    """
    [GET] /session/info

    Endpoint: /session/info

    - Request (GET)

        {
        "session_id": "string",
        }

    - Response

        Status: 200 OK

        {
        "session_id": "string",
        "user_id": "string",
        "started_at": "datetime",
        "ended_at": "datetime",
        "total_pages": "integer",
        "total_time_spent": "integer",
        "total_words_read": "integer",
        // other details to encourage readers
        ]
        }
    """

    def get(self, request):
        session_id = request.query_params.get("session_id")
        print("[DEBUG] Fetching info for session_id:", session_id)
        if not session_id:
            return Response(status=status.HTTP_400_BAD_REQUEST)

        try:
            session = Session.objects.get(id=session_id)
            return Response(
                {
                    "session_id": str(session.id),
                    "user_id": str(session.user.uid),
                    "voice_style": session.voicePreference,
                    "isOngoing": session.isOngoing,
                    "started_at": session.created_at,
                    "ended_at": session.ended_at,
                    "total_pages": session.totalPages,
                    "total_time_spent": ((session.ended_at or timezone.now()) - session.created_at).seconds,
                    "total_words_read": session.totalWords
                },
                status=status.HTTP_200_OK,
            )
        except Session.DoesNotExist:
            return Response(
                {"error_code": 404, "message": "SESSION__NOT_FOUND"},
                status=status.HTTP_404_NOT_FOUND,
            )


class GetSessionStatsView(APIView):
    """
    [GET] /session/stats

    Endpoint: /session/stats

    - Request (GET)

        {
        "session_id": "string",
        }

    - Response

        Status: 200 OK

        {
        "session_id": "string",
        "user_id": "string",
        "page_index": 5,
        "voice_style": "string",
        "started_at": "datetime",
        "ended_at": null
        "total_pages": null
        }
    """

    def get(self, request):
        session_id = request.query_params.get("session_id")
        print("[DEBUG] Fetching info for session_id:", session_id)
        if not session_id:
            return Response(status=status.HTTP_400_BAD_REQUEST)

        try:
            session = Session.objects.get(id=session_id)
            duration = None
            if session.ended_at:
                duration = (session.ended_at - session.created_at).seconds

            return Response(
                {
                    "session_id": str(session.id),
                    "user_id": str(session.user.uid),
                    "voice_style": session.voicePreference,
                    "isOngoing": session.isOngoing,
                    "started_at": session.created_at,
                    "ended_at": session.ended_at,
                    "total_pages": session.totalPages,
                    "total_time_spent": ((session.ended_at or timezone.now()) - session.created_at).seconds,
                    "total_words_read": session.totalWords
                },
                status=status.HTTP_200_OK,
            )
        except Session.DoesNotExist:
            return Response(status=status.HTTP_404_NOT_FOUND)


class SessionReviewView(APIView):
    """
    [GET] /session/review

    Endpoint: /session/review

    - Request (GET)

        {
        "session_id": "string",
        }

    - Response

        Status: 200 OK

        {
        "session_id": "string",
        "user_id": "string",
        "started_at": "datetime",
        "ended_at": "datetime",
        "total_pages": "integer",
        }
    """

    def get(self, request):
        session_id = request.query_params.get("session_id")
        if not session_id:
            return Response(status=status.HTTP_400_BAD_REQUEST)

        try:
            session = Session.objects.get(id=session_id)
            return Response(
                {
                    "session_id": str(session.id),
                    "user_id": str(session.user.uid),
                    "started_at": session.created_at,
                    "ended_at": session.ended_at,
                    "total_pages": session.totalPages,
                },
                status=status.HTTP_200_OK,
            )
        except Session.DoesNotExist:
            return Response(status=status.HTTP_404_NOT_FOUND)


class SessionReloadView(APIView):
    """
    [GET] /session/reload
    - created_at(=started_at) 기준으로 세션을 찾아
      session_id 및 첫 페이지 데이터를 반환 (이어보기용)

    - Request Example:
        GET /session/reload?user_id=xxx&started_at=2025-11-06T04:30:00Z&page_index=0

    - Response Example:
        {
            "session_id": "string",
            "page_index": 0,
            "image_base64": "string or null",
            "translation_text": "string or null",
            "audio_url": "string or null"
        }
    """

    def get(self, request):
        user_id = request.query_params.get("user_id")
        started_at = request.query_params.get("started_at")
        page_index = int(request.query_params.get("page_index", 0))

        if not user_id or not started_at:
            return Response(
                {"error": "Missing user_id or started_at"},
                status=status.HTTP_400_BAD_REQUEST,
            )

        try:
            user = User.objects.get(uid=user_id)
        except User.DoesNotExist:
            return Response(
                {"error_code": 404, "message": "USER__NOT_FOUND"},
                status=status.HTTP_404_NOT_FOUND,
            )

        # created_at으로 세션 찾기
        parsed_time = parse_datetime(started_at)
        if not parsed_time:
            return Response(
                {"error": "Invalid started_at format"},
                status=status.HTTP_400_BAD_REQUEST,
            )

        try:
            session = Session.objects.filter(user=user, created_at=parsed_time).first()
            if not session:
                return Response(
                    {"error_code": 404, "message": "SESSION__NOT_FOUND"},
                    status=status.HTTP_404_NOT_FOUND,
                )

            pages = Page.objects.filter(session=session).order_by("created_at")
            if not pages.exists():
                return Response(
                    {
                        "session_id": str(session.id),
                        "page_index": page_index,
                        "image_base64": None,
                        "translation_text": None,
                        "audio_url": None,
                    },
                    status=status.HTTP_200_OK,
                )

            page = pages[page_index] if page_index < len(pages) else pages.first()

            # 이미지 base64 변환
            try:
                with open(page.img_url, "rb") as f:
                    encoded = base64.b64encode(f.read()).decode("utf-8")
            except Exception:
                encoded = None

            return Response(
                {
                    "session_id": str(session.id),
                    "page_index": page_index,
                    "image_base64": encoded,
                    "translation_text": page.translation_text,
                    "audio_url": page.audio_url,
                },
                status=status.HTTP_200_OK,
            )

        except Exception as e:
            print("[DEBUG] Reload error:", e)
            return Response(
                {"error": str(e)},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR,
            )


class SessionReloadAllView(APIView):
    def get(self, request):
        user_id = request.query_params.get("user_id")
        started_at = request.query_params.get("started_at")

        if not user_id or not started_at:
            return Response(
                {"error": "Missing user_id or started_at"},
                status=status.HTTP_400_BAD_REQUEST,
            )

        try:
            user = User.objects.get(device_info=user_id)
        except User.DoesNotExist:
            return Response(
                {"error_code": 404, "message": "USER__NOT_FOUND"},
                status=status.HTTP_404_NOT_FOUND,
            )

        # created_at으로 세션 찾기
        parsed_time = parse_datetime(started_at)
        if not parsed_time:
            return Response(
                {"error": "Invalid started_at format"},
                status=status.HTTP_400_BAD_REQUEST,
            )

        try:
            session = Session.objects.filter(user=user, created_at=parsed_time).first()
            if not session:
                return Response(
                    {"error_code": 404, "message": "SESSION__NOT_FOUND"},
                    status=status.HTTP_404_NOT_FOUND,
                )

            pages_data = []
            for page in session.pages.all().order_by("id"):
                page_info = {
                    "page_index": page.id,
                    "img_url": page.img_url,
                    "translation_text": page.translation_text,
                    "audio_url": page.audio_url,
                    "ocr_results": [
                        {
                            "bbox": bb.coordinates,
                            "original_txt": bb.original_text,
                            "translation_txt": bb.translated_text,
                        }
                        for bb in page.bbs.all()
                    ],
                }
                pages_data.append(page_info)

            return Response(
                {
                    "session_id": str(session.id),
                    "started_at": session.created_at.isoformat(),
                    "pages": pages_data,
                },
                status=status.HTTP_200_OK,
            )

        except Exception as e:
            print("[DEBUG][ReloadAll]", e)
            return Response(
                {"error": "Internal server error"},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR,
            )


class DiscardSessionView(APIView):
    """
    [POST] /session/discard
    - 세션과 관련된 모든 데이터를 삭제 (Session, Pages, BBs, image files)

    - Request (POST)
        {
            "session_id": "string"
        }

    - Response
        Status: 200 OK
        {
            "message": "Session discarded successfully"
        }
    """

    def post(self, request):
        session_id = request.data.get("session_id")

        if not session_id:
            return Response(
                {"error": "Missing session_id"},
                status=status.HTTP_400_BAD_REQUEST,
            )

        try:
            session = Session.objects.get(id=session_id)

            # Delete all associated image files
            pages = session.pages.all()
            for page in pages:
                if page.img_url and os.path.exists(page.img_url):
                    try:
                        os.remove(page.img_url)
                    except Exception as e:
                        print(f"[DEBUG] Failed to delete image {page.img_url}: {e}")

            # Delete session (CASCADE will delete Pages and BBs automatically)
            session.delete()

            return Response(
                {"message": "Session discarded successfully"},
                status=status.HTTP_200_OK,
            )

        except Session.DoesNotExist:
            return Response(
                {"error_code": 404, "message": "SESSION__NOT_FOUND"},
                status=status.HTTP_404_NOT_FOUND,
            )
        except Exception as e:
            print("[DEBUG] Discard error:", e)
            return Response(
                {"error": str(e)},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR,
            )
