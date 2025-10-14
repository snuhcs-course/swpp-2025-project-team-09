#!/usr/bin/env python3
# -*- coding: utf-8 -*-

# TODO: Update this code as code using langchain (adapt least update file)

import os
import csv
import time
import json
import asyncio
import argparse
import sys
import kss
import shutil
from pathlib import Path
from typing import Dict, List, Any
from openai import AsyncOpenAI

TRANSLATION_SYSTEM_PROMPT_TEMPLATE = """You are an expert multilingual children's-story adapter.
You will be given a block of Korean text that may contain up to three parts: [PREVIOUS], [CURRENT], and [NEXT].
Your task is to translate ONLY the [CURRENT] Korean sentence into a single, fluent {target_lang} sentence.
Use the [PREVIOUS] and [NEXT] sentences for context to ensure pronouns, flow, and style are correct.
- Use standard {target_lang} double quotes (“ ”) for dialogue if needed.
- Your entire response must be ONLY the valid JSON for the translated sentence: {{"{target_lang}_translation": "..."}}
"""

SENTIMENT_SYSTEM_PROMPT = """
You are an expert voice director for children's audiobooks. Your task is to analyze the sentiment of a single KOREAN sentence and provide performance direction for a voice actor. Your response must be a JSON object with the following keys, using expressive ENGLISH words for all values.

- "Tone": Magical, warm, and inviting, creating a sense of wonder and excitement for young listeners. If the sentence is different, modify this (e.g., "Slightly tense", "Mysterious").
- "Pacing": Steady and measured, with slight pauses to emphasize magical moments. If the sentence is different, modify this (e.g., "Faster, more urgent").
- "Emotion": Wonder, curiosity, and a sense of adventure, with a lighthearted and positive vibe. If the sentence is different, modify this (e.g., "A hint of fear", "Joyful excitement").

**Example of High-Quality Output:**

**Input:** `“프레드릭, 넌 왜 일을 안 하니?”`
**Output JSON:**
```json
{
  "Tone": "Gently chiding, inquisitive",
  "Pacing": "Direct, with a slight upward inflection",
  "Emotion": "Mildly exasperated but friendly",
}

Important Rules:
- Be specific and creative for Tone, Pacing, and Emotion.
- Always include all keys. Your entire response must be ONLY the valid JSON.
"""

class StoryProcessor:
    def __init__(self, out_dir="out_audio", log_dir="log", openai_api_key=None):
        print(">>> StoryProcessor init start")
        self.client = AsyncOpenAI(api_key=openai_api_key or os.getenv("OPENAI_API_KEY"))
        print(">>> AsyncOpenAI created")
        self.TRANSLATE_MODEL = "gpt-4o-mini"
        self.SENTIMENT_MODEL = "gpt-4o-mini"
        self.TTS_MODEL = "gpt-4o-mini-tts"
        self.OUT_DIR = Path(out_dir)
        self.LOG_DIR = Path(log_dir)
        self.CSV_LOG = self.LOG_DIR / "sentence_log.csv"

        # Reset dirs each run
        if self.OUT_DIR.exists():
            shutil.rmtree(self.OUT_DIR)
        if self.LOG_DIR.exists():
            shutil.rmtree(self.LOG_DIR)
        self.OUT_DIR.mkdir(parents=True)
        self.LOG_DIR.mkdir(parents=True)
        print(">>> StoryProcessor ready")

    async def warm_up_api(self):
        try:
            await self.client.chat.completions.create(
                model=self.TRANSLATE_MODEL,
                messages=[{"role": "user", "content": "Hello"}],
                max_tokens=2
            )
        except Exception as e:
            print(f"⚠️ Warm-up failed: {e}")

    async def translate(self, text_with_context: str, target_lang: str="English") -> Dict[str, Any]:
        system_prompt = TRANSLATION_SYSTEM_PROMPT_TEMPLATE.format(target_lang=target_lang)
        for attempt in range(3):
            try:
                t0 = time.time()
                resp = await self.client.chat.completions.create(
                    model=self.TRANSLATE_MODEL, response_format={"type": "json_object"},
                    messages=[
                        {"role": "system", "content": system_prompt},
                        {"role": "user", "content": text_with_context},
                    ], temperature=0.7,
                )
                latency = time.time() - t0
                data = json.loads(resp.choices[0].message.content)
                data["_latency_sec"] = round(latency, 3)
                return data
            except Exception:
                if attempt == 2: raise
                await asyncio.sleep(0.7)
        return {}

    async def sentiment(self, korean_text: str) -> Dict[str, Any]:
        for attempt in range(3):
            try:
                t0 = time.time()
                resp = await self.client.chat.completions.create(
                    model=self.SENTIMENT_MODEL,
                    response_format={"type": "json_object"},
                    messages=[
                        {"role": "system", "content": SENTIMENT_SYSTEM_PROMPT},
                        {"role": "user", "content": korean_text},
                    ],
                    temperature=0.7,
                )
                latency = time.time() - t0
                data = json.loads(resp.choices[0].message.content)
                data["_latency_sec"] = round(latency, 3)
                return data
            except Exception:
                if attempt == 2:
                    raise
                await asyncio.sleep(0.7)
        return {}

    async def synthesize_tts(self, voice: str, text: str, instructions: str, out_path: Path) -> float:
        out_path.parent.mkdir(parents=True, exist_ok=True)
        t0 = time.time()
        try:
            async with self.client.audio.speech.with_streaming_response.create(
                model=self.TTS_MODEL, voice=voice, input=text, instructions=instructions
            ) as response:
                await response.stream_to_file(out_path)
            return round(time.time() - t0, 3)
        except Exception as e:
            print(f"TTS error for {out_path.name}: {e}")
            return -1.0

    async def process_sentence(self, index: int, sentences: List[str], stem: str, target_lang: str, check_latency: bool) -> Dict:
        current = sentences[index]
        context = []
        if index > 0: context.append(f"[PREVIOUS]: {sentences[index-1]}")
        context.append(f"[CURRENT]: {current}")
        if index < len(sentences)-1: context.append(f"[NEXT]: {sentences[index+1]}")
        context_prompt = "\n".join(context)

        trans_task = self.translate(context_prompt, target_lang)
        senti_task = self.sentiment(current)
        trans, senti = await asyncio.gather(trans_task, senti_task)

        translated = trans.get(f"{target_lang}_translation", "").strip()
        if not translated:
            return {"status": "translation_failed"}

        voice = "shimmer"
        out_mp3 = self.OUT_DIR / f"{stem}_sent{index+1}.mp3"
        tts_instr = (
            f"[Affect: A gentle, curious narrator with a clear American accent, guiding a magical, child-friendly adventure through a fairy tale world.]"
            f"[Pronunciation: Clear and precise, with an emphasis on storytelling, ensuring the words are easy to follow and enchanting to listen to.]"
            f"[Tone: {senti.get('Tone')}] [Emotion: {senti.get('Emotion')}] [Pacing: {senti.get('Pacing')}]"
        )
        tts_latency = await self.synthesize_tts(voice, translated, tts_instr, out_mp3)

        result = {
            "status": "ok",
            "ko_sentence": current,
            "translation": translated,
            "path": str(out_mp3),
            "tone": senti.get("Tone"),
            "emotion": senti.get("Emotion"),
            "pacing": senti.get("Pacing"),
            "voice": voice,
        }
        if check_latency:
            result.update({
                "trans_latency": trans.get("_latency_sec", 0),
                "senti_latency": senti.get("_latency_sec", 0),
                "tts_latency": tts_latency,
            })
        return result

    async def process_page(self, idx: int, page: Dict[str, str], target_lang: str, log_csv: bool, check_latency: bool):
        file_name = page["fileName"]
        sentences = kss.split_sentences(page["text"].strip())
        if not sentences: return {"status": "no_sentences"}

        stem = Path(file_name).stem
        results = await asyncio.gather(*[
            self.process_sentence(i, sentences, stem, target_lang, check_latency)
            for i in range(len(sentences))
        ])
        ok_results = [r for r in results if r.get("status") == "ok"]
        
        translated_sentences = [r["translation"] for r in ok_results if "translation" in r]
        full_translation = " ".join(translated_sentences).strip()

        if log_csv and ok_results:
            header = ["page_file", "index", "ko", "translation", "tone", "emotion", "pacing", "voice", "path"]
            if check_latency:
                header += ["trans_latency", "senti_latency", "tts_latency"]
            if not self.CSV_LOG.exists():
                with open(self.CSV_LOG, "w", newline="", encoding="utf-8") as f: csv.writer(f).writerow(header)
            with open(self.CSV_LOG, "a", newline="", encoding="utf-8") as f:
                writer = csv.writer(f)
                for i, r in enumerate(ok_results, start=1):
                    row = [file_name, i, r["ko_sentence"], r["translation"], r["tone"], r["emotion"], r["pacing"], r["voice"], r["path"]]
                    if check_latency: row += [r["trans_latency"], r["senti_latency"], r["tts_latency"]]
                    writer.writerow(row)

        return {
            "status": "ok" if ok_results else "failed",
            "translation_text": full_translation,
            "details": ok_results
        }

