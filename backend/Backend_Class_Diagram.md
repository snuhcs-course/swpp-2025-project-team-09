# StoryBridge Backend Class Diagram Information

## Overview
This document contains a comprehensive list of all classes, methods, and relationships for the StoryBridge backend codebase. This information is suitable for creating a detailed class diagram.

---

## 1. MODELS (Database Models)

### 1.1 User Model

**Class: User(models.Model)**
- **Primary Key:** `uid: UUIDField`
- **Fields:**
  - `device_info: CharField(max_length=255, unique=True)`
  - `language_preference: CharField(max_length=20, default="en")`
  - `created_at: DateTimeField`
  - `updated_at: DateTimeField`

- **Methods:**
  - `__str__()` - String representation
  - `getSessions()` - Returns all sessions for the user
  - `deleteSession(session_id)` - Deletes a specific session

- **Relationships:**
  - One-to-Many: User → Session (reverse name: "sessions")

---

### 1.2 Session Model

**Class: Session(models.Model)**
- **Primary Key:** `id: UUIDField`
- **Fields:**
  - `user: ForeignKey(User, on_delete=CASCADE, related_name="sessions")`
  - `title: CharField(max_length=255)`
  - `translated_title: CharField(max_length=255, nullable)`
  - `cover_img_url: TextField(nullable)`
  - `created_at: DateTimeField`
  - `ended_at: DateTimeField(nullable)`
  - `isOngoing: BooleanField(default=True)`
  - `totalPages: IntegerField(default=0)`
  - `totalWords: IntegerField(default=0)`
  - `voicePreference: CharField(max_length=50, nullable)`

- **Methods:**
  - `__str__()` - String representation
  - `getPages()` - Returns all pages in the session
  - `addPage(image_url, index)` - Helper method to add a page

- **Relationships:**
  - Many-to-One: Session → User
  - One-to-Many: Session → Page (reverse name: "pages")

---

### 1.3 Page Model

**Class: Page(models.Model)**
- **Primary Key:** `id: AutoField`
- **Fields:**
  - `session: ForeignKey(Session, on_delete=CASCADE, related_name="pages")`
  - `img_url: TextField(nullable)`
  - `audio_url: TextField(nullable)`
  - `translation_text: TextField(nullable)`
  - `bbox_json: JSONField(default=dict)`
  - `created_at: DateTimeField`

- **Methods:**
  - `__str__()` - String representation
  - `getBBs()` - Returns all bounding boxes for the page
  - `addBB(bbox_list, translated_list, audio_list)` - Helper to add bounding boxes

- **Relationships:**
  - Many-to-One: Page → Session
  - One-to-Many: Page → BB (reverse name: "bbs")

---

### 1.4 BB (Bounding Box) Model

**Class: BB(models.Model)**
- **Primary Key:** `id: AutoField` (implicit)
- **Fields:**
  - `page: ForeignKey(Page, on_delete=CASCADE, related_name="bbs")`
  - `original_text: TextField`
  - `translated_text: TextField(nullable)`
  - `audio_base64: JSONField(default=list)` - Stores list of base64-encoded audio clips
  - `coordinates: JSONField(default=dict)` - Stores {x1, y1, x2, y2, x3, y3, x4, y4}
  - `tts_status: CharField(max_length=20, choices=["pending", "processing", "ready", "failed"])`

- **Methods:**
  - `__str__()` - String representation
  - `updateTranslation(new_text)` - Updates translated text
  - `updateAudio(new_audio_base64)` - Updates audio
  - `updatePosition(new_position)` - Updates coordinates
  - `points` (property) - Returns list of (x, y) coordinate tuples

- **Relationships:**
  - Many-to-One: BB → Page

---

## 2. CONTROLLERS (API Views)

### 2.1 User Controller

#### Class: UserRegisterView(APIView)
- **Endpoint:** POST /user/register
- **Methods:**
  - `post(request)` - Register a new user with device info and language preference
    - Input: `{device_info, language_preference}`
    - Output: `{user_id, language_preference}`
    - Status Codes: 200, 400, 409, 500

#### Class: UserLoginView(APIView)
- **Endpoint:** POST /user/login
- **Methods:**
  - `post(request)` - Login user with device info
    - Input: `{device_info}`
    - Output: `{user_id, language_preference}`
    - Status Codes: 200, 400, 404, 500

#### Class: UserChangeLangView(APIView)
- **Endpoint:** PATCH /user/lang
- **Methods:**
  - `patch(request)` - Update user's language preference
    - Input: `{device_info, language_preference}`
    - Output: `{user_id, language_preference, updated_at}`
    - Status Codes: 200, 400, 404, 500

#### Class: UserInfoView(APIView)
- **Endpoint:** GET /user/info
- **Methods:**
  - `get(request)` - Retrieve all sessions for a user
    - Input: Query params: `{device_info}`
    - Output: List of `{user_id, session_id, title, translated_title, image_base64, started_at}`
    - Status Codes: 200, 400, 404, 500

---

### 2.2 Session Controller

#### Class: StartSessionView(APIView)
- **Endpoint:** POST /session/start
- **Methods:**
  - `post(request)` - Start a new reading session
    - Input: `{user_id, page_index=0}`
    - Output: `{session_id, started_at, page_index}`
    - Status Codes: 200, 400, 404, 500

#### Class: SelectVoiceView(APIView)
- **Endpoint:** POST /session/voice
- **Methods:**
  - `post(request)` - Set voice preference for a session
    - Input: `{session_id, voice_style}`
    - Output: `{session_id, voice_style}`
    - Status Codes: 200, 400, 404

#### Class: EndSessionView(APIView)
- **Endpoint:** POST /session/end
- **Methods:**
  - `post(request)` - End a reading session
    - Input: `{session_id}`
    - Output: `{session_id, ended_at, total_pages}`
    - Status Codes: 200, 400, 404

#### Class: GetSessionInfoView(APIView)
- **Endpoint:** GET /session/info
- **Methods:**
  - `get(request)` - Retrieve detailed session information
    - Input: Query params: `{session_id}`
    - Output: `{session_id, user_id, voice_style, isOngoing, started_at, ended_at, total_pages, total_time_spent, total_words_read}`
    - Status Codes: 200, 400, 404

#### Class: GetSessionStatsView(APIView)
- **Endpoint:** GET /session/stats
- **Methods:**
  - `get(request)` - Get session statistics
    - Input: Query params: `{session_id}`
    - Output: `{session_id, user_id, voice_style, isOngoing, started_at, ended_at, total_pages, total_time_spent, total_words_read}`
    - Status Codes: 200, 400, 404

#### Class: SessionReviewView(APIView)
- **Endpoint:** GET /session/review
- **Methods:**
  - `get(request)` - Get session review information
    - Input: Query params: `{session_id}`
    - Output: `{session_id, user_id, started_at, ended_at, total_pages}`
    - Status Codes: 200, 400, 404

#### Class: SessionReloadView(APIView)
- **Endpoint:** GET /session/reload
- **Methods:**
  - `get(request)` - Reload a session by user_id and started_at timestamp
    - Input: Query params: `{user_id, started_at, page_index}`
    - Output: `{session_id, page_index, image_base64, translation_text, audio_url}`
    - Status Codes: 200, 400, 404, 500

#### Class: SessionReloadAllView(APIView)
- **Endpoint:** GET /session/reload
- **Methods:**
  - `get(request)` - Reload entire session with all pages
    - Input: Query params: `{user_id, started_at}`
    - Output: `{session_id, started_at, pages[]}`
    - Status Codes: 200, 400, 404, 500

#### Class: DiscardSessionView(APIView)
- **Endpoint:** POST /session/discard
- **Methods:**
  - `post(request)` - Delete session and all related data
    - Input: `{session_id}`
    - Output: `{message}`
    - Status Codes: 200, 400, 404, 500

---

### 2.3 Page Controller

#### Class: PageGetImageView(APIView)
- **Endpoint:** GET /page/get_image
- **Methods:**
  - `get(request)` - Retrieve page image as base64
    - Input: Query params: `{session_id, page_index}`
    - Output: `{session_id, page_index, image_base64, stored_at}`
    - Status Codes: 200, 400, 404, 500

#### Class: PageGetOCRView(APIView)
- **Endpoint:** GET /page/get_ocr
- **Methods:**
  - `get(request)` - Retrieve OCR results with bounding boxes and translations
    - Input: Query params: `{session_id, page_index}`
    - Output: `{session_id, page_index, ocr_results[], processed_at}`
    - Status Codes: 200, 400, 404, 500

#### Class: PageGetTTSView(APIView)
- **Endpoint:** GET /page/get_tts
- **Methods:**
  - `get(request)` - Retrieve TTS audio results as base64
    - Input: Query params: `{session_id, page_index}`
    - Output: `{session_id, page_index, audio_results[], generated_at}`
    - Status Codes: 200, 400, 404, 500

---

### 2.4 Process Controller

#### Class: ProcessUploadView(APIView)
- **Endpoint:** POST /process/upload
- **Methods:**
  - `post(request)` - Upload page image, run OCR, translation, and start background TTS
    - Input: `{session_id, lang, image_base64}`
    - Output: `{session_id, page_index, status, submitted_at}`
    - Internal Methods:
      - `_save_image(image_base64, session_id, page_index)` - Saves image to disk
      - `_get_all_translations(tts_module, ocr_result, session_id, page_index)` - Runs translation asynchronously
      - `_create_page_and_bbs(session, image_path, ocr_result, translation_data)` - Creates DB records
      - `_start_background_tts(...)` - Starts TTS in background thread
    - Status Codes: 200, 400, 404, 422

#### Class: CheckOCRStatusView(APIView)
- **Endpoint:** GET /process/check_ocr
- **Methods:**
  - `get(request)` - Check OCR/translation status
    - Input: Query params: `{session_id, page_index}`
    - Output: `{session_id, page_index, status, progress, submitted_at, processed_at}`
    - Status Codes: 200, 400, 404

#### Class: CheckTTSStatusView(APIView)
- **Endpoint:** GET /process/check_tts
- **Methods:**
  - `get(request)` - Check TTS progress
    - Input: Query params: `{session_id, page_index}`
    - Output: `{session_id, page_index, status, progress, submitted_at, processed_at}`
    - Internal Methods:
      - `_has_audio(bb)` - Checks if bounding box has audio
    - Status Codes: 200, 400, 404

#### Class: ProcessUploadCoverView(APIView)
- **Endpoint:** POST /process/upload_cover
- **Methods:**
  - `post(request)` - Upload cover image, run OCR for title, translation, and dual-voice TTS
    - Input: `{session_id, lang, image_base64}`
    - Output: `{session_id, page_index, status, submitted_at, title, translated_title, tts_male, tts_female}`
    - Internal Methods:
      - `_save_image(image_base64, session_id, page_index)` - Saves cover image
      - `_get_all_translations(tts_module, ocr_result, session_id, page_index)` - Gets translations
      - `_create_page_and_bbs(session, image_path, ocr_result, translation_data)` - Creates DB records
      - `_run_async(coroutine)` - Helper to run async code in sync context
    - Status Codes: 200, 400, 404, 422

---

## 3. PROCESSOR/SERVICE MODULES

### 3.1 OCRModule

**Class: OCRModule**
- **Constructor:** `__init__(conf_threshold: float = 0.8)`
- **Attributes:**
  - `api_url: str` - OCR API endpoint URL
  - `secret_key: str` - OCR API secret key
  - `conf_threshold: float` - Confidence threshold for filtering

- **Public Methods:**
  - `process_page(image_path: str) -> List[str]` - Process a page image
    - Returns: List of paragraphs with text and bounding boxes
  - `process_cover_page(image_path: str) -> str` - Process cover page for title extraction
    - Returns: Title text string or None

- **Private Methods:**
  - `_filter_low_confidence(result_json: Dict) -> Dict` - Filters OCR results by confidence
  - `_font_size(result_json: Dict) -> float` - Calculates average font size
  - `_parse_infer_text(result_json: Dict) -> List[str]` - Parses OCR JSON response
    - Uses DBSCAN clustering for paragraph and line detection
    - Returns: List of paragraph objects with text and bbox

**Dependencies:**
- External API: OCR API (probably Naver/Clova OCR)
- Libraries: requests, sklearn.cluster.DBSCAN, numpy

---

### 3.2 TTSModule

**Class: TTSModule**
- **Constructor:** `__init__(out_dir="out_audio", log_dir="log", target_lang: str = "English")`
- **Attributes:**
  - `client: AsyncOpenAI` - OpenAI async client
  - `TTS_MODEL: str` - Main TTS model (gpt-4o-mini-tts)
  - `TTS_MODEL_LITE: str` - Light TTS model (tts-1)
  - `OUT_DIR: Path` - Output directory for audio files
  - `LOG_DIR: Path` - Log directory
  - `CSV_LOG: Path` - CSV log file path
  - `target_lang: str` - Target language (e.g., "English", "Chinese")
  - `llm: ChatOpenAI` - Language model for translation
  - `translation_chain: Chain` - LangChain pipeline for translation
  - `sentiment_chain: Chain` - LangChain pipeline for sentiment analysis

- **Public Async Methods:**
  - `translate(text_with_context: str) -> Dict[str, Any]` - Translate text with retry logic
  - `sentiment(korean_text: str) -> Dict[str, Any]` - Analyze sentiment with retry logic
  - `synthesize_tts(voice, text, instructions, out_path, response_format) -> Tuple[float, bytes]` - Synthesize TTS audio
  - `synthesize_tts_lite(voice, text, out_path, response_format) -> Tuple[float, bytes]` - Light TTS synthesis
  - `get_translations_only(page: Dict) -> Dict[str, Any]` - Get translations without TTS
    - Returns: `{status, sentences[]}`
  - `run_tts_only(translation_data, session_id, page_index, para_index, para_voice) -> List[str]` - Run TTS with pre-computed translations
  - `translate_and_tts_cover(title, session_id, page_index) -> Tuple[str, str]` - Translate title and generate male/female TTS
  - `run_tts_cover_only(translation_data, session_id, page_index, para_index) -> Tuple[str, str]` - TTS for cover titles
  - `process_paragraph(page, log_csv, check_latency, response_format)` - Complete processing pipeline (legacy)

- **Private Methods:**
  - `_create_translation_chain()` - Creates LangChain pipeline for translation
  - `_create_sentiment_chain()` - Creates LangChain pipeline for sentiment
  - `_write_to_csv(results, file_name, check_latency)` - Logs results to CSV

**Supporting Pydantic Models:**
- `Translation` - Structured output for translation
- `Sentiment` - Structured output for sentiment analysis

**Dependencies:**
- External API: OpenAI (for translation, sentiment, and TTS)
- Libraries: langchain, openai, pydantic, kss (Korean sentence segmentation)

---

## 4. CLASS RELATIONSHIPS AND DEPENDENCIES

### 4.1 Model Relationships
```
User (1) ----> (N) Session
  ↓
  └─── Session (1) ----> (N) Page
         ↓
         └─── Page (1) ----> (N) BB
```

### 4.2 Controller-Model Relationships
- **UserRegisterView, UserLoginView, UserChangeLangView, UserInfoView** ↔ **User Model**
- **StartSessionView, SelectVoiceView, EndSessionView, GetSessionInfoView, GetSessionStatsView, SessionReviewView, SessionReloadView, SessionReloadAllView, DiscardSessionView** ↔ **Session Model**
- **PageGetImageView, PageGetOCRView, PageGetTTSView** ↔ **Page Model**
- **ProcessUploadView, CheckOCRStatusView, CheckTTSStatusView, ProcessUploadCoverView** ↔ **Page, BB Models**

### 4.3 Controller-Service Relationships
- **ProcessUploadView** ↔ **OCRModule, TTSModule**
  - Uses OCRModule.process_page() for OCR
  - Uses TTSModule.get_translations_only() for translation
  - Uses TTSModule.run_tts_only() for TTS (background)

- **ProcessUploadCoverView** ↔ **OCRModule, TTSModule**
  - Uses OCRModule.process_cover_page() for title extraction
  - Uses TTSModule.translate_and_tts_cover() for translation and dual-voice TTS

### 4.4 Service Relationships
- **TTSModule** depends on:
  - OpenAI API (for translation, sentiment, TTS)
  - LangChain (for LLM chain orchestration)
  - kss (Korean sentence segmentation)

- **OCRModule** depends on:
  - External OCR API (Naver Clova OCR)
  - scikit-learn (DBSCAN clustering)
  - numpy (numerical operations)

---

## 5. ASYNC/THREADING PATTERNS

### 5.1 Async Operations in TTSModule
- **get_translations_only()**: Uses asyncio.gather() to process all sentences in parallel
- **run_tts_only()**: Uses asyncio.gather() to synthesize all sentences concurrently
- **translate()**, **sentiment()**: Async methods with retry logic
- **translate_and_tts_cover()**: Generates male and female voices concurrently

### 5.2 Threading in ProcessUploadView
- **_start_background_tts()**: Spawns a daemon thread to run TTS without blocking the response
- Uses asyncio.new_event_loop() and asyncio.close() for isolated event loops

---

## 6. API ENDPOINT SUMMARY

### User Endpoints
- POST /user/register
- POST /user/login
- PATCH /user/lang
- GET /user/info

### Session Endpoints
- POST /session/start
- POST /session/voice
- POST /session/end
- GET /session/info
- GET /session/stats
- GET /session/review
- GET /session/reload
- POST /session/discard

### Page Endpoints
- GET /page/get_image
- GET /page/get_ocr
- GET /page/get_tts

### Process Endpoints
- POST /process/upload
- GET /process/check_ocr
- GET /process/check_tts
- POST /process/upload_cover

---

## 7. KEY FEATURES AND PATTERNS

### 7.1 Image Processing Pipeline
1. Image upload (base64) → Save to disk
2. OCR processing → Extract text and bounding boxes
3. Translation → Translate text to target language
4. Sentiment analysis → Analyze emotional tone
5. TTS generation → Generate audio with sentiment-based instructions

### 7.2 Cover Image Processing
1. Cover image upload → OCR to extract title
2. Translate title
3. Generate TTS for both male and female voices simultaneously

### 7.3 Word Count Tracking
- Session tracks total words read
- Words are counted from OCR results and accumulated per session

### 7.4 Session Management
- Sessions can be started, ended, or discarded
- Pages are associated with sessions (One-to-Many)
- Sessions track metadata: title, voice preference, total pages, total words