# InstaTranscript

Android app for Instagram video transcription with Auto/Urdu/Hindi/English modes, mixed-language preservation, long-video jobs, and TXT/Markdown/JSON export.

## Architecture

The Android app is a simple client. A Python FastAPI backend performs Instagram media extraction with yt-dlp, audio conversion with FFmpeg, and speech recognition with faster-whisper.

Language mode is passed as auto, ur, hi, or en. The backend uses transcription, not translation, so it does not intentionally transliterate Urdu/Hindi.

Public/reachable Instagram media is supported. Private or login-required media may need an exported Instagram cookies file.

## Build

Android source is under android/.

Every push to main triggers the Android APK Build workflow. The workflow uploads app-debug.apk as InstaTranscript-debug-apk.

Backend Docker:

    cp .env.example .env
    docker compose up --build

Backend health endpoint:

    GET /health

The APK needs the backend URL once. On a real phone, use the server LAN/public address, not localhost.

## Exports

The app can save:
- TXT
- Markdown
- JSON

The filename base follows:

    Caption by Account

## Resource note

The default Whisper model is large-v3 for quality-first transcription. It is heavy on CPU/RAM. Use a smaller model through WHISPER_MODEL when necessary.

## Security

Do not expose an unauthenticated transcription service publicly. Add authentication, rate limiting and quotas before public deployment.

## License

MIT
