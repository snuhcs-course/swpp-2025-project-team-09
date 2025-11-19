from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework import status
from django.utils import timezone
from apis.models.user_model import User
from apis.models.session_model import Session
from apis.models.page_model import Page
import base64


class UserRegisterView(APIView):
    """
    [POST] /user/register
    - 앱 최초 실행 시 단말 정보를 기반으로 사용자 등록

        - Request (POST)

            {
            "device_info": "string",
            "language_preference": "string"
            }

        - Response

            Status: 200 OK

            {
            "user_id": "string",
            "language_preference": "string"
            }
    """

    def post(self, request):
        """ """
        device_info = request.data.get("device_info")
        lang = request.data.get("language_preference")

        if not device_info or not lang:
            return Response(
                {"error_code": 400, "message": "USER__INVALID_REQUEST_BODY"},
                status=status.HTTP_400_BAD_REQUEST,
            )

        if User.objects.filter(device_info=device_info).exists():
            return Response(
                {"error_code": 409, "message": "USER__DEVICE_ALREADY_REGISTERED"},
                status=status.HTTP_409_CONFLICT,
            )

        try:
            user = User.objects.create(
                device_info=device_info,
                language_preference=lang,
                created_at=timezone.now(),
            )
            return Response(
                {"user_id": user.uid, "language_preference": user.language_preference},
                status=status.HTTP_200_OK,
            )
        except Exception:
            return Response(
                {"error_code": 500, "message": "SERVER__INTERNAL_ERROR"},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR,
            )


class UserLoginView(APIView):
    """
    Endpoint: /user/login

    - Request (POST)

        {
        "device_info": "string",
        }
        Response

    - Status: 200 OK

        {
        "user_id": "string",
        "language_preference": "string"
        }
    """

    def post(self, request):
        device_info = request.data.get("device_info")

        if not device_info:
            return Response(status=status.HTTP_400_BAD_REQUEST)

        try:
            user = User.objects.get(device_info=device_info)
            return Response(
                {"user_id": user.uid, "language_preference": user.language_preference},
                status=status.HTTP_200_OK,
            )
        except User.DoesNotExist:
            return Response(
                {"error_code": 404, "message": "USER__DEVICE_NOT_REGISTERED"},
                status=status.HTTP_404_NOT_FOUND,
            )
        except Exception:
            return Response(status=status.HTTP_500_INTERNAL_SERVER_ERROR)


class UserChangeLangView(APIView):
    """
    [PATCH] /user/lang
    - 사용자 언어 설정 변경

    Endpoint: /user/lang

    - Request (PATCH)

        {
        "device_info": "string",
        "language_preference": "string"
        }

    - Response

        Status: 200 OK

        {
        "user_id": "string",
        "language_preference": "string",
        "updated_at": "datetime"
        }
    """

    def patch(self, request):
        device_info = request.data.get("device_info")
        lang = request.data.get("language_preference")

        if not device_info or not lang:
            return Response(status=status.HTTP_400_BAD_REQUEST)

        try:
            user = User.objects.get(device_info=device_info)
            user.language_preference = lang
            user.updated_at = timezone.now()
            user.save()

            return Response(
                {
                    "user_id": user.uid,
                    "language_preference": user.language_preference,
                    "updated_at": user.updated_at,
                },
                status=status.HTTP_200_OK,
            )
        except User.DoesNotExist:
            return Response(
                {"error_code": 404, "message": "USER__DEVICE_NOT_REGISTERED"},
                status=status.HTTP_404_NOT_FOUND,
            )
        except Exception:
            return Response(status=status.HTTP_500_INTERNAL_SERVER_ERROR)


class UserInfoView(APIView):
    """
    [GET] /user/info
    - 사용자의 전체 읽기 기록(책, 이미지, 시작시간) 조회

    Endpoint: /user/info

    - Request Example:
        GET /user/info?device_info=SM-S901N

    - Response

        Status: 200 OK

        {
        "user_id": "string",
        "title": "string",
        "image_base64": "string",
        "started_at": "datetime"
        }
    """

    def get(self, request):
        device_info = request.query_params.get("device_info")
        if not device_info:
            return Response(status=status.HTTP_400_BAD_REQUEST)

        try:
            user = User.objects.get(device_info=device_info)
            sessions = Session.objects.filter(user=user)
            if not sessions.exists():
                return Response([], status=status.HTTP_200_OK)

            result = []
            for session in sessions:
                pages = Page.objects.filter(session=session)
                if pages.exists():
                    first_page = pages.first()
                    image_path = first_page.img_url  # 필드명 맞게 변경
                    try:
                        with open(image_path, "rb") as img_file:
                            encoded = base64.b64encode(img_file.read())
                            image_base64 = encoded.decode("utf-8")
                    except FileNotFoundError:
                        image_base64 = None
                else:
                    image_base64 = None

                result.append(
                    {
                        "user_id": user.uid,
                        "session_id": session.id,
                        "title": session.title,
                        "translated_title": session.translated_title,
                        "image_base64": image_base64,
                        "started_at": session.created_at,
                    }
                )
            print(session.translated_title)

            return Response(result, status=status.HTTP_200_OK)

        except User.DoesNotExist:
            return Response(
                {"error_code": 404, "message": "USER__DEVICE_NOT_REGISTERED"},
                status=status.HTTP_404_NOT_FOUND,
            )
        except Exception as e:
            print("[DEBUG] UserInfoView error:", e)
            return Response(status=status.HTTP_500_INTERNAL_SERVER_ERROR)
