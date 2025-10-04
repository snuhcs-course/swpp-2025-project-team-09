import asyncio
from pathlib import Path
from typing import List, Dict, Any

from ocr_processor import OCRProcessor
from story_processor import StoryProcessor


import asyncio
import time
from pathlib import Path
from typing import List, Dict, Any
from pydub import AudioSegment  
import os
from dotenv import load_dotenv
load_dotenv()

import kss
from ocr_processor import OCRProcessor
from story_processor import StoryProcessor


class OCR2TTS:
    def __init__(self, ocr_api_url: str, ocr_secret: str,
                 out_dir: str = "out_audio", log_dir: str = "log"):
        self.ocr = OCRProcessor(ocr_api_url, ocr_secret)
        self.story = StoryProcessor(out_dir=out_dir, log_dir=log_dir)
        
    @staticmethod
    def concat_paragraph_mp3s(out_dir: str, stem: str, num_paragraphs: int, delete_original: bool = True):
        out_dir = Path(out_dir)
        for i in range(1, num_paragraphs+1):
            files = sorted(out_dir.glob(f"{stem}_para{i}_sent*.mp3"))
            if not files:
                continue

            combined = AudioSegment.silent(duration=500) 
            for f in files:
                audio = AudioSegment.from_file(f, format="mp3")
                combined += audio + AudioSegment.silent(duration=250)

            out_path = out_dir / f"{stem}_para{i}.mp3"
            combined.export(out_path, format="mp3")

            if delete_original:
                for f in files:
                    try:
                        f.unlink()
                    except Exception as e:
                        print(f"{f} Failed to delete: {e}")

    async def process_image(self, image_path: str, target_lang: str = "English",
                            log_csv: bool = True, check_latency: bool = False) -> Dict[str, Any]:
        start_time = time.time()
        paragraphs = self.ocr.run_ocr(image_path)
        ocr_end_time = time.time()
        
        if not paragraphs:
            return {"status": "no_text"}

        stem = Path(image_path).stem
        all_results = []

        tasks = []
        for i, para in enumerate(paragraphs, 1):
            sentences = kss.split_sentences(para.strip())
            page = {"fileName": f"{stem}_para{i}.txt", "text": " ".join(sentences)}
            tasks.append(self.story.process_page(page, log_csv, check_latency))

        results = await asyncio.gather(*tasks)
        tts_end_time = time.time()
        # --- mp3 concat ---
        self.concat_paragraph_mp3s(self.story.OUT_DIR, stem, len(paragraphs))

        total_latency = round(time.time() - start_time, 3)
        ocr_latency = round(ocr_end_time - start_time, 3)
        tts_latency = round(tts_end_time - ocr_end_time, 3)
        return {"status": "ok", "details": all_results,  "ocr_latency_sec": ocr_latency, "tts_latency_sec": tts_latency,"total_latency_sec": total_latency}


async def main():
    ocr_api_url = os.getenv("OCR_API_URL")
    ocr_secret = os.getenv("OCR_SECRET")
    if not ocr_api_url or not ocr_secret:
        raise ValueError("Environment variables OCR_API_URL and OCR_SECRET must be set.")
    resources_dir = Path("./resources")

    image_files = sorted(resources_dir.glob("*.jpeg"))

    for image_path in image_files:
        stem = image_path.stem  # "001", "002" ...
        
        out_dir = Path("out_audio") / stem
        log_dir = Path("log") / stem

        pipeline = OCR2TTS(ocr_api_url, ocr_secret, out_dir=out_dir, log_dir=log_dir)

        result = await pipeline.process_image(str(image_path), target_lang="English",
                                              log_csv=True, check_latency=True)

        print(f"=== {image_path.name} results ===")
        print(result)

if __name__ == "__main__":
    asyncio.run(main())
