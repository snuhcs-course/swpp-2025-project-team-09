DATABASES = {
    'default': {
        'ENGINE': 'django.db.backends.postgresql',
        'NAME': 'storybridge-db',
        'USER': 'sb_admin',
        'PASSWORD': 'storygridge1234',
        'HOST': 'storybridge-db.c9mckwquq188.ap-northeast-2.rds.amazonaws.com',
        'PORT': '5432',
    }
}

ALLOWED_HOSTS = ['ec2-3-36-128-110.ap-northeast-2.compute.amazonaws.com']