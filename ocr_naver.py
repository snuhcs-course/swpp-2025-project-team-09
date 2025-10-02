import requests
import uuid
import time
import json

def font_size(result_json: dict) -> float:
    # get average bbox height as font size
    images = result_json.get("images", [])
    if not images:
        return 0.0

    fields = images[0].get("fields", [])
    heights = []

    for field in fields:
        vertices = field.get("boundingPoly", {}).get("vertices", [])
        ys = [v.get("y", 0.0) for v in vertices]
        height = max(ys) - min(ys)
        heights.append(height)

    if not heights:
        return 0.0

    return sum(heights) / len(heights)

def parse_infer_text(result_json: dict):
    # parse OCR result into paragraphs using bbox position and avg font size
    avg_font_size = font_size(result_json)
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

        if prev_y > 0 and (abs(avg_y - prev_y) > avg_font_size*3 or abs(avg_x - prev_x) > avg_font_size*25):
            if current_paragraph:
                paragraphs.append(" ".join(current_paragraph).strip())
                current_paragraph = []

        current_paragraph.append(text)

        prev_y = avg_y
        prev_x = avg_x

    if current_paragraph:
        paragraphs.append(" ".join(current_paragraph).strip())

    return paragraphs

api_url = "" # Replace with your OCR API URL 
secret_key = "" # Replace with your OCR Secret Key

image_file = "014.jpeg" # Replace with your image file path


request_json = {
    'images': [
        {'format': 'png', 'name': 'demo'}
    ],
    'requestId': str(uuid.uuid4()),
    'version': 'V2',
    'timestamp': int(round(time.time() * 1000))
}

headers = {'X-OCR-SECRET': secret_key}

files = {
    'file': open(image_file, 'rb'),
    'message': (None, json.dumps(request_json), 'application/json')
}

response = requests.post(api_url, headers=headers, files=files)



result = response.json()

parsed_paragraphs = parse_infer_text(result)

for i, p in enumerate(parsed_paragraphs, 1):
    print(f"[Paragraph {i}]")
    print(p)
