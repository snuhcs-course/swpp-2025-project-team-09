# SWPP 2025 Project Team 09

## Project Overview
The backend is deployed on an AWS EC2 server, and the app communicates with that server.

---

## Installation and Run Instructions

1. Open **Android Studio**.
2. Open the `android-app` directory.
3. Wait until the Gradle build finishes.
4. Connect an emulator or a physical Android device.
5. Press the `Run` button to install and launch the app.

---

## Backend Server Information

The app connects to the following EC2 server endpoint:

```
private var BASE_URL = "http://ec2-15-164-229-164.ap-northeast-2.compute.amazonaws.com:8000"
```

This URL points to the backend API server.

---

## Implemented features

### Backend
- Optimized for lower user-perceived latency: the server sends partial translation results as soon as they are ready while waiting for TTS (text-to-speech) results to complete.

### Frontend
- Added audio playback button.
- Added cover page and voice selection features.
- Added text box movement functionality.
- Added page navigation functionality.

### Testing
- Backend testing is available in the `testing/backend` branch.
