import os
import requests
import uuid
import time
import json
from pathlib import Path
from typing import List, Dict, Any

class OCRProcessor:
    def __init__(self, api_url: str = None, secret_key: str = None):
        self.api_url = api_url or os.getenv("OCR_API_URL")
        self.secret_key = secret_key or os.getenv("OCR_SECRET_KEY")

        if not self.api_url or not self.secret_key:
            raise ValueError("OCRProcessor: Missing OCR_API_URL or OCR_SECRET_KEY environment variables")

    def _font_size(self, result_json: Dict[str, Any]) -> float:
        images = result_json.get("images", [])
        if not images:
            return 0.0

        fields = images[0].get("fields", [])
        heights = []

        for field in fields:
            vertices = field.get("boundingPoly", {}).get("vertices", [])
            ys = [v.get("y", 0.0) for v in vertices]
            if ys:
                height = max(ys) - min(ys)
                heights.append(height)

        return sum(heights) / len(heights) if heights else 0.0

    def _parse_infer_text(self, result_json: Dict[str, Any]) -> List[str]:
        avg_font_size = self._font_size(result_json)
        images = result_json.get("images", [])
        if not images:
            return []

        fields = images[0].get("fields", [])
        paragraphs = []
        current_paragraph = []

        prev_y = -1.0
        prev_x = -1.0

        for field in fields:
            text = field.get("inferText", "")
            vertices = field.get("boundingPoly", {}).get("vertices", [])

            ys = [v.get("y", 0.0) for v in vertices]
            xs = [v.get("x", 0.0) for v in vertices]

            avg_y = sum(ys) / len(ys) if ys else 0.0
            avg_x = sum(xs) / len(xs) if xs else 0.0

            if prev_y > 0 and (
                False
                # TODO: implement better paragraph detection logic (maybe K-means clustering?)
                
                # abs(avg_y - prev_y) > avg_font_size * 3
                # or abs(avg_x - prev_x) > avg_font_size * 25
            ):
                if current_paragraph:
                    paragraphs.append(" ".join(current_paragraph).strip())
                    current_paragraph = []

            current_paragraph.append(text)
            prev_y = avg_y
            prev_x = avg_x

        if current_paragraph:
            paragraphs.append(" ".join(current_paragraph).strip())

        return paragraphs

    def run_ocr(self, image_path: str) -> tuple[list[str], dict]:
        request_json = {
            "images": [{"format": "png", "name": Path(image_path).stem}],
            "requestId": str(uuid.uuid4()),
            "version": "V2",
            "timestamp": int(round(time.time() * 1000)),
        }

        headers = {"X-OCR-SECRET": self.secret_key}
        with open(image_path, "rb") as img_file:
            files = {
                "file": (Path(image_path).name, img_file, "application/octet-stream"),
                "message": (None, json.dumps(request_json), "application/json"),
            }
            response = requests.post(self.api_url, headers=headers, files=files)
            result = response.json()

        paragraphs = self._parse_infer_text(result)
        return paragraphs, result

# --- Prompt Templates ---

TRANSLATION_PROMPT = """
You are an expert multilingual children's-story adapter.
You will be given a block of Korean text that may contain up to three parts: [PREVIOUS], [CURRENT], and [NEXT].
Your task is to translate ONLY the [CURRENT] Korean sentence into a single, fluent {target_lang} sentence.
Use the [PREVIOUS] and [NEXT] sentences for context to ensure pronouns, flow, and style are correct.
"""

class Translation(BaseModel):
    """A single, fluent translation of a Korean sentence."""
    translated_text: str = Field(..., description="The translated sentence in the target language.", alias="translation")

class TranslationProcessor:
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