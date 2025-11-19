# 📂 StoryBridge 프로젝트 구조

> 다국어 스토리텔링 앱 - OCR, 번역, 감정 기반 TTS를 활용한 독서 보조 애플리케이션

---

## 📱 android-app (안드로이드 애플리케이션)

```
android-app/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/storybridge_android/
│   │   │   │
│   │   │   ├── StoryBridgeApplication.kt          # 앱 진입점 (Application 클래스)
│   │   │   ├── ServiceLocator.kt                  # 의존성 주입 (DI/서비스 로케이터)
│   │   │   │
│   │   │   ├── 📂 data/                           # Repository 레이어 (MVVM 패턴)
│   │   │   │   ├── PageRepository.kt              # 페이지 데이터 인터페이스
│   │   │   │   ├── PageRepositoryImpl.kt          # 페이지 Repository 구현체
│   │   │   │   ├── SessionRepository.kt           # 세션 데이터 인터페이스
│   │   │   │   ├── SessionRepositoryImpl.kt       # 세션 Repository 구현체
│   │   │   │   ├── ProcessRepository.kt           # OCR/처리 데이터 인터페이스
│   │   │   │   ├── ProcessRepositoryImpl.kt       # OCR/처리 Repository 구현체
│   │   │   │   ├── UserRepository.kt              # 사용자 데이터 인터페이스
│   │   │   │   └── UserRepositoryImpl.kt          # 사용자 Repository 구현체
│   │   │   │
│   │   │   ├── 📂 network/                        # Retrofit API 클라이언트
│   │   │   │   ├── RetrofitClient.kt              # Retrofit 인스턴스 설정
│   │   │   │   ├── PageApi.kt                     # 페이지 API 엔드포인트
│   │   │   │   ├── SessionApi.kt                  # 세션 API 엔드포인트
│   │   │   │   ├── ProcessApi.kt                  # OCR/처리 API 엔드포인트
│   │   │   │   └── UserApi.kt                     # 사용자 API 엔드포인트
│   │   │   │
│   │   │   ├── 📂 ui/                             # UI 레이어 (Activity & ViewModel)
│   │   │   │   │
│   │   │   │   ├── 📂 landing/                    # 온보딩/랜딩 화면
│   │   │   │   │   ├── LandingActivity.kt         # 랜딩 페이지 Activity
│   │   │   │   │   ├── LandingViewModel.kt        # 랜딩 ViewModel
│   │   │   │   │   └── LandingViewModelFactory.kt # ViewModel Factory
│   │   │   │   │
│   │   │   │   ├── 📂 main/                       # 메인 화면 (세션 목록)
│   │   │   │   │   ├── MainActivity.kt            # 메인 Activity
│   │   │   │   │   ├── MainViewModel.kt           # 메인 ViewModel
│   │   │   │   │   └── MainViewModelFactory.kt    # ViewModel Factory
│   │   │   │   │
│   │   │   │   ├── 📂 camera/                     # 카메라/OCR 캡처
│   │   │   │   │   ├── CameraActivity.kt          # 카메라 촬영 Activity
│   │   │   │   │   ├── CameraViewModel.kt         # 카메라 ViewModel
│   │   │   │   │   ├── CameraViewModelFactory.kt  # ViewModel Factory
│   │   │   │   │   ├── CameraSessionActivity.kt   # 세션 중 카메라 Activity
│   │   │   │   │   ├── CameraSessionViewModel.kt  # 세션 카메라 ViewModel
│   │   │   │   │   └── CameraSessionViewFactory.kt# 세션 카메라 Factory
│   │   │   │   │
│   │   │   │   ├── 📂 session/                    # 세션 관리 플로우
│   │   │   │   │   ├── StartSessionActivity.kt    # 세션 시작 화면
│   │   │   │   │   ├── StartSessionViewModel.kt
│   │   │   │   │   ├── StartSessionViewModelFactory.kt
│   │   │   │   │   ├── VoiceSelectActivity.kt     # TTS 목소리 선택
│   │   │   │   │   ├── VoiceSelectViewModel.kt
│   │   │   │   │   ├── VoiceSelectViewModelFactory.kt
│   │   │   │   │   ├── ContentInstructionActivity.kt # 콘텐츠 안내 화면
│   │   │   │   │   ├── ContentInstructionViewModel.kt
│   │   │   │   │   ├── ContentInstructionViewModelFactory.kt
│   │   │   │   │   ├── LoadingActivity.kt         # 로딩 화면
│   │   │   │   │   ├── LoadingViewModel.kt
│   │   │   │   │   ├── LoadingViewModelFactory.kt
│   │   │   │   │   ├── DecideSaveActivity.kt      # 세션 저장 여부 결정
│   │   │   │   │   ├── FinishActivity.kt          # 세션 완료 화면
│   │   │   │   │   ├── FinishViewModel.kt
│   │   │   │   │   └── FinishViewModelFactory.kt
│   │   │   │   │
│   │   │   │   ├── 📂 reading/                    # 독서/콘텐츠 표시 화면
│   │   │   │   │   ├── ReadingActivity.kt         # 독서 화면 Activity
│   │   │   │   │   ├── ReadingViewModel.kt        # 독서 ViewModel
│   │   │   │   │   ├── ReadingViewModelFactory.kt # ViewModel Factory
│   │   │   │   │   └── ThumbnailAdapter.kt        # 페이지 썸네일 어댑터
│   │   │   │   │
│   │   │   │   ├── 📂 setting/                    # 설정 화면
│   │   │   │   │   ├── SettingActivity.kt         # 설정 Activity
│   │   │   │   │   ├── SettingViewModel.kt        # 설정 ViewModel
│   │   │   │   │   ├── SettingViewModelFactory.kt # ViewModel Factory
│   │   │   │   │   └── AppSettings.kt             # SharedPreferences 관리
│   │   │   │   │
│   │   │   │   └── 📂 common/                     # 공통 UI 컴포넌트
│   │   │   │       ├── BottomNav.kt               # 하단 네비게이션
│   │   │   │       ├── TopNav.kt                  # 상단 네비게이션
│   │   │   │       ├── TopNavigationBar.kt        # 상단 타이틀바
│   │   │   │       ├── SessionCard.kt             # 세션 카드 아이템
│   │   │   │       └── LeftOverlay.kt             # 좌측 패널 오버레이
│   │   │   │
│   │   │   └── 📂 util/                           # 유틸리티 함수
│   │   │       └── ImageUtil.kt                   # 이미지 처리 유틸
│   │   │
│   │   ├── 📂 res/                                # 안드로이드 리소스
│   │   │   ├── layout/                            # XML 레이아웃 파일
│   │   │   │   ├── activity_landing_first.xml
│   │   │   │   ├── activity_landing_second.xml
│   │   │   │   ├── activity_camera.xml
│   │   │   │   ├── activity_camera_session.xml
│   │   │   │   ├── activity_voice_select.xml
│   │   │   │   ├── activity_content_instruction.xml
│   │   │   │   ├── activity_start_session.xml
│   │   │   │   ├── activity_loading.xml
│   │   │   │   ├── activity_decide_save.xml
│   │   │   │   ├── activity_finish.xml
│   │   │   │   ├── activity_main.xml
│   │   │   │   ├── activity_reading.xml
│   │   │   │   ├── activity_setting.xml
│   │   │   │   ├── bottom_nav.xml
│   │   │   │   ├── top_nav.xml
│   │   │   │   ├── top_navigation_bar.xml
│   │   │   │   ├── left_panel.xml
│   │   │   │   ├── card_item.xml
│   │   │   │   └── item_thumbnail.xml
│   │   │   │
│   │   │   ├── drawable/                          # 드로어블 리소스
│   │   │   │   ├── big_button.xml
│   │   │   │   ├── rounded_button.xml
│   │   │   │   ├── rounded_button_selector.xml
│   │   │   │   ├── card_background.xml
│   │   │   │   ├── bbox_background.xml
│   │   │   │   ├── gradient_background.xml
│   │   │   │   ├── progress_drawable.xml
│   │   │   │   ├── play_triangle.xml
│   │   │   │   └── (기타 아이콘 및 도형)
│   │   │   │
│   │   │   ├── color/                             # 색상 리소스
│   │   │   ├── values/                            # 문자열, 치수, 스타일
│   │   │   ├── values-night/                      # 다크 모드 테마
│   │   │   ├── values-zh/                         # 중국어 현지화
│   │   │   ├── font/                              # 커스텀 폰트
│   │   │   ├── mipmap-*/                          # 앱 아이콘 (다양한 해상도)
│   │   │   └── xml/                               # 설정 파일
│   │   │       ├── network_security_config.xml
│   │   │       ├── backup_rules.xml
│   │   │       └── data_extraction_rules.xml
│   │   │
│   │   └── AndroidManifest.xml                    # 앱 매니페스트 (권한, Activity 선언)
│   │
│   ├── src/androidTest/java/                      # 안드로이드 계측 테스트
│   │   └── LandingActivityMokitoTest.kt
│   │
│   ├── build.gradle.kts                           # 앱 수준 Gradle 빌드 파일
│   └── settings.gradle.kts                        # 프로젝트 수준 Gradle 설정
│
└── build/                                         # 빌드 출력물 (자동 생성)
```

### 안드로이드 앱 주요 특징
- **아키텍처**: MVVM 패턴 (ViewModel + Repository)
- **네트워킹**: Retrofit2 기반 REST API 통신
- **의존성 주입**: ServiceLocator 패턴
- **주요 기능**:
  - 카메라 촬영 → OCR 텍스트 인식
  - 실시간 번역 (다국어 지원)
  - 감정 기반 TTS (남성/여성 목소리 선택)
  - 세션 관리 (독서 기록 저장)
  - AR 오버레이 (원문 위에 번역 표시)

---

## 🖥️ backend (Django REST API 서버)

```
backend/
├── 📂 app/                                    # Django 프로젝트 설정
│   ├── __init__.py
│   ├── settings.py                            # Django 설정 (DB, Installed Apps)
│   ├── urls.py                                # 루트 URL 라우팅
│   ├── asgi.py                                # ASGI 설정 (비동기)
│   └── wsgi.py                                # WSGI 설정 (배포용)
│
├── 📂 apis/                                   # 메인 Django 앱 (API 로직)
│   ├── __init__.py
│   ├── admin.py                               # Django 관리자 설정
│   ├── apps.py                                # 앱 설정
│   ├── urls.py                                # API URL 라우팅
│   │
│   ├── 📂 models/                             # 데이터베이스 모델
│   │   ├── __init__.py
│   │   ├── user_model.py                      # 사용자 모델 (인증, 환경설정)
│   │   ├── session_model.py                   # 세션 모델 (독서 세션)
│   │   ├── page_model.py                      # 페이지 모델 (책 페이지)
│   │   └── bb_model.py                        # 바운딩 박스 모델 (OCR 텍스트 위치)
│   │
│   ├── 📂 controller/                         # API 엔드포인트 (views)
│   │   ├── 📂 user_controller/
│   │   │   ├── urls.py                        # 사용자 엔드포인트 라우팅
│   │   │   └── views.py                       # 사용자 관리 API (로그인, 프로필)
│   │   │
│   │   ├── 📂 session_controller/
│   │   │   ├── urls.py                        # 세션 엔드포인트 라우팅
│   │   │   └── views.py                       # 세션 CRUD 작업
│   │   │
│   │   ├── 📂 page_controller/
│   │   │   ├── urls.py                        # 페이지 엔드포인트 라우팅
│   │   │   └── views.py                       # 페이지 조회 및 관리
│   │   │
│   │   └── 📂 process_controller/
│   │       ├── urls.py                        # 처리 엔드포인트 라우팅
│   │       └── views.py                       # OCR 및 AI 처리 엔드포인트
│   │
│   ├── 📂 modules/                            # AI 처리 모듈
│   │   ├── ocr_processor.py                   # Naver OCR 연동
│   │   └── tts_processor.py                   # TTS 생성 모듈
│   │
│   └── 📂 migrations/                         # 데이터베이스 마이그레이션
│       ├── 0001_initial.py
│       ├── 0002_alter_session_voicepreference_alter_user_table.py
│       ├── 0003_session_translated_title.py
│       ├── 0003_add_tts_status.py
│       ├── 0004_session_cover_tts_female_session_cover_tts_male_and_more.py
│       ├── 0005_merge_20251106_0251.py
│       ├── 0006_remove_session_cover_tts_female_and_more.py
│       └── __init__.py
│
├── 📂 tests/                                  # 테스트 suite
│   ├── 📂 unit/                               # 단위 테스트
│   │   ├── 📂 models/
│   │   │   ├── test_user_model.py
│   │   │   ├── test_session_model.py
│   │   │   ├── test_page_model.py
│   │   │   └── test_BB_model.py
│   │   │
│   │   └── 📂 controller/
│   │       ├── test_user_controller.py
│   │       ├── test_session_controller.py
│   │       ├── test_page_controller.py
│   │       └── test_process_controller.py
│   │
│   └── __init__.py
│
├── manage.py                                  # Django 관리 스크립트
├── run_tests.py                               # 테스트 러너 스크립트
├── requirements.txt                           # Python 의존성
├── settings.py                                # 백엔드 설정 (API 키, DB)
├── pytest.ini                                 # Pytest 설정
├── db.sqlite3                                 # SQLite 데이터베이스
├── .env                                       # 환경 변수 (API 키)
├── .flake8                                    # 코드 스타일 설정
├── .venv/                                     # Python 가상 환경
└── 📂 media/                                  # 사용자 업로드 디렉토리
```

## ⚙️ ci (CI/CD 파이프라인)
```
ci/
├── 📂 github-actions/                         # GitHub Actions 워크플로우
│   ├── android-build.yml                      # 안드로이드 빌드 & 테스트 파이프라인
│   └── backend-test.yml                       # 백엔드 pytest CI/CD
│
└── 📂 docker/                                 # Docker 컨테이너화
    └── .gitkeep
```

---
## 📊 data (언어 데이터 & 콘텐츠)

```
data/
├── 📂 english/                                # 영어 콘텐츠
│   └── .gitkeep
├── 📂 korean/                                 # 한국어 콘텐츠 (primary)
│   └── .gitkeep
└── 📂 vietnamese/                             # 베트남어 현지화 데이터
    └── .gitkeep
```

**용도**: 샘플 책, 번역, 테스트용 다국어 데이터셋 저장

---

## 📚 docs, logs, reports

### 📂 docs (문서)
```
docs/
└── .gitkeep
```

### 📂 logs (런타임 로그)
```
logs/
├── app/                                       # 안드로이드 앱 로그
└── backend/                                   # 백엔드 서버 로그
```

### 📂 reports (테스트 결과 & 메트릭)
```
reports/
└── .gitkeep
```

---

## 🛠️ scripts (유틸리티 스크립트)

```
scripts/
├── deploy.sh                                  # 배포 자동화
├── run_local.sh                               # 로컬 환경 설정
└── utils.py                                   # Python 유틸리티 함수
```

---

## 🗂️ 루트 설정 파일

```
/
├── README.md                                  # 프로젝트 개요 (다국어 스토리텔링)
├── FileStructure.md                           # 디렉토리 구조 문서
├── .gitignore                                 # Git 무시 규칙
├── .gitattributes                             # Git 속성
├── .github/
│   └── pull_request_template.md               # PR 템플릿
├── deploy.sh                                  # 배포 스크립트
├── .vscode/                                   # VS Code 설정
