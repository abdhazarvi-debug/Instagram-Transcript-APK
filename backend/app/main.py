from __future__ import annotations

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware

from .jobs import store
from .models import (
    TranscriptionCreated,
    TranscriptionRequest,
    TranscriptionStatus,
)

app = FastAPI(
    title="InstaTranscript API",
    version="0.1.0",
    description="Instagram transcription with yt-dlp, FFmpeg and faster-whisper.",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=False,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.post("/v1/transcriptions", response_model=TranscriptionCreated)
def create_transcription(
    payload: TranscriptionRequest,
) -> TranscriptionCreated:
    try:
        job = store.create(payload.url.strip(), payload.language)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc

    return TranscriptionCreated(
        job_id=job.job_id,
        status=job.status,
    )


@app.get(
    "/v1/transcriptions/{job_id}",
    response_model=TranscriptionStatus,
)
def get_transcription(job_id: str) -> TranscriptionStatus:
    job = store.get(job_id)
    if job is None:
        raise HTTPException(status_code=404, detail="Job not found.")

    return TranscriptionStatus(
        job_id=job.job_id,
        status=job.status,
        progress=job.progress,
        message=job.message,
        transcript=job.transcript,
        title=job.title,
        detected_language=job.detected_language,
        filename_base=job.filename_base,
        error=job.error,
    )
