# example usage: ./run_story.sh 15 --lang English --log_csv --latency
#!/bin/bash
set -e

PAGE_NUM=${1:-1}
shift
EXTRA_ARGS="$@"
CONDA_ENV_NAME="story-tts-env"

conda run -n "$CONDA_ENV_NAME" python story_tts.py \
    --page "$PAGE_NUM" \
    $EXTRA_ARGS
