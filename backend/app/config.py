from __future__ import annotations

import os
from dataclasses import dataclass


@dataclass(frozen=True)
class Settings:
    model_size: str = os.getenv("WHISPER_MODEL", "large-v3")
    compute_type: str = os.getenv("WHISPER_COMPUTE_TYPE", "int8")
    device: str = os.getenv("WHISPER_DEVICE", "cpu")
    cookies_path: str | None = os.getenv("COOKIES_PATH") or None
    work_dir: str = os.getenv("WORK_DIR", "/tmp/instatranscript")
    max_url_length: int = int(os.getenv("MAX_URL_LENGTH", "2048"))


settings = Settings()
