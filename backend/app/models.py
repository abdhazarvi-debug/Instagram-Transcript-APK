from __future__ import annotations

from typing import Literal

from pydantic import BaseModel, Field

Language = Literal["auto", "ur", "hi", "en"]


class TranscriptionRequest(BaseModel):
    url: str = Field(min_length=1, max_length=2048)
    language: Language = "auto"
    export_formats: list[str] = Field(default_factory=lambda: ["txt", "md", "json"])


class TranscriptionCreated(BaseModel):
    job_id: str
    status: str


class TranscriptionStatus(BaseModel):
    job_id: str
    status: str
    progress: int
    message: str
    transcript: str | None = None
    title: str | None = None
    detected_language: str | None = None
    filename_base: str | None = None
    error: str | None = None
