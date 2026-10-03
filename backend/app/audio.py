from __future__ import annotations

import subprocess
from pathlib import Path


def extract_audio(media_path: Path, wav_path: Path) -> None:
    cmd = [
        "ffmpeg",
        "-y",
        "-i", str(media_path),
        "-vn",
        "-ac", "1",
        "-ar", "16000",
        "-c:a", "pcm_s16le",
        str(wav_path),
    ]
    completed = subprocess.run(
        cmd,
        capture_output=True,
        text=True,
        timeout=60 * 60,
    )
    if completed.returncode != 0:
        raise RuntimeError(
            f"FFmpeg audio extraction failed: {completed.stderr[-1200:]}"
        )
