# TODO: tts module with sentiment analysis
# apis/process_controller will call this class

import csv
import time
import asyncio
import argparse
import sys
import kss
import shutil
from pathlib import Path
from typing import Dict, List, Any
from openai import AsyncOpenAI
from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate
from pydantic import BaseModel, Field

# --- Prompt Templates ---

SENTIMENT_PROMPT = """
You are an expert children's audiobook voice director.
Analyze the sentiment of this Korean sentence and provide expressive ENGLISH directions for Tone, Pacing, and Emotion.
"""

class Sentiment(BaseModel):
    """Performance direction for a voice actor based on sentiment analysis."""
    tone: str = Field(..., description="The tone of voice to use (e.g. 'Gently chiding, inquisitive').")
    pacing: str = Field(..., description="The pacing of the speech (e.g. 'Direct, with a slight upward inflection').")
    emotion: str = Field(..., description="The emotion to convey (e.g. 'Mildly exasperated but friendly').")


class TTSProcesseor:
    
    def _create_sentiment_chain(self):
        prompt = ChatPromptTemplate.from_messages([
            ("system", SENTIMENT_PROMPT),
            ("user", "{korean_text}")
        ])
        return prompt | self.llm.with_structured_output(Sentiment)
    
    async def sentiment(self, korean_text: str) -> Dict[str, Any]:
        for attempt in range(3):
            try:
                t0 = time.time()
                response = await self.sentiment_chain.ainvoke(
                    {"korean_text": korean_text}
                )
                latency = time.time() - t0
                return {"result": response, "latency": round(latency, 3)}
            except Exception as e:
                print(f"Sentiment attempt {attempt + 1} failed: {e}")
                if attempt == 2:
                    return {"result": e, "latency": -1.0}
                await asyncio.sleep(0.7)
        return {"result": None, "latency": -1.0}

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

    async def process_page(self, page: Dict[str, str], log_csv: bool, check_latency: bool):
        file_name = page["fileName"]
        sentences = kss.split_sentences(page["text"].strip())
        if not sentences:
            return {"status": "no_sentences"}

        stem = Path(file_name).stem
        
        # --- Step 1: Concurrently get all translations and sentiments ---
        llm_tasks = []
        for i, sentence in enumerate(sentences):
            context = [f"[CURRENT]: {sentence}"]
            if i > 0: context.insert(0, f"[PREVIOUS]: {sentences[i-1]}")
            if i < len(sentences) - 1: context.append(f"[NEXT]: {sentences[i+1]}")
            context_prompt = "\n".join(context)
            
            llm_tasks.append(self.translate(context_prompt))
            llm_tasks.append(self.sentiment(sentence))
            
        llm_responses = await asyncio.gather(*llm_tasks)

        # --- Step 2: Prepare TTS tasks based on the results ---
        tts_tasks = []
        valid_results = []
        for i in range(len(sentences)):
            trans_response = llm_responses[i*2]
            senti_response = llm_responses[i*2 + 1]
            trans_result = trans_response["result"]
            senti_result = senti_response["result"]
            
            if isinstance(trans_result, Exception) or isinstance(senti_result, Exception):
                continue

            translated = trans_result.translated_text.strip()
            if not translated:
                continue
            
            voice = "shimmer"
            out_mp3 = self.OUT_DIR / f"{stem}_sent{i+1}.mp3"
            tts_instr = (
                f"[Affect: A gentle, curious narrator with a clear accent, guiding a magical, child-friendly adventure through a fairy tale world.]"
                f"[Pronunciation: Clear and precise, with an emphasis on storytelling, ensuring the words are easy to follow and enchanting to listen to.]"
                f"[Tone: {senti_result.tone}] [Emotion: {senti_result.emotion}] [Pacing: {senti_result.pacing}]"
            )
            
            # Add the TTS task to a new list to be run in parallel later
            tts_tasks.append(self.synthesize_tts(voice, translated, tts_instr, out_mp3))
            
            # Store the intermediate results
            valid_results.append({
                "ko_sentence": sentences[i], "translation": translated, "path": str(out_mp3),
                "tone": senti_result.tone, "emotion": senti_result.emotion, "pacing": senti_result.pacing,
                "voice": voice, "trans_latency": trans_response["latency"], "senti_latency": senti_response["latency"]
            })

        # --- Step 3: Run all TTS tasks concurrently ---
        tts_latencies = await asyncio.gather(*tts_tasks)

        # --- Step 4: Combine final results and log ---
        ok_results = []
        for i, result_data in enumerate(valid_results):
            result_data["status"] = "ok"
            if check_latency:
                result_data["tts_latency"] = tts_latencies[i]
            ok_results.append(result_data)

        if log_csv and ok_results:
            self._write_to_csv(ok_results, file_name, check_latency)

        return {"status": "ok" if ok_results else "failed", "details": ok_results}
        
    def _write_to_csv(self, results: List[Dict], file_name: str, check_latency: bool):
        header = ["page_file", "index", "ko", "translation", "tone", "emotion", "pacing", "voice", "path"]
        if check_latency:
            header += ["trans_latency", "senti_latency", "tts_latency"]
        if not self.CSV_LOG.exists():
            with open(self.CSV_LOG, "w", newline="", encoding="utf-8") as f:
                csv.writer(f).writerow(header)

        with open(self.CSV_LOG, "a", newline="", encoding="utf-8") as f:
            writer = csv.writer(f)
            for i, r in enumerate(results, start=1):
                row = [file_name, i, r["ko_sentence"], r["translation"], r["tone"], r["emotion"], r["pacing"], r["voice"], r["path"]]
                if check_latency:
                    row.extend([r["trans_latency"], r["senti_latency"], r["tts_latency"]])
                writer.writerow(row)


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
        { "fileName": "001.jpg", "text": "프레드릭" },
        { "fileName": "002.jpg", "text": "소들이 풀을 뜯고 말들이 뛰노는 풀밭이 있었습니다." },
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

    processor = StoryProcessor(
        out_dir=args.out_dir,
        log_dir=args.log_dir,
        target_lang=args.lang
    )

    if not 1 <= args.page <= len(story_pages):
        sys.exit(f"Page must be 1–{len(story_pages)}")

    start = time.time()
    result = await processor.process_page(
        page=story_pages[args.page - 1],
        log_csv=args.log_csv,
        check_latency=args.latency
    )
    print(f"✅ Done in {time.time()-start:.2f}s | Status: {result['status']}")