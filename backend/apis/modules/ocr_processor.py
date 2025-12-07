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
    """
    OCR processing module using Naver Clova OCR API
    - Extract text from images
    - Group text into paragraphs and lines
    - Calculate bounding boxes
    """

    def __init__(self, conf_threshold: float = 0.75):
        self.api_url = os.getenv("OCR_API_URL", "")
        self.secret_key = os.getenv("OCR_SECRET", "")
        self.conf_threshold = conf_threshold

    def _filter_low_confidence(self, result_json: Dict[str, Any]) -> Dict[str, Any]:
        """Filter out OCR results below confidence threshold"""
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

        print(
            f"[DEBUG] Confidence filter: {len(fields)} -> {len(filtered_fields)} fields kept (threshold={threshold})"
        )

        new_json = dict(result_json)
        new_images = list(images)
        new_first = dict(new_images[0])
        new_first["fields"] = filtered_fields
        new_images[0] = new_first
        new_json["images"] = new_images
        return new_json

    def _font_size(self, result_json: Dict[str, Any]) -> float:
        """Calculate average font size from bounding box heights"""
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
        """
        Parse OCR results into structured paragraphs with bounding boxes

        Process:
        1. Filter low-confidence results
        2. Calculate average font size
        3. Extract tokens (text + coordinates)
        4. Cluster into paragraphs (2D DBSCAN)
        5. Cluster into lines within paragraphs (1D DBSCAN on Y)
        6. Sort words by X, lines by Y

        Returns:
            List of {"text": str, "bbox": dict}
        """
        # Filter low-confidence fields
        filtered_json = self._filter_low_confidence(result_json)

        # Calculate font size from filtered fields
        fs = self._font_size(filtered_json)

        images_f = filtered_json.get("images", [])
        if not images_f:
            return []
        fields = images_f[0].get("fields", [])

        # Build tokens from filtered fields
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
                        "xs": xs,
                        "ys": ys,
                    }
                )

        if not tokens:
            return []

        # Paragraph clustering (2D DBSCAN on x, y)
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

        # For each paragraph, cluster lines by Y and sort words by X
        for para in paragraphs:
            if not para:
                continue
            # Cluster by Y within paragraph (DBSCAN)
            Y = np.array([[t["y"]] for t in para])
            line_eps = fs * 0.25
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
                xs = []
                ys = []
                for tkn in line_tokens:
                    xs.extend(tkn["xs"])
                    ys.extend(tkn["ys"])
                lines.append({"text": line_text, "y": y_mean, "xs": xs, "ys": ys})

            # Sort lines vertically and join into paragraph text
            lines_sorted = sorted(lines, key=lambda line: line["y"])
            paragraph_text = "\n".join(line["text"] for line in lines_sorted)

            # Calculate bbox from all vertices in paragraph
            xs = []
            ys = []
            for line in lines_sorted:
                xs.extend(line["xs"])
                ys.extend(line["ys"])

            x_min, x_max = float(min(xs)), float(max(xs))
            y_min, y_max = float(min(ys)), float(max(ys))
            bbox = {
                "x1": x_min,
                "y1": y_min,
                "x2": x_max,
                "y2": y_min,
                "x3": x_max,
                "y3": y_max,
                "x4": x_min,
                "y4": y_max,
            }
            results.append({"text": paragraph_text.strip(), "bbox": bbox})
        return results

    def process_page(self, image_path: str) -> List[str]:
        """
        Process a regular page image with OCR

        Args:
            image_path: Path to image file

        Returns:
            List of paragraphs with text and bounding boxes
        """

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

        print(f"[DEBUG] Sending OCR request for {image_path}")
        start = time.time()
        response = requests.post(self.api_url, headers=headers, files=files)
        print(f"[DEBUG] OCR API call took {time.time() - start:.2f}s")
        print(f"[DEBUG] OCR raw response text (first 300 chars): {response.text[:300]}")

        result = response.json()
        paragraphs = self._parse_infer_text(result)
        print(f"[DEBUG] paragraphs: {paragraphs}")
        return paragraphs

    def process_cover_page(self, image_path: str) -> str:
        """
        Process a cover page image to extract title
        Uses height-based filtering to find the largest/title text

        Args:
            image_path: Path to cover image file

        Returns:
            Extracted title text (largest text block)
        """

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
        print(f"[DEBUG] Sending OCR request for {image_path} (cover mode)")
        start = time.time()
        response = requests.post(self.api_url, headers=headers, files=files)
        print(f"[DEBUG] OCR API call took {time.time() - start:.2f}s")
        result = response.json()

        filtered_json = self._filter_low_confidence(result)
        images_f = filtered_json.get("images", [])
        if not images_f:
            print("[DEBUG] OCR parse: no images field in response.")
            return []

        fields = images_f[0].get("fields", [])

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
                        "field": field,
                        "x": float(np.mean(xs)),
                        "y": float(np.mean(ys)),
                        "xs": xs,
                        "ys": ys,
                    }
                )

        if not tokens:
            print(f"[DEBUG] OCR parse: no tokens extracted. Field count: {len(fields)}")
            return []

        # Filter by height (keep only large text, likely titles)
        heights = [max(t["ys"]) - min(t["ys"]) for t in tokens]
        max_height = max(heights)

        filtered_tokens = [
            t for t in tokens if (max(t["ys"]) - min(t["ys"])) >= 0.33 * max_height
        ]

        if not filtered_tokens:
            print(f"[DEBUG] OCR parse: no tokens extracted after height filtering.")
            return []

        filtered_fields = [t["field"] for t in filtered_tokens]

        filtered_json_for_fs = {"images": [{"fields": filtered_fields}]}

        fs = self._font_size(filtered_json_for_fs)

        tokens = filtered_tokens

        # Paragraph clustering
        X = np.array([[t["x"], t["y"]] for t in tokens])
        para_eps = max(fs * 6.0, 15.0)
        para_db = DBSCAN(eps=para_eps, min_samples=1)
        para_labels = para_db.fit_predict(X)

        for t, lbl in zip(tokens, para_labels):
            t["para_label"] = int(lbl)

        paragraphs: List[List[Dict[str, Any]]] = []
        for lbl in sorted(set(para_labels)):
            if lbl == -1:
                continue
            paragraph_tokens = [t for t in tokens if t["para_label"] == lbl]
            paragraphs.append(paragraph_tokens)

        results: List[Dict[str, Any]] = []

        for para in paragraphs:
            if not para:
                continue

            # Line clustering
            Y = np.array([[t["y"]] for t in para])
            line_eps = max(fs * 0.5, 2.0)
            line_db = DBSCAN(eps=line_eps, min_samples=1)
            line_labels = line_db.fit_predict(Y)
            for t, lbl in zip(para, line_labels):
                t["line_label"] = int(lbl)

            lines = []
            for lbl in sorted(set(line_labels)):
                line_tokens = [t for t in para if t["line_label"] == lbl]
                line_tokens.sort(key=lambda t: t["x"])
                line_text = " ".join(t["text"] for t in line_tokens)
                y_mean = np.mean([t["y"] for t in line_tokens])
                ys = []
                for tkn in line_tokens:
                    ys.extend(tkn["ys"])
                lines.append({"text": line_text, "y": y_mean, "ys": ys})

            lines_sorted = sorted(lines, key=lambda l: l["y"])
            paragraph_text = "\n".join(l["text"] for l in lines_sorted)

            all_ys = []
            for l in lines_sorted:
                all_ys.extend(l["ys"])
            height = max(all_ys) - min(all_ys) if all_ys else 0

            results.append({"text": paragraph_text.strip(), "height": height})

        # Return the tallest text block (likely the title)
        results.sort(key=lambda r: r["height"], reverse=True)
        return results[0]["text"] if results else None
