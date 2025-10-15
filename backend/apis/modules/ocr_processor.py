import os
import requests
import uuid
import time
import json
import numpy as np
from pathlib import Path
from typing import List, Dict, Any
from sklearn.cluster import DBSCAN 
from dotenv import load_dotenv
load_dotenv()

class OCRModule:
    def __init__(self, conf_threshold: float = 0.8):
        self.api_url = os.getenv("OCR_API_URL")
        self.secret_key = os.getenv("OCR_SECRET")
        self.conf_threshold = conf_threshold

    def _filter_low_confidence(self, result_json: Dict[str, Any]) -> Dict[str, Any]:
        images = result_json.get("images", [])
        if not images:
            return result_json

        fields = images[0].get("fields", [])
        threshold = getattr(self, "conf_threshold")
        filtered_fields = []
        for f in fields:
            c = f.get("inferConfidence", None)
            if c is None or c > threshold:
                filtered_fields.append(f)

        new_json = dict(result_json)
        new_images = list(images)
        new_first = dict(new_images[0])
        new_first["fields"] = filtered_fields
        new_images[0] = new_first
        new_json["images"] = new_images
        return new_json # return shallow copied result

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

        # 1) Filter out low-confidence fields before computing font size or tokens
        filtered_json = self._filter_low_confidence(result_json)

        # 2) Compute font size from filtered fields
        fs = self._font_size(filtered_json)

        images_f = filtered_json.get("images", [])
        if not images_f:
            return []
        fields = images_f[0].get("fields", [])

        # 3) Build tokens from filtered fields
        tokens = []
        for field in fields:
            text = field.get("inferText", "")
            vertices = field.get("boundingPoly", {}).get("vertices", [])
            xs = [v.get("x", 0.0) for v in vertices]
            ys = [v.get("y", 0.0) for v in vertices]
            if xs and ys:
                tokens.append(
                    {
                        "text": text,
                        "x": float(np.mean(xs)),
                        "y": float(np.mean(ys)),
                    }
                )

        if not tokens:
            return []

        # 4) Paragraph clustering (2D DBSCAN on x, y)
        X = np.array([[t["x"], t["y"]] for t in tokens])
        para_eps = max(fs * 6.0, 15.0)
        para_db = DBSCAN(eps=para_eps, min_samples=2)
        para_labels = para_db.fit_predict(X)

        for t, lbl in zip(tokens, para_labels):
            t["para_label"] = int(lbl)

        paragraphs: List[List[Dict[str, Any]]] = []
        for lbl in sorted(set(para_labels)):
            if lbl == -1:
                continue  # skip noise cluster
            paragraph_tokens = [t for t in tokens if t["para_label"] == lbl]
            paragraphs.append(paragraph_tokens)

        results: List[str] = []

        # 5) For each paragraph, cluster lines by Y and sort words by X
        for para in paragraphs:
            if not para:
                continue
            # Cluster by Y within paragraph (DBSCAN)
            Y = np.array([[t["y"]] for t in para])
            line_eps = max(fs * 0.5, 2.0)
            line_db = DBSCAN(eps=line_eps, min_samples=1)
            line_labels = line_db.fit_predict(Y)
            for t, lbl in zip(para, line_labels):
                t["line_label"] = int(lbl)

            # Build line-level text
            lines = []
            for lbl in sorted(set(line_labels)):
                line_tokens = [t for t in para if t["line_label"] == lbl]
                line_tokens.sort(key=lambda t: t["x"])
                line_text = " ".join(t["text"] for t in line_tokens)
                y_mean = np.mean([t["y"] for t in line_tokens])
                lines.append({"text": line_text, "y": y_mean})

            # Sort lines vertically and join into paragraph text
            lines_sorted = sorted(lines, key=lambda l: l["y"])
            paragraph_text = "\n".join(l["text"] for l in lines_sorted)
            results.append(paragraph_text.strip())

        return results

    def process_page(self, image_path: str) -> List[str]:
        request_json = {
            "images": [{"format": "png", "name": Path(image_path).stem}],
            "requestId": str(uuid.uuid4()),
            "version": "V2",
            "timestamp": int(round(time.time() * 1000)),
        }

        headers = {"X-OCR-SECRET": self.secret_key}
        files = {
            "file": open(image_path, "rb"),
            "message": (None, json.dumps(request_json), "application/json"),
        }
        response = requests.post(self.api_url, headers=headers, files=files)
        result = response.json()
        paragraphs = self._parse_infer_text(result)

        return paragraphs

# # --- Prompt Templates ---

# TRANSLATION_PROMPT = """
# You are an expert multilingual children's-story adapter.
# You will be given a block of Korean text that may contain up to three parts: [PREVIOUS], [CURRENT], and [NEXT].
# Your task is to translate ONLY the [CURRENT] Korean sentence into a single, fluent {target_lang} sentence.
# Use the [PREVIOUS] and [NEXT] sentences for context to ensure pronouns, flow, and style are correct.
# """

# class Translation(BaseModel):
#     """A single, fluent translation of a Korean sentence."""
#     translated_text: str = Field(..., description="The translated sentence in the target language.", alias="translation")

# class TranslationProcessor:
#     def __init__(self, out_dir="out_audio", log_dir="log", target_lang: str = "English"):
#         self.client = AsyncOpenAI()
#         self.TTS_MODEL = "gpt-4o-mini-tts"
#         self.OUT_DIR = Path(out_dir)
#         self.LOG_DIR = Path(log_dir)
#         self.CSV_LOG = self.LOG_DIR / "sentence_log.csv"
        
#         self.target_lang = target_lang
#         self.llm = ChatOpenAI(model="gpt-4o-mini", temperature=0.7)
#         self.translation_chain = self._create_translation_chain()
#         self.sentiment_chain = self._create_sentiment_chain()

#         # Reset dirs each run
#         if self.OUT_DIR.exists(): shutil.rmtree(self.OUT_DIR)
#         if self.LOG_DIR.exists(): shutil.rmtree(self.LOG_DIR)
#         self.OUT_DIR.mkdir(parents=True)
#         self.LOG_DIR.mkdir(parents=True)

#     def _create_translation_chain(self):
#         prompt = ChatPromptTemplate.from_messages([
#             ("system", TRANSLATION_PROMPT),
#             ("user", "{text_with_context}")
#         ])
#         return prompt | self.llm.with_structured_output(Translation)

#     def _create_sentiment_chain(self):
#         prompt = ChatPromptTemplate.from_messages([
#             ("system", SENTIMENT_PROMPT),
#             ("user", "{korean_text}")
#         ])
#         return prompt | self.llm.with_structured_output(Sentiment)
    
#     async def translate(self, text_with_context: str) -> Dict[str, Any]:
#         for attempt in range(3):
#             try:
#                 t0 = time.time()
#                 response = await self.translation_chain.ainvoke({
#                     "text_with_context": text_with_context,
#                     "target_lang": self.target_lang
#                 })
#                 latency = time.time() - t0
#                 return {"result": response, "latency": round(latency, 3)}
#             except Exception as e:
#                 print(f"Translation attempt {attempt + 1} failed: {e}")
#                 if attempt == 2:
#                     return {"result": e, "latency": -1.0}
#                 await asyncio.sleep(0.7)
#         return {"result": None, "latency": -1.0}