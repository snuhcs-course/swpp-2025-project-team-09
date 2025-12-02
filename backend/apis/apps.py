from django.apps import AppConfig

class ApisConfig(AppConfig):
    default_auto_field = "django.db.models.BigAutoField"
    name = "apis"

    def ready(self):
        """
        Load profanity lists when Django app is ready.
        This ensures profanity lists are loaded for all Django contexts
        (runserver, shell, tests, etc.)
        """
        from .modules.profanity_check import load_profanity_lists

        load_profanity_lists()
