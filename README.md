# StoryBridge - SWPP 2025 Project Team 09

For multicultural children to thrive, early exposure to all languages, especially Korean during the crucial kindergarten stage, is essential; however, many currently face significant educational challenges due to a lack of tailored Korean language materials and effective learning methods.

**StoryBridge** directly addresses this need by transforming any physical picture book into a **multilingual, interactive resource**, ensuring that reading is inclusive and accessible by delivering translations in the child's home language alongside the original Korean. Going beyond simple text overlays, the application utilizes advanced **Emotional Text-to-Speech (TTS)** to provide a deeply engaging and expressive narration, helping children form a strong, emotional, and cognitive connection with the story in both their native tongue and Korean, making story time a dynamic and comprehensive learning experience.

<center>
<img width="512" height="512" alt="image" src="https://github.com/user-attachments/assets/9524d450-c5fc-4637-868e-78ce0c2919d9" />
</center>

## Demo Video

You can watch the demo video here: [Google Drive Link](https://drive.google.com/file/d/11pnpqpl5iuQ6Ef6Lpv0T-cEeTbaDFiJz/view?usp=drive_link)

## Features

### Core Features
- **Live AI-Powered Translation**: The app uses Naver OCR for Text Recognition to instantly identify the words on the page. The recognized text is then translated into the user's preferred language using gpt-4o-mini. The translated text can be overlaid on the book in an augmented reality view.

- **Emotion-Based Text-to-Speech (TTS)**: This is a crucial AI feature designed to bring stories to life. Instead of a monotone narration, the app will analyze the translated text to infer its emotional context (e.g., happy, sad, excited) and use an expressive TTS engine to read the story aloud with appropriate feeling and intonation. This creates a more natural and engaging storytelling experience for the child.

- **Reading History Management**: The app maintains a database of all previously read books, allowing children to easily track their reading journey. Users can browse their reading history and resume sessions at any time.

- **Reading Progress & Encouragement**: After completing a book, the app presents a congratulations screen showing reading statistics (pages read, word count, time spent) to encourage and motivate young learners, supporting their educational journey through positive reinforcement.

### Implemented Features

#### Backend
- **Optimized Latency**: The server sends partial translation results as soon as they are ready while waiting for TTS (text-to-speech) results to complete, reducing user-perceived latency.
- **Automated Deployment**: Backend is automatically deployed to the EC2 server through GitHub Actions.
- **RESTful API**: Django REST Framework-based API for seamless frontend-backend communication.

#### Frontend
- **Audio Playback**: Added audio playback button for TTS narration.
- **Cover Page & Voice Selection**: Users can select their preferred voice and language before starting the session.
- **Text Box Movement**: Interactive text boxes that can be repositioned on the camera view.
- **Page Navigation**: Easy navigation between book pages during reading sessions.
- **Session Management**: Save and delete session functionality for managing reading progress.
- **Reading Statistics**: Congratulations screen displaying pages read, word count, and reading time to encourage learners.
- **MVVM Architecture**: Refactored frontend using the MVVM (Model-View-ViewModel) architecture pattern for better code organization and testability.

## How to Run the Demo

### Prerequisites
- Android Studio Narwhal Feature Drop | 2025.1.2 Patch 2 or later
- Android device with camera support (or emulator)
- Internet connection (app connects to deployed backend)

### Installation and Run Instructions

The backend is already deployed on an AWS EC2 server. You only need to set up and run the Android app.

**Step 1: Clone the Repository**
```bash
git clone https://github.com/snuhcs-course/swpp-2025-project-team-09.git
cd swpp-2025-project-team-09
```

**Step 2: Open in Android Studio**
1. Open Android Studio
2. Select `File` → `Open`
3. Navigate to and select the `android-app` directory
4. Wait for Gradle sync to complete

**Step 3: Run the App**
1. Connect an Android device (USB debugging enabled) or start an emulator
2. Verify device is connected: `adb devices`
3. Click the Run (▶️) button in Android Studio
4. The app will install and launch automatically

### Backend Server Information

The app is pre-configured to connect to the deployed EC2 server:

```
Backend URL: http://ec2-3-36-206-206.ap-northeast-2.compute.amazonaws.com:8000
```

No additional backend setup is required. The server is already running and accessible.

### Optional: Local Backend Setup

If you want to run the backend locally for development:

**Step 1: Set Up Virtual Environment**
```bash
# Navigate to backend directory
cd backend

# Create and activate virtual environment
python -m venv .venv
source .venv/bin/activate  # On Windows: .venv\Scripts\activate

# Install dependencies
pip install -r requirements.txt
```

**Step 2: Configure Environment Variables**
Create a `.env` file in the backend directory:
```
OPENAI_API_KEY=your_openai_api_key
OCR_API_URL=your_naver_ocr_api_url
OCR_SECRET=your_naver_ocr_secret_key
```

**Step 3: Run Backend Server**
```bash
# Run migrations
python manage.py migrate

# Start development server
python manage.py runserver 0.0.0.0:8000
```

## Testing

### Backend Testing Setup

Follow these steps to set up a virtual environment and run backend tests:

**Step 1: Create and Activate Virtual Environment**
```bash
# Move to backend/
cd ./backend

# Create a virtual environment
python -m venv venv

# Activate the virtual environment
source venv/bin/activate  # On Windows: venv\Scripts\activate
```

**Step 2: Install Dependencies**
After activating the virtual environment, install all dependencies:
```bash
pip install -r requirements.txt
```

**Step 3: Run Backend Tests**

**Option A: Interactive CLI Interface**
```bash

# Navigate to backend directory
cd backend

# Run interactive test menu
python run_tests.py
```

**Option B: Coverage Check with pytest**
```bash
# Check test coverage for the apis module
cd backend
pytest --cov=apis
```

### Test Coverage
The test suite covers:
- API endpoints and request/response validation
- Translation service integration
- OCR processing
- Session management (including save/delete functionality)
- Database operations
- Reading history tracking

**Step 4: Run Frontend Tests**

Run 
```bash
./gradlew clean jacocoTestReport 
```
with a real device connected.

After execution, the Jacoco coverage report can be viewed at: 
app/build/reports/jacoco/jacocoTestReport/html/index.html

This test covers both unit test and instrumental(UI) test of M, V, VM

## Technology Stack

### Backend
- Python 3.11 with Django 5.2.7
- Django REST Framework for API
- SQLite for development database (PostgreSQL supported for production)
- OpenAI GPT-4o-mini for translation
- Naver OCR API for text recognition
- LangChain for AI orchestration

### Frontend
- Kotlin 2.0 for Android development
- Android SDK (API level 34, min 26)
- ML Kit Document Scanner for camera features
- Retrofit for API communication
- Camera2 API for advanced camera control

## Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                      Frontend (Android)                      │
│                                                              │
│  ┌──────────────┐    ┌──────────────┐   ┌─────────────────┐  │
│  │   Camera     │───►│   ML Kit     │──►│  Image Capture  │  │
│  │   Activity   │    │   Scanner    │   │                 │  │
│  └──────────────┘    └──────────────┘   └─────────────────┘  │
│                                                   │          │
└───────────────────────────────────────────────────┼──────────┘
                                                    │
                                         Image Upload
                                                    │
                                                    ▼
┌─────────────────────────────────────────────────────────────┐
│                    Backend (Django REST API)                │
│                                                             │
│  ┌──────────────┐    ┌──────────────┐    ┌────────────────┐ │
│  │   Process    │───►│     OCR      │───►│  Translation   │ │
│  │  Controller  │    │   Module     │    │  & Sentiment   │ │
│  └──────────────┘    └──────┬───────┘    └───────┬────────┘ │
│                             │                     │         │
│                             │                     │         │
│                       ┌────▼────┐           ┌────▼─────┐    │
│  ┌──────────────┐     │  Naver  │           │  OpenAI  │    │
│  │   SQLite     │     │   OCR   │           │ GPT-4o   │    │
│  │   Database   │     │   API   │           │   API    │    │
│  └──────────────┘     └─────────┘           └──────────┘    │
│         │                                          │        │
│         │         ┌────────────────────────────────┘        │
│         │         │   (Translation, Sentiment, TTS)         │
│         │         ▼                                         │
│  ┌──────▼──────────────────┐                                │
│  │  Session & Page Models  │                                │
│  │  (Store results in DB)  │                                │
│  └─────────────────────────┘                                │
└─────────────────────────────────────────────────────────────┘
                            │
                   Translated Text + TTS Audio
                            │
                            ▼
                    Frontend (Display)
```

## What the Demo Demonstrates

### Goals Achieved
1. **Multilingual Book Translation**: Successfully transforms physical picture books into multilingual interactive resources using AI-powered OCR and translation.

2. **Emotional Text-to-Speech**: Implemented expressive TTS that analyzes emotional context and delivers natural, engaging narration.

3. **Optimized User Experience**:
   - Reduced latency through partial result streaming
   - Interactive UI with text box repositioning
   - Smooth page navigation and session management
   - Reading history database for tracking progress
   - Motivational reading statistics and encouragement system

4. **Educational Support**: Provides reading progress feedback (pages read, word count, time spent) with congratulations messaging to encourage young learners and build reading confidence.

5. **Clean Architecture**: Refactored frontend using MVVM pattern for maintainability and testability.

### Demo Flow
1. **Start Session**: Select language and voice preferences
2. **Camera Capture**: Use ML Kit to scan book pages
3. **OCR & Translation**: Naver OCR recognizes text, OpenAI translates to selected language
4. **Interactive Reading**: View translated text overlay, play emotional TTS audio
5. **Navigation**: Move between pages, adjust text positions as needed
6. **Complete & Celebrate**: Congratulations screen shows reading statistics (pages, words, time) to encourage continued learning
7. **Save & Manage**: Session management allows saving progress and deleting unwanted sessions

## Configuration

### Database
The backend uses **SQLite** (`db.sqlite3`) for development and testing. For production deployment, PostgreSQL is supported.

## Troubleshooting

### Backend Issues

**Port 8000 already in use:**
```bash
# Find and kill process using port 8000
lsof -ti:8000 | xargs kill -9
```

**Missing API keys:**
```bash
# Make sure .env file exists in backend/ directory
# Check that all required API keys are set
cat backend/.env
```

### Frontend Issues

**Gradle sync fails:**
- Ensure Java 17+ is installed
- Check internet connection for dependency downloads
- Try: File → Invalidate Caches → Invalidate and Restart

**App cannot connect to backend:**
- Check backend is running: `curl http://localhost:8000/`
- For emulator: Use `http://10.0.2.2:8000/`
- For physical device: Use your computer's IP address
- Ensure firewall allows Python connections
- Verify device and computer are on same network

**Camera not working:**
- Ensure camera permissions are granted
- Check device has camera hardware
- Verify ML Kit dependencies are installed

## Project Status

- ✅ Project structure and setup
- ✅ Backend API (Django REST Framework)
- ✅ AI-powered OCR integration (Naver OCR)
- ✅ Translation service (OpenAI GPT-4o-mini)
- ✅ Emotional TTS implementation
- ✅ Camera integration with ML Kit
- ✅ Session management (save/delete functionality)
- ✅ Reading statistics and encouragement system
- ✅ Production deployment (AWS EC2 with CI/CD)

## License

This project is developed for educational purposes as part of the SNU SWPP course.

---

StoryBridge - 책으로 세계를 연결하는 다리
