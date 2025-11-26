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
    Start a new reading session

    [POST] /session/start

    Request Body:
        {
            "user_id": "string",
            "page_index": 0
        }

    Response (200 OK):
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
    Set voice preference for session

    [POST] /session/voice

    Request Body:
        {
            "session_id": "string",
            "voice_style": "string"
        }

    Response (200 OK):
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
    End an ongoing session

    [POST] /session/end

    Request Body:
        {
            "session_id": "string"
        }

    Response (200 OK):
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


class GetSessionStatsView(APIView):
    """
    Get session statistics

    [GET] /session/stats?session_id={session_id}

    Query Parameters:
        session_id: Session identifier

    Response (200 OK):
        {
            "session_id": "string",
            "user_id": "string",
            "voice_style": "string",
            "isOngoing": true,
            "started_at": "datetime",
            "ended_at": "datetime or null",
            "total_pages": 5,
            "total_time_spent": 120,
            "total_words_read": 500
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
                    "started_at": session.started_at,
                    "ended_at": session.ended_at,
                    "total_pages": session.totalPages,
                    "total_time_spent": ((session.ended_at or timezone.now()) - session.started_at).seconds,
                    "total_words_read": session.totalWords
                },
                status=status.HTTP_200_OK,
            )
        except Session.DoesNotExist:
            return Response(status=status.HTTP_404_NOT_FOUND)

class SessionReloadAllView(APIView):
    """
    Reload all session data for resuming reading

    [GET] /session/reload?user_id={user_id}&started_at={started_at}

    Query Parameters:
        user_id: Device identifier
        started_at: Session creation time (ISO format)

    Response (200 OK):
        {
            "session_id": "string",
            "started_at": "datetime",
            "pages": [
                {
                    "page_index": 0,
                    "img_url": "string",
                    "translation_text": "string",
                    "audio_url": "string",
                    "ocr_results": [
                        {
                            "bbox": {},
                            "original_txt": "string",
                            "translation_txt": "string"
                        }
                    ]
                }
            ]
        }
    """
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
            session.started_at = timezone.now()
            session.save(update_fields=["started_at"])
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
    Delete session and all associated data

    [POST] /session/discard

    Request Body:
        {
            "session_id": "string"
        }

    Response (200 OK):
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
