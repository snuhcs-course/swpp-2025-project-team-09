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
- Backend is automatically deployed to the EC2 server through GitHub Actions.

### Frontend
- Added audio playback button.
- Added cover page and voice selection features.
- Added text box movement functionality.
- Added page navigation functionality.
- Refactored the frontend using the MVVM architecture pattern.

### Testing
- Backend and frontend testing is available in the `testing/backend` branch.

## Backend Testing Setup

Follow these steps to set up a virtual environment and run backend tests.

### 1. Create and Activate Virtual Environment

```bash
# Move to backend/
cd ./backend

# Create a virtual environment
python -m venv venv

# Activate the virtual environment
source venv/bin/activate
```

After activating the virtual environment, install all dependencies listed in the `requirements.txt` file:

```bash
pip install -r requirements.txt
```

### 2. Run Backend Tests

Switch to the testing branch and execute the test script:

```bash
# Move to testing/backend branch
git checkout testing/backend

# Enter the backend directory
cd backend

# Run tests
python run_tests.py
```
![BE Coverage](https://i.imgur.com/Um7yEyo.png)
Most of the backend files have achieved 100% test coverage.