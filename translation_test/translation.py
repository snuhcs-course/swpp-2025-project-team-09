# evaluation.py
# -*- coding: utf-8 -*-

"""
evaluation.py
TTSModule의 번역 기능을 그대로 재현해 동일한 방식으로 번역하고,
그 결과를 BERTScore로 자동 평가하는 전용 모듈.
"""

import time
import asyncio
from typing import List, Dict, Any
from bert_score import score as bert_score
from dotenv import load_dotenv
load_dotenv()

from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate
from pydantic import BaseModel, Field
import json

# ---------------------------------------------------------
# Translation Prompt (TTSModule과 일치)
# ---------------------------------------------------------
TRANSLATION_PROMPT_A = """
You are an expert adapter of multilingual children's stories.
You will receive a block of Korean text that may contain up to three parts:
[PREVIOUS], [CURRENT], and [NEXT].

Your task is to translate ONLY the [CURRENT] Korean sentence into a single,
gentle, child-friendly {target_lang} sentence.

Use the [PREVIOUS] and [NEXT] sentences only for contextual reference so that
pronouns, tone, and overall flow stay natural. Preserve all semantic information
from the original Korean sentence and avoid adding or removing meaning. Maintain
the original structure where possible, adjusting only enough to produce natural
grammar in {target_lang}.

While translating:
- Use simple, clear, kind words suitable for young readers.
- Keep a warm and friendly tone without altering factual details.
- Avoid any harmful or age-inappropriate expressions.
- Do not summarize, expand, embellish, or reinterpret the original content.
- Your output must be exactly one sentence, with no explanations or extra text.
"""

TRANSLATION_PROMPT_B = """
You are an expert multilingual children's-story adapter.
You will be given a block of Korean text that may contain up to three
parts: [PREVIOUS], [CURRENT], and [NEXT].
Your task is to translate ONLY the [CURRENT] Korean sentence into a
single, fluent {target_lang} sentence.
Use the [PREVIOUS] and [NEXT] sentences for context to ensure pronouns,
flow, and style are correct.
"""

class Translation(BaseModel):
    translated_text: str = Field(..., alias="translation")


class Translator:
    def __init__(self, prompt: str, target_lang="English"):
        self.prompt = prompt
        self.target_lang = target_lang
        self.llm = ChatOpenAI(model="gpt-4o-mini", temperature=0.4)
        self.chain = self._build_chain()

    def _build_chain(self):
        prompt = ChatPromptTemplate.from_messages(
            [
                ("system", self.prompt),
                ("user", "{text_with_context}")
            ]
        )
        return prompt | self.llm.with_structured_output(Translation)

    async def translate_sentences(self, sentences: List[str]) -> List[str]:
        tasks = [
            self.translate_with_context(sentences, i)
            for i in range(len(sentences))
        ]
        return await asyncio.gather(*tasks)

    async def translate_with_context(self, sentences: List[str], idx: int) -> str:
        blocks = []

        if idx > 0:
            blocks.append(f"[PREVIOUS]: {sentences[idx - 1]}")

        blocks.append(f"[CURRENT]: {sentences[idx]}")

        if idx < len(sentences) - 1:
            blocks.append(f"[NEXT]: {sentences[idx + 1]}")

        context_prompt = "\n".join(blocks)

        response = await self.chain.ainvoke(
            {
                "text_with_context": context_prompt,
                "target_lang": self.target_lang
            }
        )
        return response.translated_text.strip()


async def translate_json(pages_json: Dict[str, Any], prompt: str) -> Dict[str, Any]:
    translator = Translator(prompt=prompt)

    new_json = {
        "title": pages_json["title"],
        "pages": []
    }

    for page in pages_json["pages"]:
        ko_sentences = page["sentences"]

        en_sentences = await translator.translate_sentences(ko_sentences)

        new_json["pages"].append(
            {
                "fileName": page["fileName"],
                "sentences": ko_sentences,
                "translations": en_sentences
            }
        )

    return new_json


async def run_translation():
    with open("data.json", "r", encoding="utf-8") as f:
        data = json.load(f)

    new_data_A = await translate_json(data, prompt=TRANSLATION_PROMPT_A)
    with open("translated_A.json", "w", encoding="utf-8") as f:
        json.dump(new_data_A, f, ensure_ascii=False, indent=2)

    new_data_B = await translate_json(data, prompt=TRANSLATION_PROMPT_B)
    with open("translated_B.json", "w", encoding="utf-8") as f:
        json.dump(new_data_B, f, ensure_ascii=False, indent=2)


if __name__ == "__main__":
    asyncio.run(run_translation())