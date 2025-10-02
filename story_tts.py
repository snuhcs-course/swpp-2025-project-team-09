#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import csv
import time
import json
import asyncio
import argparse
import sys
import kss
import shutil
from pathlib import Path
from typing import Dict, List, Any
from openai import AsyncOpenAI

TRANSLATION_SYSTEM_PROMPT_TEMPLATE = """You are an expert multilingual children's-story adapter.
You will be given a block of Korean text that may contain up to three parts: [PREVIOUS], [CURRENT], and [NEXT].
Your task is to translate ONLY the [CURRENT] Korean sentence into a single, fluent {target_lang} sentence.
Use the [PREVIOUS] and [NEXT] sentences for context to ensure pronouns, flow, and style are correct.
- Use standard {target_lang} double quotes (“ ”) for dialogue if needed.
- Your entire response must be ONLY the valid JSON for the translated sentence: {{"{target_lang}_translation": "..."}}
"""

SENTIMENT_SYSTEM_PROMPT = """
You are an expert voice director for children's audiobooks. Your task is to analyze the sentiment of a single KOREAN sentence and provide performance direction for a voice actor. Your response must be a JSON object with the following keys, using expressive ENGLISH words for all values.

- "Tone": Magical, warm, and inviting, creating a sense of wonder and excitement for young listeners. If the sentence is different, modify this (e.g., "Slightly tense", "Mysterious").
- "Pacing": Steady and measured, with slight pauses to emphasize magical moments. If the sentence is different, modify this (e.g., "Faster, more urgent").
- "Emotion": Wonder, curiosity, and a sense of adventure, with a lighthearted and positive vibe. If the sentence is different, modify this (e.g., "A hint of fear", "Joyful excitement").

**Example of High-Quality Output:**

**Input:** `“프레드릭, 넌 왜 일을 안 하니?”`
**Output JSON:**
```json
{
  "Tone": "Gently chiding, inquisitive",
  "Pacing": "Direct, with a slight upward inflection",
  "Emotion": "Mildly exasperated but friendly",
}

Important Rules:
- Be specific and creative for Tone, Pacing, and Emotion.
- Always include all keys. Your entire response must be ONLY the valid JSON.
"""

class StoryProcessor:
    def __init__(self, out_dir="out_audio", log_dir="log"):
        self.client = AsyncOpenAI()
        self.TRANSLATE_MODEL = "gpt-4o-mini"
        self.SENTIMENT_MODEL = "gpt-4o-mini"
        self.TTS_MODEL = "gpt-4o-mini-tts"
        self.OUT_DIR = Path(out_dir)
        self.LOG_DIR = Path(log_dir)
        self.CSV_LOG = self.LOG_DIR / "sentence_log.csv"

        # Reset dirs each run
        if self.OUT_DIR.exists():
            shutil.rmtree(self.OUT_DIR)
        if self.LOG_DIR.exists():
            shutil.rmtree(self.LOG_DIR)
        self.OUT_DIR.mkdir(parents=True)
        self.LOG_DIR.mkdir(parents=True)

    async def warm_up_api(self):
        try:
            await self.client.chat.completions.create(
                model=self.TRANSLATE_MODEL,
                messages=[{"role": "user", "content": "Hello"}],
                max_tokens=2
            )
        except Exception as e:
            print(f"⚠️ Warm-up failed: {e}")

    async def translate(self, text_with_context: str, target_lang: str="English") -> Dict[str, Any]:
        system_prompt = TRANSLATION_SYSTEM_PROMPT_TEMPLATE.format(target_lang=target_lang)
        for attempt in range(3):
            try:
                t0 = time.time()
                resp = await self.client.chat.completions.create(
                    model=self.TRANSLATE_MODEL, response_format={"type": "json_object"},
                    messages=[
                        {"role": "system", "content": system_prompt},
                        {"role": "user", "content": text_with_context},
                    ], temperature=0.7,
                )
                latency = time.time() - t0
                data = json.loads(resp.choices[0].message.content)
                data["_latency_sec"] = round(latency, 3)
                return data
            except Exception:
                if attempt == 2: raise
                await asyncio.sleep(0.7)
        return {}

    async def sentiment(self, korean_text: str) -> Dict[str, Any]:
        for attempt in range(3):
            try:
                t0 = time.time()
                resp = await self.client.chat.completions.create(
                    model=self.SENTIMENT_MODEL,
                    response_format={"type": "json_object"},
                    messages=[
                        {"role": "system", "content": SENTIMENT_SYSTEM_PROMPT},
                        {"role": "user", "content": korean_text},
                    ],
                    temperature=0.7,
                )
                latency = time.time() - t0
                data = json.loads(resp.choices[0].message.content)
                data["_latency_sec"] = round(latency, 3)
                return data
            except Exception:
                if attempt == 2:
                    raise
                await asyncio.sleep(0.7)
        return {}

    async def synthesize_tts(self, voice: str, text: str, instructions: str, out_path: Path) -> float:
        out_path.parent.mkdir(parents=True, exist_ok=True)
        t0 = time.time()
        try:
            async with self.client.audio.speech.with_streaming_response.create(
                model=self.TTS_MODEL, voice=voice, input=text, instructions=instructions
            ) as response:
                await response.stream_to_file(out_path)
            return round(time.time() - t0, 3)
        except Exception as e:
            print(f"TTS error for {out_path.name}: {e}")
            return -1.0

    async def process_sentence(self, index: int, sentences: List[str], stem: str, target_lang: str, check_latency: bool) -> Dict:
        current = sentences[index]
        context = []
        if index > 0: context.append(f"[PREVIOUS]: {sentences[index-1]}")
        context.append(f"[CURRENT]: {current}")
        if index < len(sentences)-1: context.append(f"[NEXT]: {sentences[index+1]}")
        context_prompt = "\n".join(context)

        trans_task = self.translate(context_prompt, target_lang)
        senti_task = self.sentiment(current)
        trans, senti = await asyncio.gather(trans_task, senti_task)

        translated = trans.get(f"{target_lang}_translation", "").strip()
        if not translated:
            return {"status": "translation_failed"}

        voice = "shimmer"
        out_mp3 = self.OUT_DIR / f"{stem}_sent{index+1}.mp3"
        tts_instr = (
            f"[Affect: A gentle, curious narrator with a clear American accent, guiding a magical, child-friendly adventure through a fairy tale world.]"
            f"[Pronunciation: Clear and precise, with an emphasis on storytelling, ensuring the words are easy to follow and enchanting to listen to.]"
            f"[Tone: {senti.get('Tone')}] [Emotion: {senti.get('Emotion')}] [Pacing: {senti.get('Pacing')}]"
        )
        tts_latency = await self.synthesize_tts(voice, translated, tts_instr, out_mp3)

        result = {
            "status": "ok",
            "ko_sentence": current,
            "translation": translated,
            "path": str(out_mp3),
            "tone": senti.get("Tone"),
            "emotion": senti.get("Emotion"),
            "pacing": senti.get("Pacing"),
            "voice": voice,
        }
        if check_latency:
            result.update({
                "trans_latency": trans.get("_latency_sec", 0),
                "senti_latency": senti.get("_latency_sec", 0),
                "tts_latency": tts_latency,
            })
        return result

    async def process_page(self, idx: int, page: Dict[str, str], target_lang: str, log_csv: bool, check_latency: bool):
        file_name = page["fileName"]
        sentences = kss.split_sentences(page["text"].strip())
        if not sentences: return {"status": "no_sentences"}

        stem = Path(file_name).stem
        results = await asyncio.gather(*[
            self.process_sentence(i, sentences, stem, target_lang, check_latency)
            for i in range(len(sentences))
        ])
        ok_results = [r for r in results if r.get("status") == "ok"]

        if log_csv and ok_results:
            header = ["page_file", "index", "ko", "translation", "tone", "emotion", "pacing", "voice", "path"]
            if check_latency:
                header += ["trans_latency", "senti_latency", "tts_latency"]
            if not self.CSV_LOG.exists():
                with open(self.CSV_LOG, "w", newline="", encoding="utf-8") as f: csv.writer(f).writerow(header)
            with open(self.CSV_LOG, "a", newline="", encoding="utf-8") as f:
                writer = csv.writer(f)
                for i, r in enumerate(ok_results, start=1):
                    row = [file_name, i, r["ko_sentence"], r["translation"], r["tone"], r["emotion"], r["pacing"], r["voice"], r["path"]]
                    if check_latency: row += [r["trans_latency"], r["senti_latency"], r["tts_latency"]]
                    writer.writerow(row)

        return {"status": "ok" if ok_results else "failed", "details": ok_results}


async def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--page", type=int, required=True, help="Page number (1-based).")
    parser.add_argument("--log_csv", action="store_true", help="Log results to CSV.")
    parser.add_argument("--latency", action="store_true", help="Measure and log latency.")
    parser.add_argument("--lang", type=str, default="English", help="Target translation language.")
    parser.add_argument("--out_dir", type=str, default="out_audio", help="Output audio directory.")
    parser.add_argument("--log_dir", type=str, default="log", help="Log directory.")
    args = parser.parse_args()

    story_pages = [
        {"fileName": "001.jpg", "text": "프레드릭"},
        {"fileName": "002.jpg", "text": "소들이 풀을 뜯고 말들이 뛰노는 풀밭이 있었습니다."},
        { "fileName": "003.jpg", "text": "헛간과 곳간에서 가까운 이 돌담에는 수다쟁이 들쥐 가족의 보금자리가 있었습니다." },
        { "fileName": "004.jpg", "text": "농부들이 이사를 가자, 헛간은 버려지고 곳간은 텅 비었습니다. 겨울이 다가오자, 작은 들쥐들은 옥수수와 나무 열매와 밀과 짚을 모으기 시작했습니다. 들쥐들은 밤낮없이 열심히 일했습니다. 단 한 마리, 프레드릭만 빼고 말입니다." },
        { "fileName": "005.jpg", "text": "“프레드릭, 넌 왜 일을 안 하니?” 들쥐들이 물었습니다." },
        { "fileName": "006.jpg", "text": "“나도 일하고 있어. 난 춥고 어두운 겨울날들을 위해 햇살을 모으는 중이야.” 프레드릭이 대답했습니다." },
        { "fileName": "007.jpg", "text": "어느 날, 들쥐들은 동그마니 앉아 풀밭을 내려다보고 있는 프레드릭을 보았습니다. 들쥐들은 또다시 물었습니다. “프레드릭, 지금은 뭐해?” “색깔을 모으고 있어. 겨울엔 온통 잿빛이잖아.” 프레드릭이 짤막하게 대답했습니다." },
        { "fileName": "008.jpg", "text": "한 번은 프레드릭이 조는 듯이 보였습니다. “프레드릭, 너 꿈꾸고 있지?” 들쥐들이 나무라듯 말했습니다. 그러나 프레드릭은, “아니야, 난 지금 이야기를 모으고 있어. 기나긴 겨울엔 얘기거리가 동이 나잖아.” 했습니다." },
        { "fileName": "009.jpg", "text": "겨울이 되었습니다. 첫눈이 내리자, 작은 들쥐 다섯 마리는 돌담 틈새로 난 구멍으로 들어갔습니다." },
        { "fileName": "010.jpg", "text": "처음엔 먹이가 아주 넉넉했습니다. 들쥐들은 바보 같은 늑대와 어리석은 고양이 얘기를 하며 지냈습니다. 들쥐 가족은 행복했습니다." },
        { "fileName": "011.jpg", "text": "그러나 들쥐들은 나무 열매며 곡식 낟알들을 조금씩조금씩 다 갉아먹었습니다. 짚도 다 떨어져 버렸고, 옥수수 역시 아스라한 추억이 되어 버렸습니다. 돌담 사이로는 찬바람이 스며들었습니다. 들쥐들은 누구 하나 재잘대고 싶어하지 않았습니다." },
        { "fileName": "012.jpg", "text": "그러던 들쥐들은, 햇살과 색깔과 이야기를 모은다고 했던 프레드릭의 말이 생각났습니다. “네 양식들은 어떻게 되었니, 프레드릭?” 들쥐들이 물었습니다." },
        { "fileName": "013.jpg", "text": "프레드릭이 커다란 돌 위로 기어 올라가더니, “눈을 감아 봐. 내가 너희들에게 햇살을 보내 줄게. 찬란한 금빛 햇살이 느껴지지 않니.” 했습니다. 프레드릭이 햇살 얘기를 하자, 네 마리 작은 들쥐들은 몸이 점점 따뜻해지는 것을 느낄 수 있었습니다. 프레드릭의 목소리 때문이었을까요? 마법 때문이었을까요?" },
        { "fileName": "014.jpg", "text": "“색깔은 어떻게 됐어, 프레드릭?” 들쥐들이 조바심을 내하며 물었습니다. “다시 눈을 감아 봐.” 프레드릭은 파란 덩굴꽃과, 노란 밀짚 속의 붉은 양귀비꽃, 또 초록빛 딸기 덤불 얘기를 들려 주었습니다. 들쥐들은 마음 속에 그려져 있는 색깔들을 또렷이 볼 수 있었습니다." },
        { "fileName": "015.jpg", "text": "“이야기는?” 프레드릭은 목소리를 가다듬으며 잠시 동안 가만히 있었습니다. 그리고는 마치 무대 위에서 공연이라도 하듯 말하기 시작했습니다. “눈송이는 누가 뿌릴까? 얼음은 누가 녹일까? 궂은 날씨는 누가 가져올까? 맑은 날씨는 누가 가져올까? 유월의 네 잎 클로버는 누가 피워 낼까? 날을 저물게 하는 건 누구일까? 달빛을 밝히는 건 누구일까? 하늘에 사는 들쥐 네 마리. 너희들과 나 같은 들쥐 네 마리. 봄쥐는 소나기를 몰고 온다네. 여름쥐는 온갖 꽃에 색칠을 하지. 가을쥐는 열매와 밀을 가져온다네. 겨울쥐는 오들오들 작은 몸을 웅크리지. 계절이 넷이니 얼마나 좋아? 넘치지도 모자라지도 않는 딱 사계절.” 프레드릭이 이야기를 마치자, 들쥐들은 박수를 치며 감탄을 했습니다. “프레드릭, 넌 시인이야!”" },
        { "fileName": "016.jpg", "text": "프레드릭은 얼굴을 붉히며 인사를 한 다음, 수줍게 말했습니다. “나도 알아.”" }
    ]

    processor = StoryProcessor(out_dir=args.out_dir, log_dir=args.log_dir)
    await processor.warm_up_api()

    if not 1 <= args.page <= len(story_pages):
        sys.exit(f"Page must be 1–{len(story_pages)}")

    start = time.time()
    result = await processor.process_page(args.page-1, story_pages[args.page-1], args.lang, args.log_csv, args.latency)
    print(f"✅ Done in {time.time()-start:.2f}s | Status: {result['status']}")

if __name__ == "__main__":
    asyncio.run(main())
