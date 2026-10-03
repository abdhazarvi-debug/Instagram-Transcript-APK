from __future__ import annotations

import shutil
import uuid
from concurrent.futures import ThreadPoolExecutor
from dataclasses import dataclass, field
from pathlib import Path
from threading import Lock

from .audio import extract_audio
from .config import settings
from .instagram import download_video
from .transcriber import transcribe


@dataclass
class Job:
    job_id: str
    status: str = "queued"
    progress: int = 0
    message: str = "Queued"
    transcript: str | None = None
    title: str | None = None
    detected_language: str | None = None
    filename_base: str | None = None
    error: str | None = None
    work_dir: Path | None = field(default=None, repr=False)


class JobStore:
    def __init__(self) -> None:
        self._jobs: dict[str, Job] = {}
        self._lock = Lock()
        self._executor = ThreadPoolExecutor(max_workers=1)
        Path(settings.work_dir).mkdir(parents=True, exist_ok=True)

    def create(self, url: str, language: str) -> Job:
        job_id = uuid.uuid4().hex[:12]
        job = Job(
            job_id=job_id,
            work_dir=Path(settings.work_dir) / job_id,
        )
        with self._lock:
            self._jobs[job_id] = job
        self._executor.submit(self._run, job_id, url, language)
        return job

    def get(self, job_id: str) -> Job | None:
        with self._lock:
            return self._jobs.get(job_id)

    def update(self, job_id: str, **values: object) -> None:
        with self._lock:
            job = self._jobs[job_id]
            for key, value in values.items():
                setattr(job, key, value)

    def _run(self, job_id: str, url: str, language: str) -> None:
        job = self.get(job_id)
        if not job or not job.work_dir:
            return

        try:
            self.update(
                job_id,
                status="running",
                progress=3,
                message="Downloading Instagram video…",
            )

            media = download_video(url, job.work_dir)

            self.update(
                job_id,
                progress=12,
                message="Preparing audio…",
                title=media["title"],
                filename_base=media["filename_base"],
            )

            audio_path = job.work_dir / "audio.wav"
            extract_audio(media["path"], audio_path)

            try:
                media["path"].unlink(missing_ok=True)
            except Exception:
                pass

            self.update(
                job_id,
                progress=22,
                message="Starting speech recognition…",
            )

            transcript, detected = transcribe(
                audio_path,
                language,
                progress_cb=lambda p, m: self.update(
                    job_id,
                    progress=p,
                    message=m,
                ),
            )

            self.update(
                job_id,
                status="completed",
                progress=100,
                message="Done",
                transcript=transcript,
                detected_language=detected,
            )
        except Exception as exc:
            self.update(
                job_id,
                status="error",
                progress=100,
                message="Failed",
                error=str(exc),
            )
        finally:
            shutil.rmtree(job.work_dir, ignore_errors=True)


store = JobStore()
