# Project: StoryBridge

# How to Run Demo (Execution Instructions)

This demo contains **two processes** to run:  
- The **Android client application**  
- The **Python backend server**

---

## 1. Initial Setup and Branch Checkout

Follow the steps below to set up the project locally and check out the demo branch.

### Step 1. Clone the repository
```bash
git clone https://github.com/snuhcs-course/swpp-2025-project-team-09.git
```

### Step 2. Navigate to the project directory
```bash
cd swpp-2025-project-team-09
```

### Step 3. Checkout the demo branch
```bash
git checkout iteration-1-demo
```

## 2. Android App Setup

### Step 1. Environment

Please make sure your development environment matches the following version specifications required for this course:

| Item | Version |
|------|----------|
| **Android Studio SDK** | 34 |
| **compileSdk** | 34 |
| **minSdk** | 24 |
| **targetSdk** | 34 |
| **Gradle JDK** | Embedded JDK (21.0.6) |
| **sourceCompatibility** | JavaVersion.VERSION_11 |
| **targetCompatibility** | JavaVersion.VERSION_11 |

You can use either of the following devices for testing:
- **Physical Device:** Samsung Galaxy Tab (Android 13 or 14)  
- **Emulator:** *Medium Tablet* (2560×1600, 320 dpi, API 34)

After opening the project in Android Studio, click **“Sync Now”** to automatically download all required Gradle dependencies.

### Step 2. Open the Android Project
1. Launch **Android Studio**.  
2. Select **“Open an existing project”** and open: android-app/
3. Wait until Gradle sync completes successfully.

### Step 3. Run the App
1. Connect a physical device or start the emulator.  
2. Click **Run ▶️** in Android Studio.  
3. The app will launch on your device, starting from the **MainActivity** screen.  

---

## 3. Backend Server Setup

### Overview
Pipeline to extract Korean text from images, translate to English, analyze sentiment, and generate audio narration.

## Prerequisites
1.  **Miniconda/Anaconda:** You must have a Conda installation. We recommend [Miniconda](https://docs.conda.io/en/latest/miniconda.html).
2.  **Platform-Specific Dependencies:** This project requires the MeCab-ko morphological analyzer. Please install it **before** creating the Conda environment. You only need to install this for Korean analysis speed up. Can skip installing this backend for kss if speed up is not needed.

    * **For macOS (Intel & Apple Silicon):** Install via [Homebrew](https://brew.sh/).
        ```bash
        brew install mecab mecab-ko-dic
        ```

    * **For Linux (Debian/Ubuntu):**
        ```bash
        bash <(curl -s [https://raw.githubusercontent.com/konlpy/konlpy/master/scripts/mecab.sh](https://raw.githubusercontent.com/konlpy/konlpy/master/scripts/mecab.sh))
        ```
    * **For Windows:** Please follow the official installation guides for MeCab-ko on Windows.

---

### Setup Instructions

**Step 1. Navigate to the backend directory**
```bash
cd backend
```

**Step 2. Create the Conda Environment**

Use the provided `environment.yml` file to automatically create a new Conda environment with all the necessary dependencies.

```bash
conda env create -f environment.yml
```

This will create a new environment named `story-ocr-tts-env`.

### ⚠️ Windows Setup Notes

On some Windows setups, the pip section in environment.yml might not install properly if a global Python installation interferes.
If the environment seems empty (only pip, setuptools, and wheel installed), follow these manual steps instead:

**(1) Remove any existing environment**
```bash
conda deactivate
conda env remove -n story-ocr-tts-env
```
**(2) Create a clean base environment**
```bash
conda create -n story-ocr-tts-env python=3.10 ffmpeg pip
```
**(3) Activate the environment**
```bash
conda activate story-ocr-tts-env
```
**(4) Install dependencies manually**
```bash
python -m pip install --upgrade pip setuptools wheel
python -m pip install openai kss requests pydub langchain langchain-openai python-dotenv
```

**Step 2. Set Your OpenAI API Key**

This project requires an OpenAI API key and a Naver OCR API URL. You must set them as environment variables before running the demo.

Please note that these keys are not included in the repository for security reasons.
If necessary (e.g., for TA verification), the .env file or keys can be provided upon request.

Create a `.env` file in `backend/` directory with the following content:
   ```
   OPENAI_API_KEY=sk-...
   OCR_API_URL=...
   OCR_SECRET=...
   ```


**Step 3. Activate the Environment**

Before running the script, you must activate the environment you created in step 2.

```bash
conda activate story-ocr-tts-env
```
## How to Run

```bash
python ./main.py
```

The generated audio files will be in the `out_audio/` directory.

# What the Demo Demonstrates

This first demo focuses on building the **basic structure and flow of StoryBridge**, including the user interface and backend pipeline connection.

### Implemented Features

| Area | Implemented Features |
|------|----------------------|
| **Android App (Frontend)** | - Basic UI layout for the StoryBridge app<br>- Page navigation and button click interactions<br>- Activity transitions between main screens (Main → Language → Voice → Camera → Result) |
| **Backend (Python)** | - Core pipeline to extract **Korean text from images**, translate to **English**, analyze **sentiment**, and generate **audio narration**|

### Goals Achieved
- Established the **end-to-end connection** between the Android client and the backend server.  
- Verified **UI navigation flow** and event handling across activities.  
- Built the **foundational OCR–Translation–TTS pipeline** in the backend to prepare for full integration in later iterations.

### Not Implemented Yet
- Complete OCR and TTS integration with the Android app.  
- Advanced UI components
- Multi-language translation selection

---

This demo demonstrates the **core flow** of StoryBridge — capturing an image, sending it to the backend, and preparing translated and narrated output — laying the groundwork for future full-feature integration.
