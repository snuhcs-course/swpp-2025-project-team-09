from django.urls import path, include

urlpatterns = [
    # routing to user controller
    path("user/", include("apis.controller.user_controller.urls")),

    # routing to session controller
    path("session/", include("apis.controller.session_controller.urls")),

    # routing to process controller (OCR / TTS)
    path("process/", include("apis.controller.process_controller.urls")),

    # routing to page controller
    path("page/", include("apis.controller.page_controller.urls")),
]
