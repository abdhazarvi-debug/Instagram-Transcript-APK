# InstaTranscript

Android app for turning public/reachable Instagram, Facebook, and `fb.watch` video or Reel links into transcripts.

## Self-contained Android architecture

The APK no longer needs a Python/FastAPI backend or a backend URL.

Flow:

    Instagram / Facebook URL
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

The yt-dlp Android library bundles its runtime inside the app, while the Whisper Android library performs speech recognition locally on-device. The first transcription downloads the multilingual Whisper Small model once (~466 MB) and caches it in the app's private storage. Subsequent transcriptions reuse the cached model.

## Language modes

- Auto Detect (keeps the spoken language)
- Urdu
- Hindi
- English

The app uses transcription rather than a translation/transliteration stage. For mixed speech, Whisper receives the original audio and is not instructed to translate it to another language.

## Instagram and Facebook access

Instagram can block anonymous media extraction with login or rate-limit errors. This is an Instagram-side access restriction, and recent yt-dlp reports show the same failure mode for public-looking Reel URLs. citeturn943988search5

The APK therefore includes an optional **Instagram Login** screen. Log in inside the app, tap **Done**, and the app stores a local Netscape-format cookie file in private app storage. yt-dlp then uses those cookies automatically for the Reel download. The cookies are not uploaded to the backend because the Android app does not use a backend.

The maintainer's Android yt-dlp library documents the same cookie-based authentication flow with `--cookies`. citeturn284878search0

Private/login-required media may still fail if Instagram changes its session or blocks the device/IP. In that case, re-login in the app.

Facebook and `fb.watch` links use the same bundled yt-dlp downloader. Public videos work without login; private or region-restricted videos may still be rejected by the source site.

## Exports

- TXT
- Markdown
- JSON

TXT and Markdown downloads use the sanitized source video title as the filename (for example, `My Video.txt` and `My Video.md`).

## Build

Android source is under android/.

Every push to main builds release APKs and uploads them as the GitHub Actions artifact:

    InstaTranscript-release-apks

ABI splits are enabled for arm64-v8a and x86_64 to keep device-specific APKs smaller.

## Model choice

The app currently uses Whisper multilingual Small. It supports transcription in 99 languages and runs fully on-device after the one-time model download; no AI API key or paid cloud service is used.

## Optional backend

The old backend directory remains in the repository for reference/deployment experiments, but the Android app does not depend on it.

## License

MIT

CI smoke-build branch: validates the self-contained Android release APK.
