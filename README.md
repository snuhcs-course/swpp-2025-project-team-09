# StoryBridge Iteration 2 Demo

## Demo Video
[Google Drive Video Link](https://drive.google.com/file/d/1--f2O0EBmdfJ4LIOAiRf4Nkua0eDT1nl/view?usp=sharing)

## How to Run

To demonstrate the app with a local backend, we used **ngrok** to expose the backend server to your mobile emulator. Make sure the mobile device is connected to the same Wi-Fi as your local machine.

### 1. Install Dependencies
Before running the backend, install the required Python packages. Open a terminal and run:

```bash
cd backend
pip install -r requirements.txt
```
### 2. Configure Environment Variables

Create a .env file inside the backend folder with the following content:

```bash
OPENAI_API_KEY=<your_api_key>
OCR_API_URL=<your_api_url>
OCR_SECRET=<your_api_key>
```

This allows the backend to access OpenAI and Clova OCR services.

### 3. Start Django Backend
Open a terminal and run:

```bash
cd backend
python manage.py runserver 0.0.0.0:8000
```
This will start the Django development server accessible on your local network.

### 4. Start ngrok
In a separate terminal, run:
```bash
cd backend
python ngrok.py
```
If you encounter an authentication error, follow these steps:

1. Sign up at https://ngrok.com/.  
2. Go to **Your Authtoken** from the left menu and copy your token.  
3. Add your token via terminal and restart ngrok:
```bash
ngrok config add-authtoken <YOUR_TOKEN>
python ngrok.py
```

Then run:
```bash
ngrok http 8000
```
<img width="792" height="190" alt="Screenshot 2025-10-19 at 8 25 03 PM" src="https://github.com/user-attachments/assets/ca11b776-0a4a-44cd-a73a-67f7b4d8dc4f" />

### 5. Accessing the Web Interface
- ngrok provides a **Web Interface** where you can monitor incoming HTTP requests.
- The **Forwarding URL** should be used as the `BASE_URL` in `RetrofitClient` in your Android app.
  <img width="679" height="83" alt="Screenshot 2025-10-19 at 8 24 15 PM" src="https://github.com/user-attachments/assets/6ee35e3c-b1d7-4623-8c6d-6b524cb76d1d" />

### 6. Demo
Once ngrok is running, you can run the Android emulator or connect a physical device to the same Wi-Fi. All API calls will be forwarded to your local backend via the ngrok URL.

<br><br>
## Implemented Features

During Iteration 2, we successfully implemented almost all of the features we initially aimed for:

- **Backend**: Complete backend functionality including session management, user handling, page processing, and integration with AI modules (OCR, translation, TTS).  
- **HTTP APIs**: Full set of RESTful APIs for user, session, page, and processing operations.  
- **Frontend Main Flow**: Complete main flow from app launch to session completion, including user registration, session start, voice selection, reading activity, and session summary.  
- **Automatic Image Capture & Alignment**: Pages are captured automatically using CameraX, with real-time guidance for proper alignment and lighting correction.  
- **Display translated text & Audio Playback**: Translated text is displayed on screen, and corresponding audio is played using emotion-based TTS for an immersive reading experience.

Although the core features are in place, a few enhancements are planned to fully complete the initial goals:

- **Bounding Box & Audio Details**: Fine-tune bounding box handling and audio playback per detected text region.
- **Session Navigation & Review**: Implement navigation to previous pages within a session and the ability to review past sessions.

Once these improvements are added, all features originally targeted for this project will be fully implemented.

