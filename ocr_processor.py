import requests
import uuid
import time
import json
from pathlib import Path
from typing import List, Dict, Any


class OCRProcessor:
    def __init__(self, api_url: str, secret_key: str):
        self.api_url = api_url
        self.secret_key = secret_key

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

    def run_ocr(self, image_path: str) -> List[str]:
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

        return self._parse_infer_text(result)