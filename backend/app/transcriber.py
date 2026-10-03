from __future__ import annotations

import re
import threading
from pathlib import Path
from typing import Callable

from faster_whisper import WhisperModel

from .config import settings

_model: WhisperModel | None = None
_lock = threading.Lock()


def get_model() -> WhisperModel:
    global _model
    if _model is None:
        with _lock:
            if _model is None:
                _model = WhisperModel(
                    settings.model_size,
                    device=settings.device,
                    compute_type=settings.compute_type,
                )
    return _model


def normalize_transcript(text: str) -> str:
    text = re.sub(
        r"[\u0000-\u0008\u000B\u000C\u000E-\u001F]",
        "",
        text,
    )
    return text.replace("\r\n", "\n").strip()


def transcribe(
    audio_path: Path,
    language: str,
    progress_cb: Callable[[int, str], None] | None = None,
) -> tuple[str, str]:
    model = get_model()
    forced_language = None if language == "auto" else language

    segments, info = model.transcribe(
        str(audio_path),
        language=forced_language,
        task="transcribe",
        beam_size=5,
        vad_filter=True,
        condition_on_previous_text=True,
        temperature=0.0,
    )

    collected: list[str] = []
    total = max(1.0, float(getattr(info, "duration", 0.0) or 0.0))

    for segment in segments:
        text = normalize_transcript(segment.text)
        if text:
            collected.append(text)

        if progress_cb:
            pct = min(
                98,
                25 + int((float(segment.end) / total) * 73),
            )
            progress_cb(pct, "Transcribing…")

    transcript = normalize_transcript(" ".join(collected))
    detected = str(getattr(info, "language", "unknown"))
    return transcript, detected
