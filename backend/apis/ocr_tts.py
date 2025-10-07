import asyncio
import time
from pathlib import Path
from typing import Dict, Any, List
from io import BytesIO
from pydub import AudioSegment

from .processors.ocr_processor import OCRProcessor
from .processors.story_processor import StoryProcessor

class OCR2TTS:
    def __init__(self, ocr_api_url: str = "", ocr_secret: str = "",
                 out_dir: str = "media/audio", log_dir: str = "media/log"):
        self.ocr = OCRProcessor(ocr_api_url, ocr_secret)
        self.story = StoryProcessor(out_dir=out_dir, log_dir=log_dir)

    @staticmethod
    def concat_paragraph_mp3s(out_dir: str, stem: str, num_paragraphs: int) -> BytesIO:
        """문단 단위 mp3를 합쳐서 BytesIO 객체로 반환"""
        out_dir = Path(out_dir)
        combined = AudioSegment.silent(duration=500)

        for i in range(1, num_paragraphs + 1):
            files = sorted(out_dir.glob(f"{stem}_para{i}_sent*.mp3"))
            if not files:
                continue

            for f in files:
                audio = AudioSegment.from_file(f, format="mp3")
                combined += audio + AudioSegment.silent(duration=250)

        buffer = BytesIO()
        combined.export(buffer, format="mp3")
        buffer.seek(0)
        return buffer

    def process_image(self, image_path: str, target_lang: str = "English") -> Dict[str, Any]:
        """이미지를 OCR → 번역 → TTS 처리 후 Django view로 반환할 데이터 생성"""

        start_time = time.time()

        # --- OCR 단계 ---
        paragraphs, bbox_results = self.ocr.run_ocr(image_path)
        ocr_end_time = time.time()

        if not paragraphs:
            return {"status": "no_text"}

        stem = Path(image_path).stem

        # --- 병렬 TTS (async) ---
        async def tts_pipeline(paragraphs: List[str]):
            tasks = []
            for i, para in enumerate(paragraphs, 1):
                tasks.append(self.story.process_page(i, para, target_lang))
            return await asyncio.gather(*tasks)

        asyncio.run(tts_pipeline(paragraphs))
        tts_end_time = time.time()

        # --- 오디오 병합 (in-memory) ---
        audio_buffer = self.concat_paragraph_mp3s(self.story.OUT_DIR, stem, len(paragraphs))

        total_latency = round(time.time() - start_time, 3)
        ocr_latency = round(ocr_end_time - start_time, 3)
        tts_latency = round(tts_end_time - ocr_end_time, 3)

        return {
            "status": "ok",
            "translation_text": " ".join(paragraphs),
            "audio_file": audio_buffer,          # BytesIO 객체
            "bbox_results": bbox_results,
            "latency": {
                "ocr_sec": ocr_latency,
                "tts_sec": tts_latency,
                "total_sec": total_latency
            }
        }
