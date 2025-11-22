import re
from typing import List, Dict, Tuple

SUPPORTED_LANGS = ["en", "zh", "vi"]
PROFANITY_DICT: Dict[str, List[str]] = {}

def load_profanity_lists(base_path=None):
    import os
    global PROFANITY_DICT
    if base_path is None:
        base_path = os.path.join(os.path.dirname(__file__), "../../media/profanity")
    
    for lang in SUPPORTED_LANGS:
        try:
            path = os.path.join(base_path, f"{lang}.txt")
            with open(path, "r", encoding="utf-8") as f:
                PROFANITY_DICT[lang] = [line.strip().lower() for line in f if line.strip()]
        except FileNotFoundError:
            PROFANITY_DICT[lang] = []
            print(f"Warning: {lang}.txt not found, empty list loaded.")

def is_clean(text: str, lang: str) -> Tuple[bool, List[str]]:
    """
    text에 profanity가 하나라도 있으면 False, 존재한 단어 리스트도 반환
    """
    if lang not in SUPPORTED_LANGS:
        raise ValueError(f"Unsupported language code: {lang}")

    profanity_list = PROFANITY_DICT.get(lang, [])
    text_lower = text.lower()
    found_words = []

    for word in profanity_list:
        pattern = r'\b' + re.escape(word) + r'\b'
        if re.search(pattern, text_lower) or word in text_lower:
            found_words.append(word)

    is_text_clean = len(found_words) == 0
    return is_text_clean, found_words
