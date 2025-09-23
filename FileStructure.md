📂 StoryBridge
 ├── 📂 android-app                  # Android Studio 프로젝트
 │    ├── app/src/main/java/com/storybridge/app/   # Kotlin/Java 코드
 │    │    ├── MainActivity.kt       # 앱 진입점
 │    │    ├── network/              # Retrofit API 정의
 │    │    └── ui/                   # 화면 (Activity, Fragment)
 │    ├── app/src/main/res/          # 레이아웃 XML, 이미지, 문자열 리소스
 │    ├── app/src/test/java/...      # 단위 테스트 (JUnit)
 │    ├── app/src/androidTest/java/... # UI 테스트 (Espresso)
 │    ├── build.gradle
 │    └── settings.gradle
 │
 ├── 📂 backend                      # Django/FastAPI 서버 (API 중계)
 │    ├── 📂 src
 │    │    ├── 📂 app                # Django 메인 앱 (urls.py, settings.py)
 │    │    ├── 📂 apis               # 실제 엔드포인트 (REST API)
 │    │    │    ├── camera_api/
 │    │    │    ├── ocr_api/
 │    │    │    ├── translation_api/
 │    │    │    └── tts_api/         # (추후 확장)
 │    │    ├── 📂 services           # GPT/외부 API 호출 모듈
 │    │    │    ├── camera/
 │    │    │    ├── ocr/
 │    │    │    ├── translation/
 │    │    │    └── tts/             # (추후 확장)
 │    │    ├── config.py             # ✅ .env 불러오기 (API 키 관리)
 │    │    ├── __init__.py
 │    │    └── urls.py
 │    │
 │    ├── 📂 tests                   # 백엔드 테스트
 │    │    ├── unit/                 # 함수/모듈 단위 테스트
 │    │    ├── integration/          # 모듈 연동/flow 테스트
 │    │    ├── system/               # 전체 시스템 테스트 (추후 바깥 폴더로 분리)
 │    │    ├── uat/                  # 사용자 인수 테스트 (추후 바깥 폴더로 분리)
 │    │    │    ├── qa/              # 내부 QA 시나리오 
 │    │    │    └── real_users/      # 실제 사용자 테스트
 │    │    └── conftest.py           # 공통 fixture
 │    │
 │    ├── requirements.txt
 │    ├── manage.py
 │    └── .env                       # API 키 (gitignore 처리)
 │
 ├── 📂 ci                           # CI/CD 관련
 │    ├── github-actions/            # GitHub Actions 워크플로우
 │    │    ├── android-build.yml     # 안드로이드 빌드 & 테스트
 │    │    └── backend-test.yml      # 백엔드 pytest 실행
 │    └── docker/                    # Dockerfile (백엔드 환경)
 │
 ├── 📂 data                         # Predrick 데이터셋
 │    ├── korean/
 │    ├── vietnamese/
 │    └── english/
 │
 ├── 📂 docs                         # 문서 (추후 파일 추가/폴더 삭제)
 │
 ├── 📂 logs                         # 실행/에러 로그 (gitignore 처리 권장)
 │    ├── app/
 │    └── backend/
 │
 ├── 📂 reports                      # 테스트/메트릭 결과 (추후 파일 추가/폴더 삭제)
 │    
 ├── 📂 scripts                      # 유틸리티 스크립트
 │    ├── preprocess_data.py
 │    ├── run_local.sh
 │    └── deploy.sh
 │
 ├── .gitignore
 └── README.md