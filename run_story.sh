#!/bin/bash
#
# This script is a wrapper to run the story_tts.py Python script
# within a specific Conda environment.

# Example Usage:
#
# 1. Generate default MP3 audio for page 15 with logging and latency checks:
#    ./run_story.sh 15 --lang English --log_csv --latency
#
# 2. Generate WAV audio for page 5:
#    ./run_story.sh 5 --format wav
#
# 3. Generate FLAC audio for page 3 with logging:
#    ./run_story.sh 3 --format flac --log_csv

# Exit immediately if a command exits with a non-zero status.
set -e

# --- Configuration ---

# The first argument is the page number (defaults to 1 if not provided).
PAGE_NUM=${1:-1}

# The name of your Conda environment.
CONDA_ENV_NAME="story-tts-env"

# --- Script Logic ---

# Check if a page number was provided.
if [[ -z "$1" ]]; then
  echo "Usage: ./run_story.sh <page_number> [optional_python_args]"
  echo "No page number provided. Defaulting to page 1."
else
  # Consume the first argument (page number) so it isn't passed again in EXTRA_ARGS.
  shift
fi

# All remaining arguments are captured and passed directly to the Python script.
EXTRA_ARGS="$@"

# --- Execution ---
echo "Running TTS process for page $PAGE_NUM with extra args: $EXTRA_ARGS"

conda run -n "$CONDA_ENV_NAME" python story_tts.py \
    --page "$PAGE_NUM" \
    $EXTRA_ARGS

echo "✅ Script finished for page $PAGE_NUM."
