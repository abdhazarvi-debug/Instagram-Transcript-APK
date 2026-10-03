# Testing

## Backend

    cd backend
    python -m pytest -q

Smoke check:

    curl http://localhost:8000/health

Expected:

    {"status":"ok"}

## Language matrix

Test:
- Urdu-only speech
- Hindi-only speech
- English-only speech
- Urdu + English mixed speech

Verify that output is not intentionally translated or transliterated and that Unicode scripts are preserved.

## Long video

Use a video longer than 20 minutes and verify:
- job progress advances
- the complete audio is processed
- final transcript is returned
- TXT, Markdown and JSON open as UTF-8

## Instagram

Test a public/reachable reel first. Test login-required media only with a valid exported cookies file.
