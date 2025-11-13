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
        self.api_url = os.getenv("OCR_API_URL", "")
        self.secret_key = os.getenv("OCR_SECRET", "")
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

        print(
            f"[DEBUG] Confidence filter: {len(fields)} -> {len(filtered_fields)} fields kept (threshold={threshold})"
        )

        new_json = dict(result_json)
        new_images = list(images)
        new_first = dict(new_images[0])
        new_first["fields"] = filtered_fields
        new_images[0] = new_first
        new_json["images"] = new_images
        return new_json  # return shallow copied result

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

        # 1) Filter out low-confidence fields before computing
        # font size or tokens
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
                        "xs": xs,
                        "ys": ys,
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

        # 5) For each paragraph, cluster lines by Y and sort words by X.
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
        # 디버깅용...
        print(f"[DEBUG] Sending OCR request for {image_path}")
        start = time.time()
        response = requests.post(self.api_url, headers=headers, files=files)
        # 디버깅용
        print(f"[DEBUG] OCR API call took {time.time() - start:.2f}s")
        print(f"[DEBUG] OCR raw response text (first 300 chars): {response.text[:300]}")
        result = response.json()
        paragraphs = self._parse_infer_text(result)

        return paragraphs

    def process_cover_page(self, image_path: str) -> str:  # Or Optional[str]
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
        fs = self._font_size(filtered_json)
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
                        "x": float(np.mean(xs)),
                        "y": float(np.mean(ys)),
                        "xs": xs,
                        "ys": ys,
                    }
                )
        if not tokens:
            print(f"[DEBUG] OCR parse: no tokens extracted. Field count: {len(fields)}")
            return []

        heights = [max(t["ys"]) - min(t["ys"]) for t in tokens if t["ys"]]
        max_height = max(heights) if heights else 0
        tokens = [
            t for t in tokens if (max(t["ys"]) - min(t["ys"])) >= 0.33 * max_height
        ]

        if not tokens:
            print(f"[DEBUG] OCR parse: no tokens extracted. Field count: {len(fields)}")
            return []

        # Paragraph clustering (2D DBSCAN on x, y)
        X = np.array([[t["x"], t["y"]] for t in tokens])
        para_eps = max(fs * 6.0, 15.0)
        para_db = DBSCAN(eps=para_eps, min_samples=1)
        para_labels = para_db.fit_predict(X)

        for t, lbl in zip(tokens, para_labels):
            t["para_label"] = int(lbl)

        paragraphs: List[List[Dict[str, Any]]] = []
        for lbl in sorted(set(para_labels)):
            if lbl == -1:
                continue  # skip noise cluster
            paragraph_tokens = [t for t in tokens if t["para_label"] == lbl]
            paragraphs.append(paragraph_tokens)

        results: List[Dict[str, Any]] = []

        # For each paragraph, cluster lines by Y and sort words by X, build paragraph text
        for para in paragraphs:
            if not para:
                continue
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

            # Calculate height for sorting
            all_ys = []
            for l in lines_sorted:
                all_ys.extend(l["ys"])
            height = max(all_ys) - min(all_ys) if all_ys else 0

            results.append({"text": paragraph_text.strip(), "height": height})

        # Sort paragraphs by height (largest first)
        results.sort(key=lambda r: r["height"], reverse=True)

        # Return just the largest text as a string, or None if no results
        return results[0]["text"] if results else None
