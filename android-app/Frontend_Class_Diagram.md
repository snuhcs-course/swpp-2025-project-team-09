# StoryBridge Frontend (Android) Class Diagram Information

## Overview

This document contains a comprehensive list of all classes, methods, and relationships for the StoryBridge Android frontend codebase. This information is suitable for creating a detailed class diagram.

---

## **1. ACTIVITIES (Screen Controllers)**

### **1.1 LandingActivity**

- **Class: LandingActivity(AppCompatActivity)**
- **Properties:**
  - `viewModel: LandingViewModel` - Handles user authentication logic

- **Methods:**
  - `onCreate(savedInstanceState: Bundle?)` - Initializes UI, checks user authentication
  - `checkUser(deviceId: String)` - Validates if user exists or needs registration
  - `registerNewUser(deviceId: String, language: String)` - Registers new user with selected language
  - `showLanguageSelection()` - Displays language selection dialog (English/Chinese)
  - `navigateToMain()` - Navigates to MainActivity

- **Relationships:**
  - Uses: `LandingViewModel`
  - Uses: `UserApi` (via repository)
  - Uses: `AppSettings.setLanguage()`
  - Navigates to: MainActivity

---

### **1.2 MainActivity**

- **Class: MainActivity(AppCompatActivity)**
- **Properties:**
  - `viewModel: MainViewModel` - Manages user sessions and data
  - `settingsLauncher: ActivityResultLauncher<Intent>` - Handles settings screen result
  - `sessionCardContainer: FlexboxLayout` - Container for session cards

- **Methods:**
  - `onCreate(savedInstanceState: Bundle?)` - Sets up UI and navigation
  - `loadUserInfo()` - Fetches user's reading sessions from API
  - `observeUserInfo()` - Observes session list changes and displays SessionCards
  - `setupTopNavigationBar()` - Configures top navigation with settings button
  - `setupStartButton()` - Sets up "Start New Reading" button click listener
  - `navigateToStartSession()` - Launches StartSessionActivity
  - `navigateToLoadingSession(startedAt: String)` - Resumes existing session
  - `discardSession(sessionId: String)` - Deletes a session and reloads list

- **Relationships:**
  - Uses: `MainViewModel`
  - Uses: `UserRepository`, `SessionRepository`
  - Creates: Multiple `SessionCard` components dynamically
  - Uses: `TopNavigationBar` component
  - Navigates to: SettingActivity, StartSessionActivity, LoadingActivity

---

### **1.3 SettingActivity**

- **Class: SettingActivity(AppCompatActivity)**
- **Properties:**
  - `viewModel: SettingViewModel` - Manages settings state
  - `binding: ActivitySettingBinding` - View binding
  - `languageRadioGroup: RadioGroup` - Language selection (EN/ZH)
  - `voiceRadioGroup: RadioGroup` - Voice preference (Male/Female)

- **Methods:**
  - `onCreate(savedInstanceState: Bundle?)` - Initializes settings UI
  - `setupLanguageOptions()` - Sets up language radio buttons with current selection
  - `setupVoiceOptions()` - Sets up voice radio buttons with current selection
  - `setupSaveButton()` - Handles save button click, updates server and local settings
  - `observeLangResponse()` - Observes language update API response

- **Relationships:**
  - Uses: `SettingViewModel`
  - Uses: `UserRepository`
  - Calls: `UserApi.userLang()`, `AppSettings.setLanguage()`, `AppSettings.setVoice()`
  - Returns to: MainActivity (with RESULT_OK if settings changed)

---

### **1.4 StartSessionActivity**

- **Class: StartSessionActivity(AppCompatActivity)**
- **Properties:**
  - `viewModel: StartSessionViewModel` - Manages session creation
  - `binding: ActivityStartSessionBinding` - View binding

- **Methods:**
  - `onCreate(savedInstanceState: Bundle?)` - Sets up start session UI
  - `startNewSession()` - Calls API to create new session
  - `observeViewModel()` - Watches session creation state (Loading/Success/Error)
  - `navigateToCameraForCover(sessionId: String)` - Launches camera for book cover capture

- **Relationships:**
  - Uses: `StartSessionViewModel`
  - Uses: `SessionRepository`
  - Calls: `SessionApi.startSession()`
  - Navigates to: CameraSessionActivity (page_index=0, is_cover=true)

---

### **1.5 CameraActivity**

- **Class: CameraActivity(AppCompatActivity)**
- **Properties:**
  - `viewModel: CameraViewModel` - Manages ML Kit document scanner state
  - `scannerLauncher: ActivityResultLauncher<IntentSenderRequest>` - Scanner result handler
  - `permissionLauncher: ActivityResultLauncher<String>` - Camera permission handler

- **Methods:**
  - `onCreate(savedInstanceState: Bundle?)` - Initializes camera functionality
  - `initLaunchers()` - Sets up permission and scanner activity launchers
  - `checkPermissionAndStart()` - Checks camera permission before launching scanner
  - `observeUiState()` - Observes scanner state changes (Installing/Ready/Scanning/Error)
  - `startScan()` - Launches Google ML Kit document scanner

- **Returns:**
  - `RESULT_OK` with `image_path` extra on successful capture
  - `RESULT_CANCELED` if user cancels

- **Relationships:**
  - Uses: `CameraViewModel`
  - Uses: Google ML Kit `GmsDocumentScanner`
  - Returns image path to calling activity

---

### **1.6 CameraSessionActivity**

- **Class: CameraSessionActivity(AppCompatActivity)**
- **Properties:**
  - `viewModel: CameraSessionViewModel` - Manages camera session workflow
  - `sessionId: String` - Current session ID
  - `pageIndex: Int` - Page being captured
  - `isCover: Boolean` - Whether capturing book cover or content page
  - `cameraLauncher: ActivityResultLauncher<Intent>` - Launches CameraActivity

- **Intent Extras (Input):**
  - `session_id: String` - Session identifier
  - `page_index: Int` - Page number being captured
  - `is_cover: Boolean` - Cover page flag

- **Intent Extras (Output):**
  - `page_added: Boolean` - Indicates successful page addition

- **Methods:**
  - `onCreate(savedInstanceState: Bundle?)` - Initializes camera session flow
  - `startCamera()` - Launches CameraActivity for image capture
  - `handleCameraResult(resultCode: Int, data: Intent?)` - Processes camera result
  - `navigateToVoiceSelect(imagePath: String)` - Goes to voice selection (cover only)
  - `navigateToLoading(imagePath: String)` - Goes to loading screen (content pages)

- **Relationships:**
  - Uses: `CameraSessionViewModel`
  - Navigates to: CameraActivity → VoiceSelectActivity (if cover) OR LoadingActivity (if content)
  - Returns to: ContentInstructionActivity or ReadingActivity

---

### **1.7 VoiceSelectActivity**

- **Class: VoiceSelectActivity(AppCompatActivity)**
- **Properties:**
  - `viewModel: VoiceSelectViewModel` - Manages voice selection and cover upload
  - `sessionId: String` - Current session ID
  - `imagePath: String` - Path to captured cover image
  - `lang: String` - Language preference
  - `mediaPlayer: MediaPlayer?` - Plays voice sample audio
  - `binding: ActivityVoiceSelectBinding` - View binding

- **Constants:**
  - `MALE_VOICE = "onyx"` - Male voice identifier
  - `FEMALE_VOICE = "shimmer"` - Female voice identifier

- **Methods:**
  - `onCreate(savedInstanceState: Bundle?)` - Sets up voice selection UI
  - `setupVoiceButtons()` - Configures male/female voice buttons with preview
  - `playLocalAudio(audioResId: Int)` - Plays voice preview audio (R.raw.voice_man/voice_woman)
  - `selectVoice(voice: String)` - Saves selected voice to server and local settings
  - `goToContentInstruction()` - Navigates to instructions screen
  - `onBackPressed()` - Shows exit confirmation dialog

- **Relationships:**
  - Uses: `VoiceSelectViewModel`
  - Uses: `SessionRepository`, `ProcessRepository`
  - Calls: `SessionApi.selectVoice()`, `ProcessApi.uploadCoverImage()`
  - Calls: `AppSettings.setVoice()`
  - Background: Uploads cover image asynchronously
  - Navigates to: ContentInstructionActivity

---

### **1.8 ContentInstructionActivity**

- **Class: ContentInstructionActivity(AppCompatActivity)**
- **Properties:**
  - `viewModel: ContentInstructionViewModel` - Manages instruction state
  - `sessionId: String` - Current session ID
  - `binding: ActivityContentInstructionBinding` - View binding

- **Methods:**
  - `onCreate(savedInstanceState: Bundle?)` - Displays reading instructions
  - `goToCamera(sessionId: String)` - Launches camera for first content page (page_index=1)
  - `onBackPressed()` - Shows exit confirmation dialog

- **Relationships:**
  - Navigates to: CameraSessionActivity (page_index=1, is_cover=false)

---

### **1.9 LoadingActivity**

- **Class: LoadingActivity(AppCompatActivity)**
- **Properties:**
  - `viewModel: LoadingViewModel` - Manages image upload, OCR/TTS polling, and session reload
  - `binding: ActivityLoadingBinding` - View binding
  - `loadingProgressBar: ProgressBar` - Shows processing progress (0-100%)

- **Intent Extras (New Session Mode):**
  - `session_id: String` - Session identifier
  - `image_path: String` - Path to captured image
  - `page_index: Int` - Page number
  - `is_cover: Boolean` - Cover page flag
  - `lang: String` - Language code

- **Intent Extras (Resume Session Mode):**
  - `started_at: String` - Session start timestamp (ISO format)

- **Methods:**
  - `onCreate(savedInstanceState: Bundle?)` - Handles two modes: new session upload or resume session
  - **New Session Mode:**
    - `uploadImage()` - Uploads page image, triggers OCR/translation
    - `pollOcrStatus()` - Polls OCR processing status every 2 seconds
    - `handleOcrReady()` - Navigates to ReadingActivity when ready
  - **Resume Session Mode:**
    - `reloadSession()` - Fetches all session data from server
    - `handleReloadSuccess()` - Navigates to ReadingActivity with cached data
  - `navigateToReading(sessionId, pageIndex, totalPages)` - Goes to reading screen
  - `showError(message: String)` - Displays error and finishes activity
  - `onBackPressed()` - Disabled with toast message

- **Progress Mapping:**
  - 0-40%: Image upload with smooth ramping animation
  - 40-100%: OCR polling based on server progress percentage

- **Relationships:**
  - Uses: `LoadingViewModel`
  - Uses: `ProcessRepository`, `SessionRepository`, `UserRepository`, `PageRepository`
  - Calls: `ProcessApi.uploadImage()`, `uploadCoverImage()`, `checkOcrStatus()`, `checkTtsStatus()`
  - Calls: `SessionApi.reloadAllSession()`
  - Navigates to: ReadingActivity

---

### **1.10 ReadingActivity**

- **Class: ReadingActivity(AppCompatActivity)**
- **Properties:**
  - `viewModel: ReadingViewModel` - Manages page data and TTS polling
  - `sessionId: String` - Current session ID
  - `pageIndex: Int` - Current page number
  - `totalPages: Int` - Total pages in session
  - `isNewSession: Boolean` - Whether this is a new or resumed session
  - `binding: ActivityReadingBinding` - View binding
  - `pageImageView: ImageView` - Displays book page image
  - `topNavBar: TopNav` - Top navigation bar (menu, finish)
  - `bottomNavBar: BottomNav` - Bottom navigation bar (prev, next, capture)
  - `leftOverlay: LeftOverlay` - Thumbnail sidebar
  - `mediaPlayer: MediaPlayer?` - Plays TTS audio sequentially
  - `thumbnailRecyclerView: RecyclerView` - Page thumbnails
  - `thumbnailAdapter: ThumbnailAdapter` - Thumbnail list adapter

- **State Maps:**
  - `audioResultsMap: Map<Int, List<String>>` - Base64 audio chunks per bounding box
  - `playButtonsMap: Map<Int, ImageButton>` - Play buttons for each translation box
  - `boundingBoxViewsMap: Map<Int, TextView>` - Translation text overlays
  - `cachedBoundingBoxes: List<BoundingBox>` - OCR bounding box data
  - `savedBoxTranslations: Map<Int, Pair<Float, Float>>` - Saved translation box positions

- **Data Classes:**
  - `BoundingBox(x: Int, y: Int, width: Int, height: Int, text: String, index: Int)`

- **Constants:**
  - `TOUCH_SLOP = 10f` - Touch movement threshold for detecting drag vs click
  - `MIN_WIDTH = 500` - Minimum bounding box width in pixels

- **Methods:**
  - `onCreate(savedInstanceState: Bundle?)` - Initializes reading UI and fetches first page
  - `initViews()` - Sets up all UI components
  - `initUiState()` - Hides top/bottom navigation bars initially
  - `initListeners()` - Configures all click and touch listeners
  - `observeViewModel()` - Observes page data changes (image, OCR, TTS)
  - `fetchPage(sessionId, pageIndex)` - Loads image, OCR results, and TTS audio
  - `displayPage(data: GetImageResponse)` - Decodes and displays page image
  - `handleOcr(data: GetOcrTranslationResponse)` - Processes OCR results and renders bounding boxes
  - `handleTts(data: GetTtsResponse)` - Processes TTS audio and updates play buttons
  - `displayBoundingBoxes(bboxes: List<BoundingBox>)` - Renders translation text overlays
  - `setupBoundingBoxTouchListener(boxView: TextView, boxIndex: Int)` - Enables drag-to-move
  - `createPlayButton(bboxIndex: Int, rect: RectF)` - Creates audio play button
  - `playAudioForBox(bboxIndex: Int)` - Plays or pauses audio for a bounding box
  - `playNextAudio(audioList: List<String>, index: Int)` - Plays audio chunks sequentially
  - `toggleUI()` - Shows/hides top and bottom navigation bars
  - `toggleOverlay(show: Boolean)` - Shows/hides thumbnail sidebar
  - `loadPage(newPageIndex: Int)` - Switches to a different page
  - `updateBottomNavStatus()` - Updates page counter and prev/next button visibility
  - `fetchAllThumbnails()` - Loads all page thumbnails for sidebar
  - `navigateToFinish()` - Goes to FinishActivity
  - `navigateToCamera()` - Adds new page to session
  - `onBackPressed()` - Shows exit confirmation dialog

- **UI Behavior:**
  - **Screen Tap:** Toggles top/bottom navigation bars
  - **Menu Button:** Toggles thumbnail sidebar (slides in from left)
  - **Translation Boxes:** Draggable, positioned over original text
  - **Play Buttons:** Positioned at bottom-right of each translation box
  - **Prev/Next Buttons:** Conditional visibility based on page position
  - **Thumbnail Click:** Navigates to selected page

- **Relationships:**
  - Uses: `ReadingViewModel`
  - Uses: `PageRepository`
  - Calls: `PageApi.getImage()`, `getOcrResults()`, `getTtsResults()`
  - Uses: `ThumbnailAdapter` for page navigation
  - Uses: `TopNav`, `BottomNav`, `LeftOverlay` components
  - Background: Polls TTS status for updated audio (every 2 seconds)
  - Navigates to: FinishActivity, CameraSessionActivity

---

### **1.11 FinishActivity**

- **Class: FinishActivity(AppCompatActivity)**
- **Properties:**
  - `viewModel: FinishViewModel` - Fetches session statistics
  - `binding: ActivityFinishBinding` - View binding
  - `isNewSession: Boolean` - Whether session is new or resumed
  - `sessionId: String` - Current session ID

- **Methods:**
  - `onCreate(savedInstanceState: Bundle?)` - Displays session summary statistics
  - `observeSessionStats()` - Observes session statistics from API
  - `displayStats(stats: SessionStatsResponse)` - Shows total words, pages, time spent
  - `navigateNext()` - Goes to DecideSaveActivity (new) or MainActivity (resumed)

- **Displayed Statistics:**
  - Total words read
  - Total pages
  - Time spent (formatted as "X minutes Y seconds")

- **Relationships:**
  - Uses: `FinishViewModel`
  - Uses: `SessionRepository`
  - Calls: `SessionApi.endSession()`, `getSessionStats()`
  - Navigates to: DecideSaveActivity (new session) or MainActivity (resumed session)

---

### **1.12 DecideSaveActivity**

- **Class: DecideSaveActivity(AppCompatActivity)**
- **Properties:**
  - `binding: ActivityDecideSaveBinding` - View binding
  - `sessionId: String` - Session to save or discard
  - `repository: SessionRepositoryImpl` - Direct repository access
  - `decisionMade: Boolean` - Prevents double-tap

- **Methods:**
  - `onCreate(savedInstanceState: Bundle?)` - Shows save/discard options
  - `setupListeners()` - Configures save and discard button listeners
  - `setButtonSelected(selectedButton: View)` - Visual feedback for button selection
  - `handleSave()` - Keeps session (no API call needed, session already ended)
  - `handleDiscard()` - Calls API to delete session and all associated data
  - `showMainButton()` - Shows "Go to Main" button after decision
  - `navigateToMain()` - Returns to MainActivity

- **Relationships:**
  - Uses: `SessionRepository`
  - Calls: `SessionApi.discardSession()` (if user chooses discard)
  - Navigates to: MainActivity

---

## **2. UI COMPONENT CLASSES (Custom Views)**

### **2.1 SessionCard**

- **Class: SessionCard(ConstraintLayout)**
- **Properties:**
  - `bookImageView: ImageView` - Book cover image
  - `bookTitleTextView: TextView` - Book title text (original or translated)
  - `bookProgressTextView: TextView` - Session start date or progress
  - `trashButton: ImageView` - Delete button

- **Methods:**
  - `setBookImage(base64: String)` - Sets cover image from Base64
  - `setBookImageResource(resId: Int)` - Sets cover image from drawable resource
  - `setBookTitle(title: String)` - Sets title text (truncates if too long)
  - `setBookProgress(progress: String)` - Sets date/progress text
  - `setOnImageClickListener(listener: () -> Unit)` - Click to open session
  - `setOnTrashClickListener(listener: () -> Unit)` - Click to delete session

- **Usage:**
  - Dynamically created in MainActivity
  - Added to FlexboxLayout container
  - Each card represents one reading session

---

### **2.2 TopNavigationBar**

- **Class: TopNavigationBar(ConstraintLayout)**
- **Properties:**
  - `settingsButton: ImageView` - Settings gear icon

- **Methods:**
  - `setOnSettingsClickListener(listener: () -> Unit)` - Settings button click handler

- **Usage:**
  - Used in MainActivity
  - Always visible at top of screen

---

### **2.3 TopNav**

- **Class: TopNav(ConstraintLayout)**
- **Properties:**
  - `menuButton: ImageButton` - Hamburger/menu icon
  - `finishButton: Button` - Finish reading button

- **Methods:**
  - `setOnMenuButtonClickListener(listener: () -> Unit)` - Menu button handler
  - `setOnFinishButtonClickListener(listener: () -> Unit)` - Finish button handler
  - `show()` - Slides in from top
  - `hide()` - Slides out to top

- **Usage:**
  - Used in ReadingActivity
  - Toggles visibility on screen tap

---

### **2.4 BottomNav**

- **Class: BottomNav(ConstraintLayout)**
- **Properties:**
  - `prevButton: Button` - Previous page button
  - `nextButton: Button` - Next page button
  - `captureButton: ImageButton` - Add new page button (camera icon)
  - `statusTextView: TextView` - Page status display (e.g., "page 2/5")

- **Methods:**
  - `setOnPrevButtonClickListener(listener: () -> Unit)` - Previous page handler
  - `setOnCaptureButtonClickListener(listener: () -> Unit)` - Add page handler
  - `setOnNextButtonClickListener(listener: () -> Unit)` - Next page handler
  - `updatePageStatus(currentPage: Int, totalPages: Int)` - Updates display and button visibility
  - `show()` - Slides in from bottom
  - `hide()` - Slides out to bottom

- **Button Visibility Logic:**
  - **Prev:** Visible if `currentPage > 1`
  - **Next:** Visible if `currentPage < totalPages - 1`
  - **Capture:** Always visible

- **Usage:**
  - Used in ReadingActivity
  - Toggles visibility on screen tap

---

### **2.5 LeftOverlay**

- **Class: LeftOverlay(ConstraintLayout)**
- **Properties:**
  - `thumbnailRecyclerView: RecyclerView` - Page thumbnail list

- **Methods:**
  - `show()` - Slides in from left
  - `hide()` - Slides out to left

- **Usage:**
  - Used in ReadingActivity
  - Contains RecyclerView with ThumbnailAdapter
  - Toggled by menu button

---

### **2.6 ThumbnailAdapter**

- **Class: ThumbnailAdapter(RecyclerView.Adapter<ThumbnailAdapter.ViewHolder>)**

- **Data Class:**
  - `PageThumbnail(pageIndex: Int, imageBase64: String?)`

- **Properties:**
  - `thumbnails: MutableList<PageThumbnail>` - List of page thumbnails
  - `onThumbnailClick: (Int) -> Unit` - Click callback with page index

- **Methods:**
  - `submitList(newThumbnails: List<PageThumbnail>)` - Updates thumbnail list
  - `onCreateViewHolder(parent, viewType)` - Creates ViewHolder
  - `onBindViewHolder(holder, position)` - Binds data to ViewHolder
  - `getItemCount()` - Returns thumbnail count

- **ViewHolder:**
  - `thumbnailImageView: ImageView` - Page thumbnail image
  - `pageNumberTextView: TextView` - Page number label
  - `bind(thumbnail: PageThumbnail)` - Decodes Base64 image, sets click listener

- **Usage:**
  - Used in ReadingActivity's LeftOverlay
  - Displays vertical list of page thumbnails
  - Click thumbnail to navigate to that page

---

## **3. API INTERFACE CLASSES (Retrofit)**

### **3.1 UserApi**

- **Interface Methods:**
  - `@POST("/user/register") userRegister(@Body request: UserRegisterRequest): Response<UserRegisterResponse>`
  - `@POST("/user/login") userLogin(@Body request: UserLoginRequest): Response<UserLoginResponse>`
  - `@PATCH("/user/lang") userLang(@Body request: UserLangRequest): Response<UserLangResponse>`
  - `@GET("/user/info") userInfo(@Query("device_info") deviceInfo: String): Response<List<UserInfoResponse>>`

- **Request Data Classes:**

**UserRegisterRequest:**
- `device_info: String` - Android device ID
- `language_preference: String` - "en" or "zh"

**UserLoginRequest:**
- `device_info: String` - Android device ID

**UserLangRequest:**
- `device_info: String` - Android device ID
- `language_preference: String` - "en" or "zh"

- **Response Data Classes:**

**UserRegisterResponse:**
- `user_id: String` - UUID
- `language_preference: String`

**UserLoginResponse:**
- `user_id: String` - UUID
- `language_preference: String`

**UserLangResponse:**
- `user_id: String` - UUID
- `language_preference: String`
- `updated_at: String` - ISO datetime

**UserInfoResponse:**
- `user_id: String` - UUID
- `session_id: String` - UUID
- `title: String` - Book title (original)
- `translated_title: String` - Translated title
- `image_base64: String` - Cover image Base64
- `started_at: String` - ISO datetime

---

### **3.2 SessionApi**

- **Interface Methods:**
  - `@POST("/session/start") startSession(@Body request: StartSessionRequest): Response<StartSessionResponse>`
  - `@POST("/session/voice") selectVoice(@Body request: SelectVoiceRequest): Response<SelectVoiceResponse>`
  - `@POST("/session/end") endSession(@Body request: EndSessionRequest): Response<EndSessionResponse>`
  - `@GET("/session/stats") getSessionStats(@Query("session_id") sessionId: String): Response<SessionStatsResponse>`
  - `@GET("/session/info") getSessionInfo(@Query("session_id") sessionId: String): Call<SessionInfoResponse>`
  - `@GET("/session/review") getSessionReview(@Query("session_id") sessionId: String): Call<SessionReviewResponse>`
  - `@GET("/session/reload") reloadSession(@Query("user_id") userId: String, @Query("started_at") startedAt: String, @Query("page_index") pageIndex: Int): Response<ReloadSessionResponse>`
  - `@GET("/session/reload_all") reloadAllSession(@Query("user_id") userId: String, @Query("started_at") startedAt: String): Response<ReloadAllSessionResponse>`
  - `@POST("/session/discard") discardSession(@Body request: DiscardSessionRequest): Response<DiscardSessionResponse>`

- **Request Data Classes:**

**StartSessionRequest:**
- `user_id: String` - UUID

**SelectVoiceRequest:**
- `session_id: String` - UUID
- `voice_style: String` - "onyx" (male) or "shimmer" (female)

**EndSessionRequest:**
- `session_id: String` - UUID

**DiscardSessionRequest:**
- `session_id: String` - UUID

- **Response Data Classes:**

**StartSessionResponse:**
- `session_id: String` - UUID
- `started_at: String` - ISO datetime
- `page_index: Int` - Initial page (usually 0)

**SelectVoiceResponse:**
- `session_id: String` - UUID
- `voice_style: String`

**EndSessionResponse:**
- `session_id: String` - UUID
- `ended_at: String` - ISO datetime
- `total_pages: Int`

**SessionStatsResponse:**
- `session_id: String` - UUID
- `user_id: String` - UUID
- `isOngoing: Boolean`
- `started_at: String` - ISO datetime
- `ended_at: String` - ISO datetime
- `total_pages: Int`
- `total_time_spent: Int` - Seconds
- `total_words_read: Int`

**ReloadSessionResponse:**
- `session_id: String` - UUID
- `page_index: Int`
- `image_base64: String`
- `translation_text: String`
- `audio_url: String`

**ReloadAllSessionResponse:**
- `session_id: String` - UUID
- `started_at: String` - ISO datetime
- `pages: List<ReloadedPage>`

**ReloadedPage:**
- `page_index: Int`
- `img_url: String?`
- `translation_text: String?`
- `audio_url: String?`
- `ocr_results: List<OcrBox>?`

**DiscardSessionResponse:**
- `message: String`

---

### **3.3 ProcessApi**

- **Interface Methods:**
  - `@POST("/process/upload/") uploadImage(@Body request: UploadImageRequest): Response<UploadImageResponse>`
  - `@POST("/process/upload_cover/") uploadCoverImage(@Body request: UploadImageRequest): Response<UploadCoverResponse>`
  - `@GET("/process/check_ocr/") checkOcrStatus(@Query("session_id") sessionId: String, @Query("page_index") pageIndex: Int): Response<CheckOcrResponse>`
  - `@GET("/process/check_tts/") checkTtsStatus(@Query("session_id") sessionId: String, @Query("page_index") pageIndex: Int): Response<CheckTtsResponse>`

- **Request Data Classes:**

**UploadImageRequest:**
- `session_id: String` - UUID
- `page_index: Int`
- `lang: String` - "en" or "zh"
- `image_base64: String` - Base64 encoded JPEG

- **Response Data Classes:**

**UploadImageResponse:**
- `session_id: String` - UUID
- `page_index: Int`
- `status: String` - "pending", "processing", "ready"
- `submitted_at: String` - ISO datetime

**UploadCoverResponse:**
- `session_id: String` - UUID
- `page_index: Int`
- `status: String`
- `submitted_at: String` - ISO datetime
- `title: String` - Book title (original)
- `translated_title: String` - Translated title

**CheckOcrResponse:**
- `session_id: String` - UUID
- `page_index: Int`
- `status: String` - "pending", "processing", "ready"
- `progress: Int` - 0-100
- `submitted_at: String` - ISO datetime
- `processed_at: String?` - ISO datetime (nullable)

**CheckTtsResponse:**
- `session_id: String` - UUID
- `page_index: Int`
- `status: String` - "pending", "processing", "ready"
- `progress: Int` - 0-100
- `bb_status: List<BBoxStatus>?` - Per-bounding-box status

**BBoxStatus:**
- `bbox_index: Int`
- `status: String` - "pending", "processing", "ready", "failed"
- `has_audio: Boolean`

---

### **3.4 PageApi**

- **Interface Methods:**
  - `@GET("/page/get_image/") getImage(@Query("session_id") sessionId: String, @Query("page_index") pageIndex: Int): Response<GetImageResponse>`
  - `@GET("/page/get_ocr/") getOcrResults(@Query("session_id") sessionId: String, @Query("page_index") pageIndex: Int): Response<GetOcrTranslationResponse>`
  - `@GET("/page/get_tts/") getTtsResults(@Query("session_id") sessionId: String, @Query("page_index") pageIndex: Int): Response<GetTtsResponse>`

- **Response Data Classes:**

**GetImageResponse:**
- `session_id: String` - UUID
- `page_index: Int`
- `image_base64: String` - Base64 encoded JPEG
- `stored_at: String` - ISO datetime

**GetOcrTranslationResponse:**
- `session_id: String` - UUID
- `page_index: Int`
- `ocr_results: List<OcrBox>`
- `processed_at: String` - ISO datetime

**OcrBox:**
- `bbox: BBox` - Bounding box coordinates
- `original_txt: String` - Original text
- `translation_txt: String` - Translated text

**BBox:**
- `x1, y1, x2, y2, x3, y3, x4, y4: Int` - Four corner coordinates (clockwise from top-left)
- Computed properties:
  - `x: Int` - Leftmost x (min of x1, x2, x3, x4)
  - `y: Int` - Topmost y (min of y1, y2, y3, y4)
  - `width: Int` - max(x) - min(x)
  - `height: Int` - max(y) - min(y)

**GetTtsResponse:**
- `session_id: String` - UUID
- `page_index: Int`
- `audio_results: List<AudioResult>`
- `generated_at: String` - ISO datetime

**AudioResult:**
- `bbox_index: Int`
- `audio_base64_list: List<String>` - List of Base64 audio chunks (WAV format)

---

### **3.5 RetrofitClient**

- **Type:** Singleton object
- **Properties:**
  - `BASE_URL: String` - Default: `"http://ec2-43-203-114-138.ap-northeast-2.compute.amazonaws.com:8000"`
  - `userApi: UserApi` - Lazy singleton instance
  - `sessionApi: SessionApi` - Lazy singleton instance
  - `processApi: ProcessApi` - Lazy singleton instance
  - `pageApi: PageApi` - Lazy singleton instance

- **Methods:**
  - `overrideBaseUrl(url: String)` - Changes base URL and recreates all API instances

- **Configuration:**
  - **HTTP Logging:** Body-level logging with HttpLoggingInterceptor
  - **Timeouts:**
    - Connect timeout: 30 seconds
    - Read timeout: 120 seconds (for long OCR/TTS processing)
  - **Gson Converter:** Custom Gson with `FlexibleUserInfoAdapter` for handling array/object responses
  - **Base URL:** AWS EC2 backend server

- **Dependencies:**
  - Retrofit 2
  - Gson converter
  - OkHttp3 logging interceptor

---

## **4. DATA CLASSES AND SETTINGS**

### **4.1 AppSettings**

- **Type:** Singleton object
- **Storage:** SharedPreferences

- **Constants:**
  - `PREFS_NAME = "AppSettings"` - Preferences file name
  - `KEY_LANGUAGE = "language"` - Language key
  - `KEY_VOICE = "voice"` - Voice key
  - `KEY_RUN = "run"` - Previously used for tutorial flag (deprecated)

- **Methods:**
  - `setLanguage(context: Context, languageCode: String)` - Saves language preference ("en" or "zh")
  - `getLanguage(context: Context, default: String = "en"): String` - Retrieves language preference
  - `setVoice(context: Context, voiceType: String)` - Saves voice preference ("onyx" or "shimmer")
  - `getVoice(context: Context, default: String = "onyx"): String` - Retrieves voice preference
  - `setPref(context: Context, run: String)` - Deprecated method
  - `clearAll(context: Context)` - Clears all settings

- **Usage:**
  - Persists user preferences locally
  - Used across all activities for language and voice settings
  - Synchronized with server preferences

---

## **5. REPOSITORY LAYER**

### **5.1 UserRepository / UserRepositoryImpl**

- **Interface Methods:**
  - `suspend fun login(request: UserLoginRequest): Response<UserLoginResponse>`
  - `suspend fun register(request: UserRegisterRequest): Response<UserRegisterResponse>`
  - `suspend fun getUserInfo(deviceInfo: String): Response<List<UserInfoResponse>>`
  - `suspend fun userLang(request: UserLangRequest): Response<UserLangResponse>`

- **Implementation:**
  - Wraps `RetrofitClient.userApi` calls
  - Returns Retrofit `Response<T>` objects
  - All methods are suspend functions for coroutine support

---

### **5.2 SessionRepository / SessionRepositoryImpl**

- **Interface Methods:**
  - `suspend fun startSession(userId: String): Result<StartSessionResponse>`
  - `suspend fun selectVoice(sessionId: String, voiceStyle: String): Result<SelectVoiceResponse>`
  - `suspend fun endSession(sessionId: String): Result<EndSessionResponse>`
  - `suspend fun getSessionStats(sessionId: String): Result<SessionStatsResponse>`
  - `suspend fun reloadSession(userId: String, startedAt: String, pageIndex: Int): Result<ReloadSessionResponse>`
  - `suspend fun reloadAllSession(userId: String, startedAt: String): Result<ReloadAllSessionResponse>`
  - `suspend fun discardSession(sessionId: String): Result<DiscardSessionResponse>`

- **Implementation:**
  - Wraps `RetrofitClient.sessionApi` calls
  - Returns Kotlin `Result<T>` objects for better error handling
  - Converts Retrofit responses to Result.success or Result.failure

---

### **5.3 PageRepository / PageRepositoryImpl**

- **Interface Methods:**
  - `suspend fun getImage(sessionId: String, pageIndex: Int): Result<GetImageResponse>`
  - `suspend fun getOcrResults(sessionId: String, pageIndex: Int): Result<GetOcrTranslationResponse>`
  - `suspend fun getTtsResults(sessionId: String, pageIndex: Int): Result<GetTtsResponse>`

- **Implementation:**
  - Wraps `RetrofitClient.pageApi` calls
  - Returns Kotlin `Result<T>` objects
  - Used by ReadingViewModel for fetching page data

---

### **5.4 ProcessRepository / ProcessRepositoryImpl**

- **Interface Methods:**
  - `suspend fun uploadImage(request: UploadImageRequest): Result<UploadImageResponse>`
  - `suspend fun uploadCoverImage(request: UploadImageRequest): Result<UploadCoverResponse>`
  - `suspend fun checkOcrStatus(sessionId: String, pageIndex: Int): Result<CheckOcrResponse>`
  - `suspend fun checkTtsStatus(sessionId: String, pageIndex: Int): Result<CheckTtsResponse>`

- **Implementation:**
  - Wraps `RetrofitClient.processApi` calls
  - Returns Kotlin `Result<T>` objects
  - Used by LoadingViewModel and VoiceSelectViewModel

---

## **6. VIEWMODEL CLASSES**

### **6.1 LandingViewModel**

- **Inheritance:** `ViewModel`

- **UI State:**
```kotlin
sealed class LandingUiState {
    object Loading
    object NavigateMain
    object ShowLanguageSelect
    data class Error(val message: String)
}
```

- **Properties:**
  - `private val _uiState = MutableStateFlow<LandingUiState>(Loading)`
  - `val uiState: StateFlow<LandingUiState> = _uiState.asStateFlow()`

- **Methods:**
  - `fun checkUser(deviceId: String)` - Checks if user exists via login API
    - Success → `NavigateMain`
    - 404 Error → `ShowLanguageSelect`
    - Other Error → `Error(message)`

- **Dependencies:**
  - `UserRepository`

---

### **6.2 MainViewModel**

- **Inheritance:** `ViewModel`

- **Properties:**
  - `private val _userInfo = MutableStateFlow<Response<List<UserInfoResponse>>?>(null)`
  - `val userInfo: StateFlow<Response<List<UserInfoResponse>>?> = _userInfo.asStateFlow()`
  - `private val _discardResult = MutableStateFlow<Result<DiscardSessionResponse>?>(null)`
  - `val discardResult: StateFlow<Result<DiscardSessionResponse>?> = _discardResult.asStateFlow()`

- **Methods:**
  - `fun loadUserInfo(deviceInfo: String)` - Fetches user's session list from API
  - `fun discardSession(sessionId: String, deviceInfo: String)` - Deletes session and reloads list

- **Dependencies:**
  - `UserRepository` - For fetching user session list
  - `SessionRepository` - For discarding sessions

---

### **6.3 SettingViewModel**

- **Inheritance:** `ViewModel`

- **Properties:**
  - `private val _langResponse = MutableStateFlow<Response<UserLangResponse>?>(null)`
  - `val langResponse: StateFlow<Response<UserLangResponse>?> = _langResponse.asStateFlow()`

- **Methods:**
  - `fun updateLanguage(request: UserLangRequest)` - Updates language preference on server

- **Dependencies:**
  - `UserRepository`

---

### **6.4 StartSessionViewModel**

- **Inheritance:** `ViewModel`

- **UI State:**
```kotlin
sealed class StartSessionUiState {
    object Idle
    object Loading
    data class Success(val sessionId: String)
    data class Error(val message: String)
}
```

- **Properties:**
  - `private val _state = MutableStateFlow<StartSessionUiState>(Idle)`
  - `val state: StateFlow<StartSessionUiState> = _state.asStateFlow()`

- **Methods:**
  - `fun startSession(deviceId: String)` - Creates new reading session via API

- **Dependencies:**
  - `SessionRepository`

---

### **6.5 VoiceSelectViewModel**

- **Inheritance:** `ViewModel`

- **Properties:**
  - `private val _loading = MutableStateFlow(false)`
  - `val loading: StateFlow<Boolean> = _loading.asStateFlow()`
  - `private val _error = MutableSharedFlow<String>()`
  - `val error: SharedFlow<String> = _error.asSharedFlow()`
  - `private val _success = MutableSharedFlow<Unit>()`
  - `val success: SharedFlow<Unit> = _success.asSharedFlow()`

- **Methods:**
  - `fun selectVoice(sessionId: String, voice: String)` - Saves voice preference to server
  - `fun uploadCoverInBackground(sessionId: String, lang: String, imagePath: String)` - Uploads cover image asynchronously

- **Dependencies:**
  - `SessionRepository` - For voice selection API
  - `ProcessRepositoryImpl` - For cover image upload (direct instantiation)

---

### **6.6 LoadingViewModel**

- **Inheritance:** `ViewModel`

- **Properties:**
  - `private val _progress = MutableStateFlow(0)` - Progress percentage (0-100)
  - `val progress: StateFlow<Int> = _progress.asStateFlow()`
  - `private val _status = MutableStateFlow("idle")` - "idle", "uploading", "polling", "ready", "reloading"
  - `val status: StateFlow<String> = _status.asStateFlow()`
  - `private val _error = MutableStateFlow<String?>(null)`
  - `val error: StateFlow<String?> = _error.asStateFlow()`
  - `private val _cover = MutableStateFlow<CoverResult?>(null)`
  - `val cover: StateFlow<CoverResult?> = _cover.asStateFlow()`
  - `private val _navigateToVoice = MutableStateFlow<CoverResult?>(null)`
  - `val navigateToVoice: StateFlow<CoverResult?> = _navigateToVoice.asStateFlow()`
  - `private val _navigateToReading = MutableStateFlow<SessionResumeResult?>(null)`
  - `val navigateToReading: StateFlow<SessionResumeResult?> = _navigateToReading.asStateFlow()`
  - `private val _userInfo = MutableStateFlow<Response<List<UserInfoResponse>>?>(null)`
  - `val userInfo: StateFlow<Response<List<UserInfoResponse>>?> = _userInfo.asStateFlow()`
  - `private var rampJob: Job? = null` - Background job for smooth progress animation

- **Data Classes:**
  - `CoverResult(title: String, maleTts: String, femaleTts: String)`
  - `SessionResumeResult(session_id: String, page_index: Int, total_pages: Int)`

- **Methods:**
  - `fun uploadImage(sessionId: String, pageIndex: Int, lang: String, path: String)` - Uploads page image and starts polling
  - `fun uploadCover(sessionId: String, lang: String, path: String)` - Uploads cover image
  - `fun pollOcr(sessionId: String, pageIndex: Int)` - Polls OCR status until ready (every 2 seconds)
  - `fun startRampTo(targetProgress: Int, durationMs: Long)` - Smooth progress animation
  - `fun stopRamp()` - Stops progress animation
  - `fun encodeBase64(imagePath: String): String?` - Converts image file to Base64
  - `fun scaleBitmap(bitmap: Bitmap): Bitmap` - Scales large images to max 1920px
  - `fun loadUserInfo(deviceInfo: String)` - Fetches user sessions
  - `fun reloadAllSession(startedAt: String, context: Context)` - Resumes existing session

- **Progress Mapping:**
  - 0-40%: Image upload with smooth ramping animation
  - 40-100%: OCR polling based on server progress percentage

- **Dependencies:**
  - `ProcessRepository` - For upload and status polling
  - `PageRepository` - For fetching page data
  - `UserRepository` - For user info
  - `SessionRepository` - For session reload

---

### **6.7 ReadingViewModel**

- **Inheritance:** `ViewModel`

- **UI State:**
```kotlin
data class ReadingUiState(
    val image: GetImageResponse? = null,
    val ocr: GetOcrTranslationResponse? = null,
    val tts: GetTtsResponse? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
```

- **Properties:**
  - `private val _uiState = MutableStateFlow(ReadingUiState())`
  - `val uiState: StateFlow<ReadingUiState> = _uiState.asStateFlow()`
  - `private val _thumbnailList = MutableStateFlow<List<PageThumbnail>>(emptyList())`
  - `val thumbnailList: StateFlow<List<PageThumbnail>> = _thumbnailList.asStateFlow()`
  - `private var pollingJob: Job? = null` - Background TTS polling job
  - `private val pollInterval = 2000L` - Poll every 2 seconds

- **Methods:**
  - `fun fetchPage(sessionId: String, pageIndex: Int)` - Fetches image, OCR, and TTS for a page
  - `fun fetchImage(sessionId: String, pageIndex: Int)` - Gets page image only
  - `fun fetchOcr(sessionId: String, pageIndex: Int)` - Gets OCR results only
  - `fun fetchTts(sessionId: String, pageIndex: Int)` - Gets TTS audio only
  - `fun startTtsPolling(sessionId: String, pageIndex: Int)` - Polls TTS status every 2 seconds (max 60 attempts = 2 minutes)
  - `fun fetchThumbnail(sessionId: String, pageIndex: Int)` - Fetches single page thumbnail
  - `override fun onCleared()` - Cancels polling job when ViewModel is destroyed

- **Polling Behavior:**
  - Polls TTS API every 2 seconds for up to 2 minutes
  - Updates UI with new audio results as they become available
  - Continues polling even after some audio is ready (to get all bounding boxes)
  - Stops polling when all bounding boxes have audio or max attempts reached

- **Dependencies:**
  - `PageRepository` - For fetching page data and thumbnails

---

### **6.8 CameraViewModel**

- **Inheritance:** `AndroidViewModel(application: Application)`

- **UI State:**
```kotlin
data class CameraUiState(
    val isInstalling: Boolean = false,
    val isReady: Boolean = false,
    val isScanning: Boolean = false,
    val imagePath: String? = null,
    val error: String? = null
)
```

- **Properties:**
  - `private val _uiState = MutableStateFlow(CameraUiState())`
  - `val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()`
  - `private var scanner: GmsDocumentScanner? = null` - ML Kit scanner instance

- **Methods:**
  - `fun checkGooglePlayServices(): Boolean` - Verifies Google Play Services availability
  - `fun checkModuleAndInitScanner()` - Checks if ML Kit document scanner module is installed
  - `private fun installModule(optionalModuleApi: OptionalModuleApi)` - Installs ML Kit module if missing
  - `private fun initScanner()` - Initializes scanner with configuration
  - `fun prepareScannerIntent(activity: Activity, onReady: (IntentSender) -> Unit, onError: (String) -> Unit)` - Gets scanner intent
  - `fun handleScanningResult(scanningResult: GmsDocumentScanningResult, contentResolver: ContentResolver)` - Processes scanned image
  - `private fun saveImage(uri: Uri, contentResolver: ContentResolver): String` - Saves image to app files directory
  - `fun consumeReadyFlag()` - Resets ready state after consumption

- **Scanner Configuration:**
  - **Gallery Import:** Disabled
  - **Page Limit:** 1 page per scan
  - **Scanner Mode:** BASE (standard quality)
  - **Result Format:** JPEG

- **Dependencies:**
  - Google ML Kit Document Scanner (`com.google.android.gms.mlkit.documentscanner`)
  - Google Play Services

---

### **6.9 CameraSessionViewModel**

- **Inheritance:** `ViewModel`

- **UI State:**
```kotlin
sealed class SessionUiState {
    object Idle
    data class Success(val imagePath: String)
    object Cancelled
    data class Error(val message: String)
}
```

- **Properties:**
  - `private val _state = MutableStateFlow<SessionUiState>(SessionUiState.Idle)`
  - `val state: StateFlow<SessionUiState> = _state.asStateFlow()`

- **Methods:**
  - `fun handleCameraResult(resultCode: Int, imagePath: String?)` - Processes camera activity result

---

### **6.10 FinishViewModel**

- **Inheritance:** `ViewModel`

- **Properties:**
  - `private val _sessionStats = MutableLiveData<SessionStatsResponse>()`
  - `val sessionStats: LiveData<SessionStatsResponse> = _sessionStats`
  - `private val _showMainButton = MutableLiveData(false)`
  - `val showMainButton: LiveData<Boolean> = _showMainButton`

- **Methods:**
  - `fun endSession(sessionId: String)` - Calls end session API and fetches statistics

- **Dependencies:**
  - `SessionRepository`

---

## **7. CLASS RELATIONSHIPS AND DEPENDENCIES**

### **7.1 Activity-ViewModel Relationships**

- **LandingActivity** ↔ **LandingViewModel** → **UserRepository**
- **MainActivity** ↔ **MainViewModel** → **UserRepository**, **SessionRepository**
- **SettingActivity** ↔ **SettingViewModel** → **UserRepository**
- **StartSessionActivity** ↔ **StartSessionViewModel** → **SessionRepository**
- **CameraActivity** ↔ **CameraViewModel** → **Google ML Kit**
- **CameraSessionActivity** ↔ **CameraSessionViewModel**
- **VoiceSelectActivity** ↔ **VoiceSelectViewModel** → **SessionRepository**, **ProcessRepository**
- **LoadingActivity** ↔ **LoadingViewModel** → **ProcessRepository**, **SessionRepository**, **UserRepository**, **PageRepository**
- **ReadingActivity** ↔ **ReadingViewModel** → **PageRepository**
- **FinishActivity** ↔ **FinishViewModel** → **SessionRepository**

### **7.2 Repository-API Relationships**

- **UserRepository** → **RetrofitClient.userApi** → **UserApi**
- **SessionRepository** → **RetrofitClient.sessionApi** → **SessionApi**
- **ProcessRepository** → **RetrofitClient.processApi** → **ProcessApi**
- **PageRepository** → **RetrofitClient.pageApi** → **PageApi**

### **7.3 Component Usage Relationships**

- **MainActivity** uses:
  - `TopNavigationBar` (1 instance)
  - `SessionCard` (N instances, dynamic)

- **ReadingActivity** uses:
  - `TopNav` (1 instance)
  - `BottomNav` (1 instance)
  - `LeftOverlay` (1 instance)
  - `ThumbnailAdapter` (1 instance in RecyclerView)

### **7.4 Navigation Relationships**

```
LandingActivity
├─ (New User) → Language Selection → MainActivity
└─ (Existing User) → MainActivity

MainActivity
├─ Settings → SettingActivity → MainActivity (recreate)
├─ Session Card → LoadingActivity (resume) → ReadingActivity
└─ Start New → StartSessionActivity

StartSessionActivity
└─ CameraSessionActivity (cover=true, page=0)

CameraSessionActivity
└─ CameraActivity → Returns image_path

[Cover Capture Path]
CameraSessionActivity (cover=true)
└─ VoiceSelectActivity
   └─ ContentInstructionActivity
      └─ CameraSessionActivity (page=1, cover=false)

[Content Page Capture Path]
CameraSessionActivity (cover=false)
└─ LoadingActivity (upload + poll)
   └─ ReadingActivity

ReadingActivity
├─ Add Page → CameraSessionActivity → LoadingActivity → ReadingActivity
├─ Prev/Next → Internal page navigation
├─ Thumbnail → Internal page navigation
└─ Finish → FinishActivity

FinishActivity
├─ (New) → DecideSaveActivity → MainActivity
└─ (Resumed) → MainActivity

DecideSaveActivity
├─ Save → MainActivity
└─ Discard → API call → MainActivity
```

---

## **8. KEY FEATURES AND PATTERNS**

### **8.1 Architecture Patterns**

- **MVVM (Model-View-ViewModel):**
  - Activities = View layer
  - ViewModels = Presentation logic
  - Repositories = Data layer
  - Clear separation of concerns

- **Repository Pattern:**
  - Abstracts data sources (API, local storage)
  - Provides clean interface to ViewModels
  - Converts API responses to Result or Response objects

- **Dependency Injection:**
  - Manual DI via ViewModelFactory classes
  - Repository instances passed to ViewModels
  - Singleton RetrofitClient for API instances

- **Single Activity Per Screen:**
  - Each screen is a separate Activity (not single-activity architecture)
  - Intent-based navigation with extras
  - Result launchers for back-communication

### **8.2 Data Flow Patterns**

1. **Authentication Flow:**
   - `LandingActivity` → `UserApi.login/register` → `AppSettings.setLanguage()` → `MainActivity`

2. **New Session Flow:**
   - `StartSessionActivity` → `SessionApi.startSession()` → `CameraActivity` → `VoiceSelectActivity` → `SessionApi.selectVoice()` + `ProcessApi.uploadCoverImage()` → `ContentInstructionActivity` → `CameraActivity` → `LoadingActivity` → `ProcessApi.uploadImage()` → Poll `checkOcrStatus()` → `ReadingActivity`

3. **Resume Session Flow:**
   - `MainActivity` → `LoadingActivity` → `SessionApi.reloadAllSession()` → `ReadingActivity`

4. **Reading Page Flow:**
   - `ReadingActivity` → `PageApi.getImage()` + `getOcrResults()` + `getTtsResults()` → Display page with translations and audio → Poll `getTtsResults()` for updates

5. **End Session Flow:**
   - `ReadingActivity` → `FinishActivity` → `SessionApi.endSession()` + `getSessionStats()` → `DecideSaveActivity` → `SessionApi.discardSession()` (if discard) → `MainActivity`

### **8.3 User Experience Features**

- **Progress Indication:**
  - Smooth progress bar with ramping animation in LoadingActivity
  - Real-time OCR/TTS status updates
  - Progress percentage from server

- **Error Handling:**
  - Toast messages for user feedback
  - Error states in ViewModels
  - Retry logic for network failures

- **Back Button Handling:**
  - Custom confirmation dialogs in key activities
  - Prevents accidental session loss
  - Disabled in LoadingActivity during processing

- **Draggable UI:**
  - Touch listeners for moving translation boxes
  - Saved positions per page
  - Smooth drag experience with TOUCH_SLOP threshold

- **Audio Playback:**
  - Sequential audio chunk playback with MediaPlayer
  - Play/pause toggle per bounding box
  - Visual feedback with play button state

- **Thumbnail Navigation:**
  - Side panel with page previews
  - Click to jump to any page
  - Smooth slide-in/out animation

### **8.4 API Integration Features**

- **Retrofit Configuration:**
  - Type-safe HTTP client
  - Gson converter with custom adapters
  - HTTP logging for debugging

- **Coroutines:**
  - Suspend functions for async operations
  - StateFlow and SharedFlow for reactive UI
  - Background jobs for polling

- **Error Handling:**
  - Kotlin `Result<T>` wrapper for consistent error handling
  - Retrofit `Response<T>` for raw API responses
  - Try-catch blocks in repository layer

- **Timeouts:**
  - 30 seconds connect timeout
  - 120 seconds read timeout (for long OCR/TTS processing)

- **Polling:**
  - 2-second intervals for OCR/TTS status
  - Maximum attempts to prevent infinite loops
  - Automatic cancellation when ViewModel is destroyed

### **8.5 Settings and Persistence**

- **SharedPreferences:**
  - Language preference ("en" or "zh")
  - Voice preference ("onyx" or "shimmer")
  - Synchronized with server

- **Device ID:**
  - `Settings.Secure.ANDROID_ID` for user identification
  - No traditional login/password required

- **Local Files:**
  - Image storage in app files directory
  - Temporary files cleaned up after upload
  - Base64 encoding for API transmission

### **8.6 ML Kit Integration**

- **Google ML Kit Document Scanner:**
  - Automatic document detection
  - Perspective correction
  - Edge detection
  - JPEG output format

- **Module Installation:**
  - Dynamic feature module
  - On-demand installation if not present
  - Progress tracking during installation

---

## **9. API ENDPOINT SUMMARY**

### **User Endpoints**

- **POST** `/user/register` - Register new user
- **POST** `/user/login` - Login existing user
- **PATCH** `/user/lang` - Update language preference
- **GET** `/user/info` - Get user's session list

### **Session Endpoints**

- **POST** `/session/start` - Create new reading session
- **POST** `/session/voice` - Select TTS voice
- **POST** `/session/end` - End reading session
- **GET** `/session/stats` - Get session statistics
- **GET** `/session/info` - Get session info (unused)
- **GET** `/session/review` - Get session review (unused)
- **GET** `/session/reload` - Reload specific page (unused)
- **GET** `/session/reload_all` - Reload entire session
- **POST** `/session/discard` - Delete session and all data

### **Process Endpoints**

- **POST** `/process/upload/` - Upload page image
- **POST** `/process/upload_cover/` - Upload cover image
- **GET** `/process/check_ocr/` - Check OCR processing status
- **GET** `/process/check_tts/` - Check TTS processing status

### **Page Endpoints**

- **GET** `/page/get_image/` - Get page image
- **GET** `/page/get_ocr/` - Get OCR results with translations
- **GET** `/page/get_tts/` - Get TTS audio results

---

## **10. SUMMARY TABLE**

| Component | Type | Key Responsibility |
|-----------|------|-------------------|
| LandingActivity | Activity | User authentication, first launch |
| MainActivity | Activity | Session list, main hub |
| SettingActivity | Activity | Language/voice preferences |
| StartSessionActivity | Activity | Create new reading session |
| CameraActivity | Activity | ML Kit document scanning |
| CameraSessionActivity | Activity | Camera flow coordinator |
| VoiceSelectActivity | Activity | TTS voice selection |
| ContentInstructionActivity | Activity | Reading instructions |
| LoadingActivity | Activity | Image upload + OCR/TTS processing |
| ReadingActivity | Activity | Main reading interface with translations and audio |
| FinishActivity | Activity | Session summary and statistics |
| DecideSaveActivity | Activity | Save or discard session decision |
| SessionCard | Custom View | Session list item display |
| TopNavigationBar | Custom View | Settings button (MainActivity) |
| TopNav | Custom View | Menu and finish buttons (ReadingActivity) |
| BottomNav | Custom View | Page navigation and capture button |
| LeftOverlay | Custom View | Thumbnail sidebar container |
| ThumbnailAdapter | RecyclerView Adapter | Page thumbnail list |
| UserApi | Retrofit Interface | User authentication and info |
| SessionApi | Retrofit Interface | Session lifecycle management |
| ProcessApi | Retrofit Interface | Image upload and status polling |
| PageApi | Retrofit Interface | Page data retrieval |
| RetrofitClient | Singleton | API instance management and configuration |
| UserRepository | Repository | User data abstraction layer |
| SessionRepository | Repository | Session data abstraction layer |
| PageRepository | Repository | Page data abstraction layer |
| ProcessRepository | Repository | Processing data abstraction layer |
| AppSettings | Singleton | Local preferences storage (SharedPreferences) |
| LandingViewModel | ViewModel | User authentication state management |
| MainViewModel | ViewModel | Session list and discard logic |
| SettingViewModel | ViewModel | Settings update logic |
| StartSessionViewModel | ViewModel | Session creation state |
| VoiceSelectViewModel | ViewModel | Voice selection and cover upload |
| LoadingViewModel | ViewModel | Upload, polling, and session reload |
| ReadingViewModel | ViewModel | Page data and TTS polling |
| CameraViewModel | ViewModel | ML Kit scanner management |
| CameraSessionViewModel | ViewModel | Camera result handling |
| FinishViewModel | ViewModel | Session statistics fetching |
