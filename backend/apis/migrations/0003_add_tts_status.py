from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ("apis", "0002_alter_user_table"),
    ]

    operations = [
        migrations.AddField(
            model_name="bb",
            name="tts_status",
            field=models.CharField(
                max_length=20,
                default="pending",
                choices=[
                    ("pending", "Pending"),
                    ("processing", "Processing"),
                    ("ready", "Ready"),
                    ("failed", "Failed"),
                ],
            ),
        ),
    ]
