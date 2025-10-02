# Story TTS Generator

This project uses OpenAI's APIs to translate Korean children's stories into English, analyze their sentiment, and generate high-quality audio narration for each sentence.

## Prerequisites

1.  **Git:** You'll need Git to clone the project.
2.  **Miniconda/Anaconda:** You must have a Conda installation. We recommend [Miniconda](https://docs.conda.io/en/latest/miniconda.html).

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

This will create a new environment named `story-tts-env`.

**3. Set Your OpenAI API Key**

This project requires an OpenAI API key. You must set it as an environment variable.

* **On macOS / Linux:**
    ```bash
    export OPENAI_API_KEY="sk-..." 
    ```
    *(Note: You will need to run this command every time you open a new terminal window, or add it to your shell's startup file like `~/.zshrc` or `~/.bash_profile`)*

* **On Windows (Command Prompt):**
    ```bash
    set OPENAI_API_KEY="sk-..."
    ```

**4. Activate the Environment**

Before running the script, you must activate the environment you created in step 2.

```bash
conda activate story-tts-env
```

## How to Run

Use the `run_story.sh` script to process a page from the story. The first argument is the page number.

```bash
# Make the script executable (only need to do this once)
chmod +x run_story.sh

# Example: Process page 15
./run_story.sh 15
```

The generated audio files will be in the `out_audio/` directory, and a detailed log will be in `log/sentence_log.csv`.