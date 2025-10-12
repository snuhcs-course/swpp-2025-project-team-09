# Story OCR-TTS

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

## Setup Instructions

**1. Get the Code**

Open your terminal and clone the project repository (or download and unzip the folder).

```bash
git clone <your-repository-url>
cd <your-project-folder>
```

**2. Create the Conda Environment**

Use the provided `environment.yml` file to automatically create a new Conda environment with all the necessary dependencies.

```bash
conda env create -f environment.yml
```

This will create a new environment named `story-ocr-tts-env`.

**3. Set Your OpenAI API Key**

This project requires an OpenAI API key. You must set it as an environment variable.


Create a `.env` file in `backend/` directory with the following content:
   ```
   OPENAI_API_KEY=sk-...
   OCR_API_URL=...
   OCR_SECRET=...
   ```


**4. Activate the Environment**

Before running the script, you must activate the environment you created in step 2.

```bash
conda activate story-ocr-tts-env
```
## How to Run


```bash
python ./main.py
```

The generated audio files will be in the `out_audio/` directory.