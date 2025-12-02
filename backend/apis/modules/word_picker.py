#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import time
import re
from pathlib import Path
from typing import Dict, List, Any

from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate
from pydantic import BaseModel, Field
from dotenv import load_dotenv

load_dotenv()


WORD_PICKER_PROMPT = """
You are a warm and friendly children's vocabulary curator.

You will receive a short passage from a children's story written in {target_lang}.
Your job is to extract EXACTLY THREE important vocabulary words from the passage
and provide a simple Korean meaning for each word.

Guidelines:
- Choose meaningful words that carry story importance.
- If there seem to be fewer than three obvious candidates, still select the best three possible.
- Ignore very basic words like "good", "big", "dog", etc.
- Words must appear in the passage.
- Use simple, age-appropriate Korean meanings.

Return exactly three items and follow the structured output strictly.
"""



# -------------------------------------------------------------------------
# Pydantic Models
# -------------------------------------------------------------------------

class VocabItem(BaseModel):
    word: str = Field(..., description="Single vocabulary word.")
    meaning_ko: str = Field(..., description="Short Korean meaning.")


class VocabResult(BaseModel):
    items: List[VocabItem] = Field(
        ..., 
        min_items=3,
        max_items=3,
        description="Exactly 3 vocabulary items."
    )


# -------------------------------------------------------------------------
# StoryWordPicker Module
# -------------------------------------------------------------------------

class StoryWordPicker:
    """
    Extract up to 3 important vocabulary words from children's story text,
    with simple Korean meanings.
    """

    def __init__(self, target_lang: str = "English"):
        self.target_lang = target_lang
        self.llm = ChatOpenAI(model="gpt-4o-mini", temperature=0.2)
        self.word_chain = self._create_word_chain()

    def _create_word_chain(self):
        prompt = ChatPromptTemplate.from_messages(
            [
                ("system", WORD_PICKER_PROMPT),
                ("user", "TargetLang: {target_lang}\n\nText:\n{story_text}"),
            ]
        )
        return prompt | self.llm.with_structured_output(VocabResult)

    def pick_words(self, story_text: str) -> Dict[str, Any]:
        """
        Extract vocabulary words from story text (sync version).

        Returns:
            {
                "status": "ok" | "no_words" | "failed",
                "items": [...],
                "latency": float
            }
        """

        story_text = (story_text or "").strip()
        if not story_text:
            return {"status": "no_words", "items": [], "latency": 0.0}

        for attempt in range(3):
            try:
                t0 = time.time()

                response: VocabResult = self.word_chain.invoke(
                    {
                        "story_text": story_text,
                        "target_lang": self.target_lang,
                    }
                )

                latency = round(time.time() - t0, 3)

                items = response.items
                if not items:
                    return {"status": "no_words", "items": [], "latency": latency}

                cleaned = []
                seen = set()

                for item in items:
                    w = item.word.strip()
                    if not w:
                        continue
                    if w not in seen:
                        seen.add(w)
                        cleaned.append(
                            {
                                "word": w,
                                "meaning_ko": item.meaning_ko.strip()
                            }
                        )

                if not cleaned:
                    return {"status": "no_words", "items": [], "latency": latency}

                return {
                    "status": "ok",
                    "items": cleaned[:3],
                    "latency": latency
                }

            except Exception as e:
                print(f"[WordPicker] attempt {attempt+1} failed: {e}")
                if attempt == 2:
                    return {"status": "failed", "items": [], "latency": -1.0}

        return {"status": "failed", "items": [], "latency": -1.0}
    