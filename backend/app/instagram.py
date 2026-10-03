from __future__ import annotations

import os
import re
from pathlib import Path
from urllib.parse import urlparse

import yt_dlp

from .config import settings

_ALLOWED_HOSTS = {"instagram.com", "www.instagram.com", "m.instagram.com"}


def validate_instagram_url(url: str) -> None:
    parsed = urlparse(url)
    if parsed.scheme not in {"http", "https"} or parsed.hostname not in _ALLOWED_HOSTS:
        raise ValueError("Please provide a valid Instagram URL.")


def _safe_name(value: str) -> str:
    value = re.sub(r"[\\\\/:*?"<>|\r\n]+", "_", value)
    value = re.sub(r"\s+", " ", value).strip(" ._")
    return value[:160] or "transcript"


def download_video(url: str, job_dir: Path) -> dict:
    validate_instagram_url(url)
    if len(url) > settings.max_url_length:
        raise ValueError("Instagram URL is too long.")

    job_dir.mkdir(parents=True, exist_ok=True)

    options = {
        "outtmpl": str(job_dir / "source.%(ext)s"),
        "format": "bestvideo+bestaudio/best",
        "merge_output_format": "mp4",
        "noplaylist": True,
        "quiet": True,
        "no_warnings": True,
        "retries": 3,
        "socket_timeout": 30,
    }

    if settings.cookies_path and os.path.isfile(settings.cookies_path):
        options["cookiefile"] = settings.cookies_path

    with yt_dlp.YoutubeDL(options) as ydl:
        info = ydl.extract_info(url, download=True)

    caption = (
        info.get("description")
        or info.get("title")
        or "Instagram transcript"
    ).splitlines()[0].strip()
    title = (info.get("title") or caption or "Instagram transcript").strip()
    account = (
        info.get("uploader")
        or info.get("uploader_id")
        or "Instagram"
    ).strip()

    return {
        "path": _find_media(job_dir),
        "title": title,
        "caption": caption,
        "account": account,
        "filename_base": _safe_name(f"{caption} by {account}"),
    }


def _find_media(job_dir: Path) -> Path:
    candidates = [
        p for p in job_dir.iterdir()
        if p.is_file() and p.name.startswith("source.")
    ]
    if not candidates:
        raise FileNotFoundError("yt-dlp did not produce a media file.")
    return candidates[0]
