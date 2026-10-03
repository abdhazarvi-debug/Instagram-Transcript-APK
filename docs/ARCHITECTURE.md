# Architecture

Android
  |
  | POST Instagram URL + language
  v
FastAPI
  |
  +--> yt-dlp --> Instagram media
  |
  +--> FFmpeg --> 16 kHz mono WAV
  |
  +--> faster-whisper
          |
          +--> Auto detection
          +--> Manual Urdu / Hindi / English
  |
  v
Async job
  |
  +--> Android transcript display
  +--> TXT export
  +--> Markdown export
  +--> JSON export

A translation stage is intentionally absent. The goal is transcription of the spoken language.

A one-worker queue is used initially because Whisper is CPU/RAM intensive.
