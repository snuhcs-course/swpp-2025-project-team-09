#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import csv
import time
import asyncio
import base64
import shutil
from pathlib import Path
from typing import Dict, List, Any
from openai import AsyncOpenAI
from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate
from pydantic import BaseModel, Field
from dotenv import load_dotenv
import kss

load_dotenv()


# Prompt Templates.
TRANSLATION_PROMPT = """
You are an expert multilingual children's-story adapter.
You will be given a block of Korean text that may contain up to three
parts: [PREVIOUS], [CURRENT], and [NEXT].
Your task is to translate ONLY the [CURRENT] Korean sentence into a
single, fluent {target_lang} sentence.
Use the [PREVIOUS] and [NEXT] sentences for context to ensure pronouns,
flow, and style are correct.
"""

SENTIMENT_PROMPT = """
You are an expert children's audiobook voice director.
Analyze the sentiment of this Korean sentence and provide expressive
ENGLISH directions for Tone, Pacing, and Emotion.
"""


# Pydantic Models.
class Translation(BaseModel):
    """A single, fluent translation of a Korean sentence."""

    translated_text: str = Field(
        ...,
        description="The translated sentence in the target language.",
        alias="translation",
    )


class Sentiment(BaseModel):
    """Performance direction for voice actor based on sentiment."""

    tone: str = Field(..., description="The tone of voice to use.")
    pacing: str = Field(..., description="The pacing of the speech.")
    emotion: str = Field(..., description="The emotion to convey.")


class TTSModule:
    def __init__(
        self, out_dir="out_audio", log_dir="log", target_lang: str = "English"
    ):
        self.client = AsyncOpenAI()
        self.TTS_MODEL = "gpt-4o-mini-tts"
        self.TTS_MODEL_LITE = "tts-1"
        self.OUT_DIR = Path(out_dir)
        self.LOG_DIR = Path(log_dir)
        self.CSV_LOG = self.LOG_DIR / "sentence_log.csv"

        self.target_lang = target_lang
        self.llm = ChatOpenAI(model="gpt-4o-mini", temperature=0.7)
        self.translation_chain = self._create_translation_chain()
        self.sentiment_chain = self._create_sentiment_chain()

        # Reset directories
        if self.OUT_DIR.exists():
            shutil.rmtree(self.OUT_DIR)
        if self.LOG_DIR.exists():
            shutil.rmtree(self.LOG_DIR)
        self.OUT_DIR.mkdir(parents=True)
        self.LOG_DIR.mkdir(parents=True)

    def _create_translation_chain(self):
        prompt = ChatPromptTemplate.from_messages(
            [("system", TRANSLATION_PROMPT), ("user", "{text_with_context}")]
        )
        return prompt | self.llm.with_structured_output(Translation)

    def _create_sentiment_chain(self):
        prompt = ChatPromptTemplate.from_messages(
            [("system", SENTIMENT_PROMPT), ("user", "{korean_text}")]
        )
        return prompt | self.llm.with_structured_output(Sentiment)

    async def translate(self, text_with_context: str) -> Dict[str, Any]:
        """Translate text with retry logic."""
        for attempt in range(3):
            try:
                t0 = time.time()
                response = await self.translation_chain.ainvoke(
                    {
                        "text_with_context": text_with_context,
                        "target_lang": self.target_lang,
                    }
                )
                latency = time.time() - t0
                return {"result": response, "latency": round(latency, 3)}
            except Exception as e:
                print(f"Translation attempt {attempt + 1} failed: {e}")
                if attempt == 2:
                    return {"result": e, "latency": -1.0}
                await asyncio.sleep(0.7)
        return {"result": None, "latency": -1.0}

    async def sentiment(self, korean_text: str) -> Dict[str, Any]:
        """Analyze sentiment with retry logic."""
        for attempt in range(3):
            try:
                t0 = time.time()
                response = await self.sentiment_chain.ainvoke(
                    {"korean_text": korean_text}
                )
                latency = time.time() - t0
                return {"result": response, "latency": round(latency, 3)}
            except Exception as e:
                print(f"Sentiment attempt {attempt + 1} failed: {e}")
                if attempt == 2:
                    return {"result": e, "latency": -1.0}
                await asyncio.sleep(0.7)
        return {"result": None, "latency": -1.0}

    async def synthesize_tts(
        self,
        voice: str,
        text: str,
        instructions: str,
        out_path: Path,
        response_format: str,
    ) -> tuple[float, bytes]:
        """Synthesize TTS audio."""
        out_path.parent.mkdir(parents=True, exist_ok=True)
        t0 = time.time()
        try:
            response = await self.client.audio.speech.create(
                model=self.TTS_MODEL,
                voice=voice,
                input=text,
                instructions=instructions,
                response_format=response_format,
            )
            audio_bytes = response.content

            with open(out_path, "wb") as f:
                f.write(audio_bytes)

            return round(time.time() - t0, 3), audio_bytes

        except Exception as e:
            print(f"TTS error for {out_path.name}: {e}")
            return -1.0, None

    async def synthesize_tts_lite(
        self, voice: str, text: str, out_path: Path = None, response_format: str = "mp3"
    ) -> tuple[float, bytes]:
        """light model for Cover TTS - no sentiments"""
        t0 = time.time()
        try:
            response = await self.client.audio.speech.create(
                model=self.TTS_MODEL_LITE,  # tts-1
                voice=voice,
                input=text,
                response_format=response_format,
            )
            audio_bytes = response.content

            if out_path:
                out_path.parent.mkdir(parents=True, exist_ok=True)
                with open(out_path, "wb") as f:
                    f.write(audio_bytes)

            return round(time.time() - t0, 3), audio_bytes

        except Exception as e:
            print(f"TTS Lite error: {e}")
            return -1.0, None

    async def get_translations_only(self, page: Dict[str, str]) -> Dict[str, Any]:
        """
        Get translations and sentiment for all sentences
        WITHOUT running TTS.
        Used by backend to get translations before TTS.

        Args:
            page: {"fileName": "...", "text": "..."}

        Returns:
            {
                "status": "ok",
                "sentences": [
                    {
                        "translation": "...",
                        "tone": "...",
                        "emotion": "...",
                        "pacing": "...",
                        "korean": "..."
                    },
                    ...
                ]
            }
        """
        sentences = kss.split_sentences(page["text"].strip())
        if not sentences:
            return {"status": "no_sentences", "sentences": []}

        async def process_sentence(i: int, sentence: str):
            # Build context
            context = [f"[CURRENT]: {sentence}"]
            if i > 0:
                context.insert(0, f"[PREVIOUS]: {sentences[i-1]}")
            if i < len(sentences) - 1:
                context.append(f"[NEXT]: {sentences[i+1]}")
            context_prompt = "\n".join(context)

            # Run translation and sentiment in parallel
            trans_response, senti_response = await asyncio.gather(
                self.translate(context_prompt), self.sentiment(sentence)
            )

            trans_result = trans_response["result"]
            senti_result = senti_response["result"]

            is_error = isinstance(trans_result, Exception) or isinstance(
                senti_result, Exception
            )
            if is_error:
                return None

            translated = trans_result.translated_text.strip()
            if not translated:
                return None

            return {
                "translation": translated,
                "tone": senti_result.tone,
                "emotion": senti_result.emotion,
                "pacing": senti_result.pacing,
                "korean": sentence,
            }

        # Process all sentences in parallel
        results = await asyncio.gather(
            *[process_sentence(i, sent) for i, sent in enumerate(sentences)]
        )

        # Filter out None results
        ok_results = [r for r in results if r is not None]

        return {"status": "ok" if ok_results else "failed", "sentences": ok_results}

    async def run_tts_only(
        self,
        translation_data: Dict[str, Any],
        session_id: str,
        page_index: int,
        para_index: int,
        para_voice: str,
    ) -> List[str]:
        """
        Run TTS using pre-computed translations.
        Used by backend background thread.

        Args:
            translation_data: Result from get_translations_only()
            session_id, page_index, para_index: For file naming

        Returns:
            List of base64-encoded audio clips
        """
        sentences_data = translation_data["sentences"]
        if not sentences_data:
            return []

        stem = f"{session_id}_{page_index}_{para_index}"

        async def synthesize_sentence(i: int, sentence_data: dict):
            voice = para_voice
            out_file = self.OUT_DIR / f"{stem}_sent{i+1}.mp3"

            affect = (
                "[Affect: A gentle, curious narrator with a clear "
                "accent, guiding a magical, child-friendly "
                "adventure through a fairy tale world.]"
            )
            pronunciation = (
                "[Pronunciation: Clear and precise, with an emphasis "
                "on storytelling, ensuring the words are easy to "
                "follow and enchanting to listen to.]"
            )
            mood = (
                f"[Tone: {sentence_data['tone']}] "
                f"[Emotion: {sentence_data['emotion']}] "
                f"[Pacing: {sentence_data['pacing']}]"
            )
            tts_instr = affect + pronunciation + mood

            tts_latency, tts_result = await self.synthesize_tts(
                voice=voice,
                text=sentence_data["translation"],
                instructions=tts_instr,
                out_path=out_file,
                response_format="mp3",
            )

            if tts_result:
                return base64.b64encode(tts_result).decode("utf-8")
            return None

        # Run TTS for all sentences in parallel
        audio_results = await asyncio.gather(
            *[
                synthesize_sentence(i, sent_data)
                for i, sent_data in enumerate(sentences_data)
            ]
        )

        # Filter out None results
        return [audio for audio in audio_results if audio is not None]

        # async def translate_and_tts_cover(self, title: str, session_id: str, page_index: int) -> tuple[str, str]:
        """
        Translate title and generate TTS for both male and female voices.
        
        Args:
            title: Korean title text
            session_id: Session UUID
            page_index: Page index (0 for cover)
            
        Returns:
            (tts_male_base64, tts_female_base64)
        """
        # Translate the title
        trans_response = await self.translate(f"[CURRENT]: {title}")

        if isinstance(trans_response["result"], Exception):
            return "", ""

        translated_text = trans_response["result"].translated_text.strip()
        if not translated_text:
            return "", ""

        # Build translation data structure for TTS
        translation_data = {
            "status": "ok",
            "sentences": [
                {
                    "translation": translated_text,
                    "tone": "",  # Not used for cover
                    "emotion": "",  # Not used for cover
                    "pacing": "",  # Not used for cover
                    "korean": title,
                }
            ],
        }

        # Generate TTS for both voices (now returns tuple directly)
        tts_male, tts_female = await self.run_tts_cover_only(
            translation_data, session_id, page_index, 0
        )

        return tts_male, tts_female

    async def translate_and_tts_cover(
        self, title: str, session_id: str, page_index: int
    ) -> tuple[str, str]:
        """
        Translate title and generate TTS for both male and female voices.

        Args:
            title: Korean title text
            session_id: Session UUID
            page_index: Page index (0 for cover)

        Returns:
            (tts_male_base64, tts_female_base64)
        """
        # Translate the title
        trans_response = await self.translate(f"[CURRENT]: {title}")

        if isinstance(trans_response["result"], Exception):
            return "", ""

        translated_text = trans_response["result"].translated_text.strip()
        if not translated_text:
            return "", ""

        # Generate TTS for male voice (echo)
        male_latency, male_audio = await self.synthesize_tts_lite(
            voice="echo", text=translated_text
        )

        # Generate TTS for female voice (shimmer)
        female_latency, female_audio = await self.synthesize_tts_lite(
            voice="shimmer", text=translated_text
        )

        # Convert to base64
        male_b64 = base64.b64encode(male_audio).decode("utf-8") if male_audio else ""
        female_b64 = (
            base64.b64encode(female_audio).decode("utf-8") if female_audio else ""
        )

        return translated_text, male_b64, female_b64

        # async def run_tts_cover_only(self, translation_data: Dict[str, Any], session_id: str, page_index: int, para_index: int) -> tuple[str, str]:
        """
        Run TTS for book cover titles without sentiment analysis.
        Generates both male (echo) and female (shimmer) voices.
        Uses a fixed instruction suited for title reading.
        
        Returns:
            (male_audio_base64, female_audio_base64)
        """
        sentences_data = translation_data["sentences"]
        if not sentences_data:
            return "", ""

        stem = f"{session_id}_{page_index}_{para_index}"

        async def synthesize_sentence(
            i: int, sentence_data: dict, voice: str, voice_type: str
        ):
            out_file = self.OUT_DIR / f"{stem}_cover_{voice_type}_sent{i+1}.mp3"

            # Fixed instruction for cover title reading
            tts_instr = (
                "[Affect: Speak clearly and confidently, as if announcing the title of a children's storybook.] "
                "[Tone: Warm, friendly, and engaging, inviting the listener into a magical world.] "
                "[Pacing: Steady and clear, with emphasis on key words in the title.]"
            )

            tts_latency, tts_result = await self.synthesize_tts(
                voice=voice,
                text=sentence_data.get("translation", ""),
                instructions=tts_instr,
                out_path=out_file,
                response_format="mp3",
            )

            if tts_result:
                return base64.b64encode(tts_result).decode("utf-8")
            return None

        # Generate male and female voices for all sentences concurrently
        male_tasks = [
            synthesize_sentence(i, sent_data, "echo", "male")
            for i, sent_data in enumerate(sentences_data)
        ]
        female_tasks = [
            synthesize_sentence(i, sent_data, "shimmer", "female")
            for i, sent_data in enumerate(sentences_data)
        ]

        # Run all TTS tasks in parallel
        all_results = await asyncio.gather(*male_tasks, *female_tasks)

        # Split results into male and female
        mid_point = len(male_tasks)
        male_results = all_results[:mid_point]
        female_results = all_results[mid_point:]

        # Filter out None results and join (for cover, typically just one sentence).
        male_audio = male_results[0] if male_results and male_results[0] else ""
        female_audio = female_results[0] if female_results and female_results[0] else ""

        return male_audio, female_audio

    async def process_paragraph(
        self,
        page: Dict[str, str],
        log_csv: bool,
        check_latency: bool,
        response_format: str = "mp3",
    ):
        """
        Complete processing: translation + sentiment + TTS.
        Legacy method - kept for backwards compatibility.
        """
        file_name = page["fileName"]
        sentences = kss.split_sentences(page["text"].strip())
        if not sentences:
            return {"status": "no_sentences"}

        stem = Path(file_name).stem

        async def process_single_sentence(i: int, sentence: str):
            # Build context
            context = [f"[CURRENT]: {sentence}"]
            if i > 0:
                context.insert(0, f"[PREVIOUS]: {sentences[i-1]}")
            if i < len(sentences) - 1:
                context.append(f"[NEXT]: {sentences[i+1]}")
            context_prompt = "\n".join(context)

            # Run translation and sentiment in parallel
            trans_response, senti_response = await asyncio.gather(
                self.translate(context_prompt), self.sentiment(sentence)
            )

            trans_result = trans_response["result"]
            senti_result = senti_response["result"]

            is_error = isinstance(trans_result, Exception) or isinstance(
                senti_result, Exception
            )
            if is_error:
                return None

            translated = trans_result.translated_text.strip()
            if not translated:
                return None

            # Run TTS
            voice = "shimmer"
            out_file = self.OUT_DIR / f"{stem}_sent{i+1}.{response_format}"
            affect = (
                "[Affect: A gentle, curious narrator with a clear "
                "accent, guiding a magical, child-friendly "
                "adventure through a fairy tale world.]"
            )
            pronunciation = (
                "[Pronunciation: Clear and precise, with an emphasis "
                "on storytelling, ensuring the words are easy to "
                "follow and enchanting to listen to.]"
            )
            mood = (
                f"[Tone: {senti_result.tone}] "
                f"[Emotion: {senti_result.emotion}] "
                f"[Pacing: {senti_result.pacing}]"
            )
            tts_instr = affect + pronunciation + mood

            tts_latency, tts_result = await self.synthesize_tts(
                voice, translated, tts_instr, out_file, response_format
            )

            return {
                "ko_sentence": sentence,
                "translation": translated,
                "path": str(out_file),
                "tone": senti_result.tone,
                "emotion": senti_result.emotion,
                "pacing": senti_result.pacing,
                "voice": voice,
                "trans_latency": trans_response["latency"],
                "senti_latency": senti_response["latency"],
                "tts_latency": tts_latency,
                "tts_result": tts_result,
                "status": "ok",
            }

        # Process all sentences in parallel
        results = await asyncio.gather(
            *[process_single_sentence(i, sent) for i, sent in enumerate(sentences)]
        )

        # Filter out None results
        ok_results = [r for r in results if r is not None]

        if log_csv and ok_results:
            self._write_to_csv(ok_results, file_name, check_latency)

        status = "ok" if ok_results else "failed"
        return {"status": status, "details": ok_results}

    def _write_to_csv(self, results: List[Dict], file_name: str, check_latency: bool):
        """Write results to CSV log."""
        header = [
            "page_file",
            "index",
            "ko",
            "translation",
            "tone",
            "emotion",
            "pacing",
            "voice",
            "path",
        ]
        if check_latency:
            header += ["trans_latency", "senti_latency", "tts_latency"]

        if not self.CSV_LOG.exists():
            with open(self.CSV_LOG, "w", newline="", encoding="utf-8") as f:
                csv.writer(f).writerow(header)

        with open(self.CSV_LOG, "a", newline="", encoding="utf-8") as f:
            writer = csv.writer(f)
            for i, r in enumerate(results, start=1):
                row = [
                    file_name,
                    i,
                    r["ko_sentence"],
                    r["translation"],
                    r["tone"],
                    r["emotion"],
                    r["pacing"],
                    r["voice"],
                    r["path"],
                ]
                if check_latency:
                    row.extend(
                        [r["trans_latency"], r["senti_latency"], r["tts_latency"]]
                    )
                writer.writerow(row)
