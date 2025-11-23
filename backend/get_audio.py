#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Simple OpenAI TTS script
Usage: python get_audio.py "content of text" --voice shimmer --lang Korean --output output.mp3
"""

import os
import argparse
from pathlib import Path
from openai import OpenAI
from dotenv import load_dotenv

# Load environment variables from backend/.env
load_dotenv()


def get_audio(text: str, voice: str = "shimmer", language: str = "Korean", output_file: str = "output.mp3", speed: float = 0.9, instructions: str = None):
    """
    Generate audio using OpenAI TTS API

    Args:
        text: Text content to convert to speech
        voice: Voice option - "alloy", "echo", "fable", "onyx", "nova", "shimmer"
        language: Language of the text (for reference, not used in API call)
        output_file: Output file path
        speed: Speech speed (0.25 to 4.0, default 0.9 for clearer pronunciation)
        instructions: Style instructions for the voice (e.g., "Speak in a bright, energetic, and clear manner")
    """
    # Initialize OpenAI client
    client = OpenAI(api_key=os.getenv("OPENAI_API_KEY"))

    # Default instructions for clear, bright, and energetic delivery
    if instructions is None:
        instructions = (
            "Speak in a bright and energetic tone. "
            "Pronounce each word clearly and distinctly. "
        )

    print(f"Generating audio...")
    print(f"  Text: {text}")
    print(f"  Voice: {voice}")
    print(f"  Language: {language}")
    print(f"  Speed: {speed}")
    print(f"  Instructions: {instructions}")
    print(f"  Output: {output_file}")

    try:
        # Call OpenAI TTS API with instructions support
        # Note: Only certain models support the instructions parameter
        response = client.audio.speech.create(
            model="gpt-4o-mini-tts",  # Model with instructions support
            voice=voice,
            input=text,
            speed=speed,
            instructions=instructions,  # Style guidance for pronunciation and tone
        )

        # Save audio to file
        output_path = Path(output_file)
        output_path.parent.mkdir(parents=True, exist_ok=True)

        with open(output_path, "wb") as f:
            f.write(response.content)

        print(f"\nSuccess! Audio saved to: {output_path.absolute()}")
        return str(output_path.absolute())

    except Exception as e:
        print(f"\nError: {e}")
        return None


def main():
    # Example usage with bright, energetic, and clear pronunciation
    get_audio(
        text="Chào mừng các bạn đến với Story Bridge.",
        voice="shimmer",
        language="vn",
        output_file="vietnam_woman.mp3",
        speed=1.0,  # Slower for clearer pronunciation
        # instructions will use default: bright, cheerful, energetic tone
    )


if __name__ == "__main__":
    main()
