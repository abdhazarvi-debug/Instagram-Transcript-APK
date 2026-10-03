# InstaTranscript

Android app for turning public/reachable Instagram video or Reel links into transcripts.

## Self-contained Android architecture

The APK no longer needs a Python/FastAPI backend or a backend URL.

Flow:

    Instagram URL
        ↓
    yt-dlp running inside the APK
        ↓
    local audio conversion with FFmpeg
        ↓
    multilingual Whisper/whisper.cpp running on the phone
        ↓
    transcript
        ↓
    TXT / Markdown / JSON export

The yt-dlp Android library bundles its runtime inside the app, while the Whisper Android library performs speech recognition locally on-device. The first transcription downloads the multilingual Whisper base model once (~142 MB) and caches it in the app's private storage. Subsequent transcriptions reuse the cached model.

## Language modes

- Auto Detect
- Urdu
- Hindi
- English

The app uses transcription rather than a translation/transliteration stage. For mixed speech, Whisper receives the original audio and is not instructed to translate it to another language.

## Instagram access

Public/reachable Instagram media is supported. Login-required/private media can still fail because Instagram access rules and authentication can change. The app does not ask for an Instagram password.

## Exports

- TXT
- Markdown
- JSON

## Build

Android source is under android/.

Every push to main builds release APKs and uploads them as the GitHub Actions artifact:

    InstaTranscript-release-apks

ABI splits are enabled for arm64-v8a and x86_64 to keep device-specific APKs smaller.

## Model choice

The app currently uses Whisper multilingual base. The published Whisper Android documentation lists base at about 142 MB and describes it as a 99-language model suitable as a phone speed/quality trade-off.

## Optional backend

The old backend directory remains in the repository for reference/deployment experiments, but the Android app does not depend on it.

## License

MIT
