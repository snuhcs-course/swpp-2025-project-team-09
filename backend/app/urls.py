from django.contrib import admin
from django.urls import path, include

# Define url patten
# You don't have to change this setting
urlpatterns = [
    path('admin/', admin.site.urls),
    path('', include('apis.urls'))
]
