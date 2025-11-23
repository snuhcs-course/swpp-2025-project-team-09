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


# ---------------------------------------------------------
# Translation Prompt (TTSModule과 일치)
# ---------------------------------------------------------
TRANSLATION_PROMPT = """
You are an expert adapter of multilingual children's stories.
You will receive a block of Korean text that may contain up to three parts:
[PREVIOUS], [CURRENT], and [NEXT].

Your task is to translate ONLY the [CURRENT] Korean sentence into a single,
gentle, child-friendly {target_lang} sentence.

Use the [PREVIOUS] and [NEXT] sentences only for context, so that pronouns,
tone, and flow stay natural.

While translating:
- Use simple, clear, kind words that children can easily understand.
- Avoid any harmful, scary, or age-inappropriate expressions.
- Keep the style warm, friendly, and suitable for young readers.
"""


# ---------------------------------------------------------
# Pydantic Model (TTSModule과 동일)
# ---------------------------------------------------------
class Translation(BaseModel):
    translated_text: str = Field(..., alias="translation")


# ---------------------------------------------------------
# 평가 모듈 클래스
# ---------------------------------------------------------
class EvaluationTranslator:
    def __init__(self, target_lang="English"):
        self.target_lang = target_lang
        self.llm = ChatOpenAI(model="gpt-4o-mini", temperature=0.7)
        self.translation_chain = self._build_chain()

    def _build_chain(self):
        prompt = ChatPromptTemplate.from_messages(
            [
                ("system", TRANSLATION_PROMPT),
                ("user", "{text_with_context}")
            ]
        )
        return prompt | self.llm.with_structured_output(Translation)

    async def translate_page(self, sentences: List[str]) -> List[str]:
        tasks = [
            self._translate_with_context(sentences, i)
            for i in range(len(sentences))
        ]
        return await asyncio.gather(*tasks)

    async def _translate_with_context(self, sentences: List[str], idx: int) -> str:
        blocks = []

        if idx > 0:
            blocks.append(f"[PREVIOUS]: {sentences[idx - 1]}")
        blocks.append(f"[CURRENT]: {sentences[idx]}")
        if idx < len(sentences) - 1:
            blocks.append(f"[NEXT]: {sentences[idx + 1]}")

        context_prompt = "\n".join(blocks)

        response = await self.translation_chain.ainvoke(
            {
                "text_with_context": context_prompt,
                "target_lang": self.target_lang
            }
        )
        return response.translated_text.strip()



# ---------------------------------------------------------
# BERTScore 평가
# ---------------------------------------------------------
def evaluate_bertscore(candidates: List[str], references: List[str], lang="en"):
    P, R, F1 = bert_score(
        candidates,
        references,
        lang=lang,
        rescale_with_baseline=True
    )
    return {
        "precision": P.tolist(),
        "recall": R.tolist(),
        "f1": F1.tolist(),
        "precision_mean": float(P.mean().item()),
        "recall_mean": float(R.mean().item()),
        "f1_mean": float(F1.mean().item()),
    }


# ---------------------------------------------------------
# 전체 파이프라인(번역 + 평가)
# ---------------------------------------------------------
async def evaluate_translations(pages_json: Dict[str, Any]) -> Dict[str, Any]:
    translator = EvaluationTranslator()

    candidates = []
    references = []

    for page in pages_json["pages"]:
        ko_sentences = [p["ko"] for p in page["pairs"]]
        en_sentences = [p["en"] for p in page["pairs"]]

        translated = await translator.translate_page(ko_sentences)

        candidates.extend(translated)
        references.extend(en_sentences)

    bert = evaluate_bertscore(candidates, references)

    return {
        "translations": candidates,
        "bert": bert,
        "summary": {
            "avg_f1": bert["f1_mean"],
            "avg_precision": bert["precision_mean"],
            "avg_recall": bert["recall_mean"]
        }
    }



import json
import asyncio
from evaluation import evaluate_translations  

async def run_eval():
    with open("data.json", "r", encoding="utf-8") as f:
        data = json.load(f)

    result = await evaluate_translations(data)

    print("Avg F1:", result["summary"]["avg_f1"])
    print("Translations:", result["translations"])

if __name__ == "__main__":
    asyncio.run(run_eval())


