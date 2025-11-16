from django.test import TestCase
from unittest.mock import Mock, patch, AsyncMock, MagicMock
from apis.modules.tts_processor import TTSModule
import asyncio
import base64


class TestTTSModule(TestCase):
    """Unit tests for TTSModule"""

    def setUp(self):
        """Set up test fixtures"""
        with patch("shutil.rmtree"):
            with patch("pathlib.Path.mkdir"):
                self.tts_module = TTSModule(
                    out_dir="test_out", log_dir="test_log", target_lang="English"
                )

    def test_01_init(self):
        """Test TTSModule initialization"""
        self.assertEqual(self.tts_module.target_lang, "English")
        self.assertEqual(self.tts_module.TTS_MODEL, "gpt-4o-mini-tts")
        self.assertEqual(self.tts_module.TTS_MODEL_LITE, "tts-1")
        self.assertIsNotNone(self.tts_module.client)
        self.assertIsNotNone(self.tts_module.llm)

    def test_02_create_translation_chain(self):
        """Test translation chain creation"""
        chain = self.tts_module._create_translation_chain()
        self.assertIsNotNone(chain)

    def test_03_create_sentiment_chain(self):
        """Test sentiment chain creation"""
        chain = self.tts_module._create_sentiment_chain()
        self.assertIsNotNone(chain)

    @patch("apis.modules.tts_processor.TTSModule._create_translation_chain")
    def test_04_translate_success(self, mock_chain):
        """Test successful translation"""
        # Mock the translation result
        mock_result = Mock()
        mock_result.translated_text = "Test translation"

        mock_chain_instance = Mock()
        mock_chain_instance.ainvoke = AsyncMock(return_value=mock_result)
        self.tts_module.translation_chain = mock_chain_instance

        # Run async function
        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)
        result = loop.run_until_complete(
            self.tts_module.translate("Test text with context")
        )
        loop.close()

        self.assertEqual(result["result"].translated_text, "Test translation")
        self.assertGreaterEqual(result["latency"], 0)

    @patch("apis.modules.tts_processor.TTSModule._create_translation_chain")
    def test_05_translate_failure_with_retry(self, mock_chain):
        """Test translation failure with retry logic"""
        # Mock translation to fail 3 times
        mock_chain_instance = Mock()
        mock_chain_instance.ainvoke = AsyncMock(
            side_effect=Exception("Translation API error")
        )
        self.tts_module.translation_chain = mock_chain_instance

        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)
        result = loop.run_until_complete(
            self.tts_module.translate("Test text with context")
        )
        loop.close()

        # Should return error result after 3 attempts
        self.assertIsInstance(result["result"], Exception)
        self.assertEqual(result["latency"], -1.0)

    @patch("apis.modules.tts_processor.TTSModule._create_sentiment_chain")
    def test_06_sentiment_success(self, mock_chain):
        """Test successful sentiment analysis"""
        # Mock sentiment result
        mock_result = Mock()
        mock_result.tone = "happy"
        mock_result.emotion = "joyful"
        mock_result.pacing = "moderate"

        mock_chain_instance = Mock()
        mock_chain_instance.ainvoke = AsyncMock(return_value=mock_result)
        self.tts_module.sentiment_chain = mock_chain_instance

        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)
        result = loop.run_until_complete(self.tts_module.sentiment("테스트 문장"))
        loop.close()

        self.assertEqual(result["result"].tone, "happy")
        self.assertEqual(result["result"].emotion, "joyful")
        self.assertEqual(result["result"].pacing, "moderate")
        self.assertGreaterEqual(result["latency"], 0)

    @patch("apis.modules.tts_processor.TTSModule._create_sentiment_chain")
    def test_07_sentiment_failure_with_retry(self, mock_chain):
        """Test sentiment failure with retry logic"""
        mock_chain_instance = Mock()
        mock_chain_instance.ainvoke = AsyncMock(
            side_effect=Exception("Sentiment API error")
        )
        self.tts_module.sentiment_chain = mock_chain_instance

        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)
        result = loop.run_until_complete(self.tts_module.sentiment("테스트 문장"))
        loop.close()

        self.assertIsInstance(result["result"], Exception)
        self.assertEqual(result["latency"], -1.0)

    @patch("builtins.open", create=True)
    def test_08_synthesize_tts_success(self, mock_open):
        """Test successful TTS synthesis"""
        # Mock OpenAI TTS response
        mock_response = Mock()
        mock_response.content = b"fake audio data"

        self.tts_module.client.audio.speech.create = AsyncMock(
            return_value=mock_response
        )

        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)
        latency, audio_bytes = loop.run_until_complete(
            self.tts_module.synthesize_tts(
                voice="shimmer",
                text="Test text",
                instructions="Test instructions",
                out_path=self.tts_module.OUT_DIR / "test.mp3",
                response_format="mp3",
            )
        )
        loop.close()

        self.assertGreaterEqual(latency, 0)
        self.assertEqual(audio_bytes, b"fake audio data")

    def test_09_synthesize_tts_failure(self):
        """Test TTS synthesis failure"""
        self.tts_module.client.audio.speech.create = AsyncMock(
            side_effect=Exception("TTS API error")
        )

        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)
        latency, audio_bytes = loop.run_until_complete(
            self.tts_module.synthesize_tts(
                voice="shimmer",
                text="Test text",
                instructions="Test instructions",
                out_path=self.tts_module.OUT_DIR / "test.mp3",
                response_format="mp3",
            )
        )
        loop.close()

        self.assertEqual(latency, -1.0)
        self.assertIsNone(audio_bytes)

    @patch("builtins.open", create=True)
    def test_10_synthesize_tts_lite_success(self, mock_open):
        """Test successful TTS lite synthesis"""
        mock_response = Mock()
        mock_response.content = b"fake audio data lite"

        self.tts_module.client.audio.speech.create = AsyncMock(
            return_value=mock_response
        )

        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)
        latency, audio_bytes = loop.run_until_complete(
            self.tts_module.synthesize_tts_lite(
                voice="echo", text="Test title", response_format="mp3"
            )
        )
        loop.close()

        self.assertGreaterEqual(latency, 0)
        self.assertEqual(audio_bytes, b"fake audio data lite")

    def test_11_synthesize_tts_lite_failure(self):
        """Test TTS lite synthesis failure"""
        self.tts_module.client.audio.speech.create = AsyncMock(
            side_effect=Exception("TTS Lite API error")
        )

        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)
        latency, audio_bytes = loop.run_until_complete(
            self.tts_module.synthesize_tts_lite(voice="echo", text="Test title")
        )
        loop.close()

        self.assertEqual(latency, -1.0)
        self.assertIsNone(audio_bytes)

    @patch("kss.split_sentences")
    def test_12_get_translations_only_success(self, mock_kss):
        """Test getting translations without TTS"""
        mock_kss.return_value = ["첫 번째 문장.", "두 번째 문장."]

        # Mock translation and sentiment
        mock_trans_result = Mock()
        mock_trans_result.translated_text = "First sentence."
        mock_senti_result = Mock()
        mock_senti_result.tone = "calm"
        mock_senti_result.emotion = "neutral"
        mock_senti_result.pacing = "normal"

        self.tts_module.translation_chain = Mock()
        self.tts_module.translation_chain.ainvoke = AsyncMock(
            return_value=mock_trans_result
        )
        self.tts_module.sentiment_chain = Mock()
        self.tts_module.sentiment_chain.ainvoke = AsyncMock(
            return_value=mock_senti_result
        )

        page_data = {"fileName": "test.jpg", "text": "첫 번째 문장. 두 번째 문장."}

        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)
        result = loop.run_until_complete(
            self.tts_module.get_translations_only(page_data)
        )
        loop.close()

        self.assertEqual(result["status"], "ok")
        self.assertGreater(len(result["sentences"]), 0)
        self.assertIn("translation", result["sentences"][0])
        self.assertIn("tone", result["sentences"][0])
        self.assertIn("emotion", result["sentences"][0])
        self.assertIn("pacing", result["sentences"][0])

    @patch("kss.split_sentences")
    def test_13_get_translations_only_no_sentences(self, mock_kss):
        """Test get_translations_only with no sentences"""
        mock_kss.return_value = []

        page_data = {"fileName": "test.jpg", "text": ""}

        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)
        result = loop.run_until_complete(
            self.tts_module.get_translations_only(page_data)
        )
        loop.close()

        self.assertEqual(result["status"], "no_sentences")
        self.assertEqual(result["sentences"], [])

    @patch("kss.split_sentences")
    def test_14_get_translations_only_translation_failure(self, mock_kss):
        """Test get_translations_only when translation fails"""
        mock_kss.return_value = ["테스트 문장."]

        # Mock translation to fail
        self.tts_module.translation_chain = Mock()
        self.tts_module.translation_chain.ainvoke = AsyncMock(
            side_effect=Exception("Translation failed")
        )
        self.tts_module.sentiment_chain = Mock()
        self.tts_module.sentiment_chain.ainvoke = AsyncMock(return_value=Mock())

        page_data = {"fileName": "test.jpg", "text": "테스트 문장."}

        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)
        result = loop.run_until_complete(
            self.tts_module.get_translations_only(page_data)
        )
        loop.close()

        # All sentences failed, so result should be failed
        self.assertEqual(result["status"], "failed")
        self.assertEqual(len(result["sentences"]), 0)

    @patch("builtins.open", create=True)
    def test_15_run_tts_only_success(self, mock_open):
        """Test running TTS with pre-computed translations"""
        translation_data = {
            "status": "ok",
            "sentences": [
                {
                    "translation": "First sentence.",
                    "tone": "happy",
                    "emotion": "joyful",
                    "pacing": "fast",
                }
            ],
        }

        # Mock TTS response
        mock_response = Mock()
        mock_response.content = b"audio data"
        self.tts_module.client.audio.speech.create = AsyncMock(
            return_value=mock_response
        )

        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)
        result = loop.run_until_complete(
            self.tts_module.run_tts_only(
                translation_data, "session123", 0, 0, "shimmer"
            )
        )
        loop.close()

        self.assertGreater(len(result), 0)
        # Result should be base64 encoded audio
        self.assertIsInstance(result[0], str)
        # Verify it's valid base64
        decoded = base64.b64decode(result[0])
        self.assertEqual(decoded, b"audio data")

    def test_16_run_tts_only_empty_sentences(self):
        """Test run_tts_only with empty sentences"""
        translation_data = {"status": "ok", "sentences": []}

        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)
        result = loop.run_until_complete(
            self.tts_module.run_tts_only(translation_data, "session123", 0, 0, "shimmer")
        )
        loop.close()

        self.assertEqual(result, [])

    @patch("builtins.open", create=True)
    def test_17_translate_and_tts_cover_success(self, mock_open):
        """Test cover title translation and TTS"""
        # Mock translation
        mock_trans_result = Mock()
        mock_trans_result.translated_text = "Book Title"
        self.tts_module.translation_chain = Mock()
        self.tts_module.translation_chain.ainvoke = AsyncMock(
            return_value=mock_trans_result
        )

        # Mock TTS
        mock_response = Mock()
        mock_response.content = b"cover audio"
        self.tts_module.client.audio.speech.create = AsyncMock(
            return_value=mock_response
        )

        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)
        translated, male_audio, female_audio = loop.run_until_complete(
            self.tts_module.translate_and_tts_cover("책 제목", "session123", 0)
        )
        loop.close()

        self.assertEqual(translated, "Book Title")
        self.assertIsInstance(male_audio, str)
        self.assertIsInstance(female_audio, str)
        # Both should be base64 encoded
        self.assertGreater(len(male_audio), 0)
        self.assertGreater(len(female_audio), 0)

    def test_18_translate_and_tts_cover_translation_failure(self):
        """Test cover TTS when translation fails"""
        # Mock translation to return exception result
        mock_trans_response = {"result": Exception("Translation failed"), "latency": -1.0}

        original_translate = self.tts_module.translate
        async def mock_translate(text):
            return mock_trans_response

        self.tts_module.translate = mock_translate

        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)
        result = loop.run_until_complete(
            self.tts_module.translate_and_tts_cover("책 제목", "session123", 0)
        )
        loop.close()

        # Function should return ("", "") when translation fails
        self.assertEqual(len(result), 2)
        self.assertEqual(result, ("", ""))

    @patch("builtins.open", create=True)
    def test_19_translate_and_tts_cover_empty_translation(self, mock_open):
        """Test cover TTS when translation is empty"""
        # Mock empty translation result
        mock_trans_result = Mock()
        mock_trans_result.translated_text = ""
        mock_trans_response = {"result": mock_trans_result, "latency": 0.5}

        async def mock_translate(text):
            return mock_trans_response

        self.tts_module.translate = mock_translate

        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)
        result = loop.run_until_complete(
            self.tts_module.translate_and_tts_cover("", "session123", 0)
        )
        loop.close()

        # Function should return ("", "") when translation is empty
        self.assertEqual(len(result), 2)
        self.assertEqual(result, ("", ""))
