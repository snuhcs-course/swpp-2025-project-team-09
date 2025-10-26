from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework import status
from django.utils import timezone
from apis.models.session_model import Session
from apis.models.user_model import User
import uuid

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
            user = User.objects.get(uid=user_id)
            session = Session.objects.create(
                user=user,
                title=f"Reading Session {timezone.now().strftime('%Y-%m-%d %H:%M')}",
                created_at=timezone.now(),
                totalPages=0,
                isOngoing=True
            )
            return Response({
                "session_id": str(session.id),
                "started_at": session.created_at,
                "page_index": page_index
            }, status=status.HTTP_200_OK)
        except User.DoesNotExist:
            return Response({
                "error_code": 404,
                "message": "USER__NOT_FOUND"
            }, status=status.HTTP_404_NOT_FOUND)
        except Exception as e:
            return Response({"error": str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)


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
            return Response({
                "session_id": str(session.id),
                "voice_style": session.voicePreference
            }, status=status.HTTP_200_OK)
        except Session.DoesNotExist:
            return Response({
                "error_code": 404,
                "message": "SESSION__NOT_FOUND"
            }, status=status.HTTP_404_NOT_FOUND)


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

            return Response({
                "session_id": str(session.id),
                "ended_at": session.ended_at,
                "total_pages": session.totalPages
            }, status=status.HTTP_200_OK)
        except Session.DoesNotExist:
            return Response({
                "error_code": 404,
                "message": "SESSION__NOT_FOUND"
            }, status=status.HTTP_404_NOT_FOUND)


class GetSessionInfoView(APIView):
    """
    [GET] /session/info
    
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
        if not session_id:
            return Response(status=status.HTTP_400_BAD_REQUEST)

        try:
            session = Session.objects.get(id=session_id)
            return Response({
                "session_id": str(session.id),
                "user_id": str(session.user.uid),
                "voice_style": session.voicePreference,
                "isOngoing": session.isOngoing,
                "started_at": session.created_at,
                "ended_at": session.ended_at,
                "total_pages": session.totalPages
            }, status=status.HTTP_200_OK)
        except Session.DoesNotExist:
            return Response({
                "error_code": 404,
                "message": "SESSION__NOT_FOUND"
            }, status=status.HTTP_404_NOT_FOUND)


class GetSessionStatsView(APIView):
    """
    [GET] /session/stats
    
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
        "page_index": 5,
        "voice_style": "string",
        "started_at": "datetime",
        "ended_at": null
        "total_pages": null
        }
    """

    def get(self, request):
        session_id = request.query_params.get("session_id")
        if not session_id:
            return Response(status=status.HTTP_400_BAD_REQUEST)

        try:
            session = Session.objects.get(id=session_id)
            duration = None
            if session.ended_at:
                duration = (session.ended_at - session.created_at).seconds

            return Response({
                "session_id": str(session.id),
                "user_id": str(session.user.uid),
                "started_at": session.created_at,
                "ended_at": session.ended_at,
                "total_pages": session.totalPages,
                "total_time_spent": duration,
                "total_words_read": session.totalPages * 100  # dummy metric
            }, status=status.HTTP_200_OK)
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
            return Response({
                "session_id": str(session.id),
                "user_id": str(session.user.uid),
                "started_at": session.created_at,
                "ended_at": session.ended_at,
                "total_pages": session.totalPages
            }, status=status.HTTP_200_OK)
        except Session.DoesNotExist:
            return Response(status=status.HTTP_404_NOT_FOUND)
