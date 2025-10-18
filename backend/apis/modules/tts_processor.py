#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import csv
import time
import asyncio
import argparse
import sys
import kss
import shutil
from pathlib import Path
from typing import Dict, List, Any
from openai import AsyncOpenAI
from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate
from pydantic import BaseModel, Field
from dotenv import load_dotenv
load_dotenv()

# --- Prompt Templates ---

TRANSLATION_PROMPT = """
You are an expert multilingual children's-story adapter.
You will be given a block of Korean text that may contain up to three parts: [PREVIOUS], [CURRENT], and [NEXT].
Your task is to translate ONLY the [CURRENT] Korean sentence into a single, fluent {target_lang} sentence.
Use the [PREVIOUS] and [NEXT] sentences for context to ensure pronouns, flow, and style are correct.
"""

SENTIMENT_PROMPT = """
You are an expert children's audiobook voice director.
Analyze the sentiment of this Korean sentence and provide expressive ENGLISH directions for Tone, Pacing, and Emotion.
"""

class Translation(BaseModel):
    """A single, fluent translation of a Korean sentence."""
    translated_text: str = Field(..., description="The translated sentence in the target language.", alias="translation")

class Sentiment(BaseModel):
    """Performance direction for a voice actor based on sentiment analysis."""
    tone: str = Field(..., description="The tone of voice to use (e.g. 'Gently chiding, inquisitive').")
    pacing: str = Field(..., description="The pacing of the speech (e.g. 'Direct, with a slight upward inflection').")
    emotion: str = Field(..., description="The emotion to convey (e.g. 'Mildly exasperated but friendly').")

class TTSModule:
    def __init__(self, out_dir="out_audio", log_dir="log", target_lang: str = "English"):
        self.client = AsyncOpenAI()
        self.TTS_MODEL = "gpt-4o-mini-tts"
        self.OUT_DIR = Path(out_dir)
        self.LOG_DIR = Path(log_dir)
        self.CSV_LOG = self.LOG_DIR / "sentence_log.csv"
        
        self.target_lang = target_lang
        self.llm = ChatOpenAI(model="gpt-4o-mini", temperature=0.7)
        self.translation_chain = self._create_translation_chain()
        self.sentiment_chain = self._create_sentiment_chain()

        # Reset dirs each run
        if self.OUT_DIR.exists(): shutil.rmtree(self.OUT_DIR)
        if self.LOG_DIR.exists(): shutil.rmtree(self.LOG_DIR)
        self.OUT_DIR.mkdir(parents=True)
        self.LOG_DIR.mkdir(parents=True)

    def _create_translation_chain(self):
        prompt = ChatPromptTemplate.from_messages([
            ("system", TRANSLATION_PROMPT),
            ("user", "{text_with_context}")
        ])
        return prompt | self.llm.with_structured_output(Translation)

    def _create_sentiment_chain(self):
        prompt = ChatPromptTemplate.from_messages([
            ("system", SENTIMENT_PROMPT),
            ("user", "{korean_text}")
        ])
        return prompt | self.llm.with_structured_output(Sentiment)
    
    async def translate(self, text_with_context: str) -> Dict[str, Any]:
        for attempt in range(3):
            try:
                t0 = time.time()
                response = await self.translation_chain.ainvoke({
                    "text_with_context": text_with_context,
                    "target_lang": self.target_lang
                })
                latency = time.time() - t0
                return {"result": response, "latency": round(latency, 3)}
            except Exception as e:
                print(f"Translation attempt {attempt + 1} failed: {e}")
                if attempt == 2:
                    return {"result": e, "latency": -1.0}
                await asyncio.sleep(0.7)
        return {"result": None, "latency": -1.0}

    async def sentiment(self, korean_text: str) -> Dict[str, Any]:
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

    async def synthesize_tts(self, voice: str, text: str, instructions: str, out_path: Path, response_format: str) -> tuple[float, bytes]:
        out_path.parent.mkdir(parents=True, exist_ok=True)
        t0 = time.time()
        try:
            response = await self.client.audio.speech.create(
                model=self.TTS_MODEL, 
                voice=voice, 
                input=text, 
                instructions=instructions,
                response_format=response_format
            )
            audio_bytes = response.content
            
            with open(out_path, "wb") as f:
                f.write(audio_bytes)
            
            return round(time.time() - t0, 3), response.content
        
        
        except Exception as e:
            print(f"TTS error for {out_path.name}: {e}")
            return -1.0, None

    async def process_paragraph(self, page: Dict[str, str], log_csv: bool, check_latency: bool, response_format: str = "mp3"):
        file_name = page["fileName"]
        sentences = kss.split_sentences(page["text"].strip())
        if not sentences:
            return {"status": "no_sentences"}

        stem = Path(file_name).stem
        
        async def process_single_sentence(i: int, sentence: str):
            # Build context for translation
            context = [f"[CURRENT]: {sentence}"]
            if i > 0: context.insert(0, f"[PREVIOUS]: {sentences[i-1]}")
            if i < len(sentences) - 1: context.append(f"[NEXT]: {sentences[i+1]}")
            context_prompt = "\n".join(context)
            
            # Run translation and sentiment in parallel for this sentence
            trans_response, senti_response = await asyncio.gather(
                self.translate(context_prompt),
                self.sentiment(sentence)
            )
            
            trans_result = trans_response["result"]
            senti_result = senti_response["result"]
            
            if isinstance(trans_result, Exception) or isinstance(senti_result, Exception):
                return None
            
            translated = trans_result.translated_text.strip()
            if not translated:
                return None
            
            # Immediately start TTS (don't wait for other sentences)
            voice = "shimmer"
            out_file = self.OUT_DIR / f"{stem}_sent{i+1}.{response_format}"
            tts_instr = (
                f"[Affect: A gentle, curious narrator with a clear accent, guiding a magical, child-friendly adventure through a fairy tale world.]"
                f"[Pronunciation: Clear and precise, with an emphasis on storytelling, ensuring the words are easy to follow and enchanting to listen to.]"
                f"[Tone: {senti_result.tone}] [Emotion: {senti_result.emotion}] [Pacing: {senti_result.pacing}]"
            )
            
            tts_latency, tts_result = await self.synthesize_tts(voice, translated, tts_instr, out_file, response_format)
            
            return {
                "ko_sentence": sentence, "translation": translated, "path": str(out_file),
                "tone": senti_result.tone, "emotion": senti_result.emotion, "pacing": senti_result.pacing,
                "voice": voice, "trans_latency": trans_response["latency"], 
                "senti_latency": senti_response["latency"], "tts_latency": tts_latency, "tts_result": tts_result,
                "status": "ok"
            }
        
        # Process all sentences in pipeline - each starts TTS immediately when ready
        results = await asyncio.gather(*[
            process_single_sentence(i, sent) for i, sent in enumerate(sentences)
        ])
        
        # Filter out None results
        ok_results = [r for r in results if r is not None]
        
        if log_csv and ok_results:
            self._write_to_csv(ok_results, file_name, check_latency)
        
        return {"status": "ok" if ok_results else "failed", "details": ok_results}

    def _write_to_csv(self, results: List[Dict], file_name: str, check_latency: bool):
        header = ["page_file", "index", "ko", "translation", "tone", "emotion", "pacing", "voice", "path"]
        if check_latency:
            header += ["trans_latency", "senti_latency", "tts_latency"]
        if not self.CSV_LOG.exists():
            with open(self.CSV_LOG, "w", newline="", encoding="utf-8") as f:
                csv.writer(f).writerow(header)

        with open(self.CSV_LOG, "a", newline="", encoding="utf-8") as f:
            writer = csv.writer(f)
            for i, r in enumerate(results, start=1):
                row = [file_name, i, r["ko_sentence"], r["translation"], r["tone"], r["emotion"], r["pacing"], r["voice"], r["path"]]
                if check_latency:
                    row.extend([r["trans_latency"], r["senti_latency"], r["tts_latency"]])
                writer.writerow(row)
