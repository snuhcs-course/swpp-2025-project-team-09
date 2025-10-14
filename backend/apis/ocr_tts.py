import asyncio
import time
import os
from pathlib import Path
from typing import Dict, Any, List
from io import BytesIO
from pydub import AudioSegment
from dotenv import load_dotenv

from .processors.ocr_processor import OCRProcessor
from .processors.story_processor import StoryProcessor

# TODO: Exception handling 
# TODO: Save logs

class OCR2TTS:
    """
    Integrates OCR, translation, and TTS processing for a single image input.

    Attributes
    ----------
    ocr_api_url : str
        API endpoint URL for the OCR service.
    ocr_secret : str
        Secret key for authenticating OCR API requests.
    openai_api_key : str
        API key used for translation and TTS via OpenAI API.
    out_dir : str
        Directory path where generated audio files are temporarily stored.
    log_dir : str
        Directory path for logging intermediate results (CSV, latency, etc.).
    ocr : OCRProcessor
        Instance responsible for OCR text extraction and bounding box parsing.
    story : StoryProcessor
        Instance responsible for text translation, sentiment analysis, and TTS generation.
    """
    
    def __init__(self, 
                ocr_api_url: str = None,
                ocr_secret: str = None,
                openai_api_key: str = None,
                out_dir: str = "media/audio",
                log_dir: str = "media/log"):

        self.ocr_api_url = ocr_api_url or os.getenv("OCR_API_URL")
        self.ocr_secret = ocr_secret or os.getenv("OCR_SECRET_KEY")
        self.openai_api_key = openai_api_key or os.getenv("OPENAI_API_KEY")

        print(">>> OCR2TTS init start")
        self.ocr = OCRProcessor(self.ocr_api_url, self.ocr_secret)
        print(">>> OCRProcessor created")
        self.story = StoryProcessor(
            out_dir=out_dir,
            log_dir=log_dir,
            openai_api_key=self.openai_api_key
        )

    @staticmethod
    def concat_paragraph_mp3s(out_dir: str, stem: str, num_paragraphs: int) -> BytesIO:
        """
        Merges multiple paragraph-level MP3 files into a single audio buffer.

        Parameters
        ----------
        out_dir : str
            Directory containing paragraph-level MP3 files.
        stem : str
            File stem used to identify audio files for the same image.
        num_paragraphs : int
            Number of paragraphs (and corresponding MP3s) to merge.

        Returns
        -------
        io.BytesIO
            Combined audio file in memory as a BytesIO stream.
        """
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

    def process_image(self, image_path: str, target_lang: str = "English", log_csv=False, check_latency=False)-> Dict[str, Any]:
        """
        Executes the full OCR → Translation → TTS pipeline for a given image.

        Parameters
        ----------
        image_path : str
            Absolute path to the input image file.
        target_lang : str, optional
            Target translation language (default: "English").
        log_csv : bool, optional
            Whether to log detailed sentence-level results as CSV (default: False).
        check_latency : bool, optional
            Whether to record latency for each pipeline stage (default: False).

        Returns
        -------
        dict
            Dictionary containing:
            - status (str): "ok" if successful, or "no_text" if OCR detected nothing.
            - translation_text (str): Concatenated translated text from all paragraphs.
            - audio_file (BytesIO): Combined MP3 buffer.
            - bbox_results (dict): OCR bounding box metadata.
            - latency (dict): Time spent in each stage (OCR, TTS, total).
        """

        start_time = time.time()

        # Run ocr
        paragraphs, bbox_results = self.ocr.run_ocr(image_path)
        ocr_end_time = time.time()

        if not paragraphs:
            return {"status": "no_text"}

        stem = Path(image_path).stem

        # Ansync TTS
        async def tts_pipeline(paragraphs: List[str]):
            tasks = []
            for i, para in enumerate(paragraphs, 1):
                page_dict = {"fileName": f"{stem}_para{i}.png", "text": para}
                tasks.append(self.story.process_page(i, page_dict, target_lang, log_csv, check_latency))
            return await asyncio.gather(*tasks)

        tts_results = asyncio.run(tts_pipeline(paragraphs))
        tts_end_time = time.time()
        
        # Join translated texts
        translations = [r["translation_text"] for r in tts_results]
        translated_text = " ".join(translations)
        
        # Concat audio files and temporary save in audio buffer
        audio_buffer = self.concat_paragraph_mp3s(self.story.OUT_DIR, stem, len(paragraphs))

        total_latency = round(time.time() - start_time, 3)
        ocr_latency = round(ocr_end_time - start_time, 3)
        tts_latency = round(tts_end_time - ocr_end_time, 3)

        # return tranlated text, audio buffer, bbox information
        return {
            "status": "ok",
            "translation_text": translated_text,
            "audio_file": audio_buffer,
            "bbox_results": bbox_results,
            "latency": {
                "ocr_sec": ocr_latency,
                "tts_sec": tts_latency,
                "total_sec": total_latency
            }
        }
        
