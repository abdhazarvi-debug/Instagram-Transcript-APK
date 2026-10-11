package com.abdhazarvi.instatranscript

import android.content.Context
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import dev.ffmpegkit_maintained.ytdlp.YtDlp
import dev.ffmpegkit_maintained.ytdlp.YtDlpRequest
import dev.ffmpegkit.whisper.Whisper
import dev.ffmpegkit.whisper.WhisperConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URI
import java.util.UUID

data class LocalTranscriptResult(
    val transcript: String,
    val detectedLanguage: String?,
    val title: String,
    val account: String
)

class LocalTranscriptionEngine(private val context: Context) {

    suspend fun transcribe(
        instagramUrl: String,
        language: String,
        onStatus: (String, Int) -> Unit
    ): LocalTranscriptResult = withContext(Dispatchers.IO) {
        validateVideoUrl(instagramUrl)
        val workDir = File(
            context.cacheDir,
            "transcript-" + UUID.randomUUID().toString()
        ).apply { mkdirs() }

        try {
            onStatus("Preparing local downloader…", 2)
            YtDlp.init(context.applicationContext)

            val mediaTemplate = File(workDir, "media.%(ext)s").absolutePath
            val request = YtDlpRequest(instagramUrl)
                .setOutputTemplate(mediaTemplate)
                .addOption("--no-playlist")
                .addOption("--no-mtime")
                .addOption("--print", "after_move:%(title)s|||%(uploader)s")
                .addOption("-f", "bestaudio/best")
            .also {
                if (InstagramCookieStore.hasCookies(context)) {
                    it.addOption("--cookies", InstagramCookieStore.cookieFile(context).absolutePath)
                }
            }

            var metadataLine = ""
            onStatus("Downloading Instagram media…", 5)

            val response = YtDlp.execute(request) { progress, _, line ->
                if (line.contains("|||")) {
                    metadataLine = line.trim()
                }
                onStatus(
                    "Downloading Instagram media…",
                    5 + (progress * 0.45f).toInt()
                )
            }

            check(response.isSuccess) {
                if (InstagramCookieStore.hasCookies(context)) {
                    "Video download failed (exit " + response.exitCode + "). Your saved session may have expired. Tap Re-login and try again."
                } else {
                    "Video download was blocked by the source site. Try a public link, or tap Login and try again."
                }
            }
            val media = workDir.listFiles()
                ?.firstOrNull {
                    it.name.startsWith("media.") &&
                        !it.name.endsWith(".part")
                }
                ?: error("The downloader completed but did not produce a video or audio file.")

            val title = metadataLine
                .substringBefore("|||")
                .trim()
                .takeIf { it.isNotBlank() }
                ?: "Video Transcript"

            val account = metadataLine
                .substringAfter("|||", "")
                .trim()

            onStatus("Converting audio locally…", 52)
            val wav = File(workDir, "audio.wav")
            val ffmpeg = FFmpegKit.execute(
                "-y -i " + quote(media.absolutePath) +
                    " -vn -ar 16000 -ac 1 -c:a pcm_s16le " +
                    quote(wav.absolutePath)
            )

            check(ReturnCode.isSuccess(ffmpeg.returnCode)) {
                "Audio conversion failed: " +
                    (ffmpeg.failStackTrace ?: ffmpeg.output.takeLast(800))
            }

            onStatus("Preparing offline Whisper…", 62)
            val modelFile = ModelManager.ensureModel(context) { p ->
                onStatus(
                    "Downloading Whisper model… " + p + "%",
                    62 + (p * 0.18f).toInt()
                )
            }

            val model = Whisper.loadModel(context, modelFile.absolutePath)
            try {
                onStatus("Transcribing on this phone…", 82)

                val threads = Runtime.getRuntime()
                    .availableProcessors()
                    .coerceIn(2, 8)

                val config = when (language) {
                    "ur" -> WhisperConfig(language = "ur", threads = threads, printTimestamps = false)
                    "hi" -> WhisperConfig(language = "hi", threads = threads, printTimestamps = false)
                    "en" -> WhisperConfig(language = "en", threads = threads, printTimestamps = false)
                    else -> WhisperConfig(language = "auto", threads = threads, printTimestamps = false)
                }

                val result = Whisper.transcribe(model, wav.absolutePath, config)

                val text = result.segments
                    .asSequence()
                    .map { it.text.trim().replace(Regex("\\s+"), " ") }
                    .filter { it.isNotBlank() }
                    .fold(mutableListOf<String>()) { acc, segment ->
                        if (acc.lastOrNull() != segment) acc.add(segment)
                        acc
                    }
                    .joinToString("\n")
                    .trim()
                    .ifBlank { result.text.trim() }
                check(text.isNotBlank()) {
                    "Whisper returned an empty transcript."
                }

                onStatus("Finished", 100)
                LocalTranscriptResult(
                    transcript = text,
                    // The multilingual model detects the language for auto mode. This
                    // wrapper does not expose its ISO code, so do not report a guess.
                    detectedLanguage = language.takeUnless { it == "auto" } ?: "Auto-detected",
                    title = title,
                    account = account
                )
            } finally {
                Whisper.releaseModel(model)
            }
        } finally {
            workDir.deleteRecursively()
        }
    }

    private fun quote(value: String): String =
        "'" + value.replace("'", "'\\''") + "'"

    private fun validateVideoUrl(value: String) {
        val uri = runCatching { URI(value.trim()) }.getOrNull()
            ?: error("Please paste a valid video link.")
        val host = uri.host?.lowercase()?.removePrefix("www.")
            ?: error("Please paste a valid video link.")
        val allowed = host == "instagram.com" || host.endsWith(".instagram.com") ||
            host == "facebook.com" || host.endsWith(".facebook.com") || host == "fb.watch"
        check(uri.scheme in setOf("http", "https") && allowed) {
            "Only public Instagram, Facebook, and fb.watch video links are supported."
        }
    }
}
