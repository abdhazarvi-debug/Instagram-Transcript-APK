# Backend

Python 3.11+ and FFmpeg are required.

Windows:

    python -m venv .venv
    .venv\Scripts\activate
    pip install -r requirements.txt
    uvicorn app.main:app --host 0.0.0.0 --port 8000

Linux/macOS:

    python -m venv .venv
    source .venv/bin/activate
    pip install -r requirements.txt
    uvicorn app.main:app --host 0.0.0.0 --port 8000

The first real transcription downloads the selected Whisper model.

Recommended environment:
    WHISPER_MODEL=large-v3
    WHISPER_DEVICE=cpu
    WHISPER_COMPUTE_TYPE=int8
