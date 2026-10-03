package com.abdhazarvi.instatranscript

import android.content.Context
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import dev.ffmpegkit.whisper.Whisper
import dev.ffmpegkit.whisper.WhisperConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
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
        val workDir = File(
            context.cacheDir,
            "transcript-" + UUID.randomUUID().toString()
        ).apply { mkdirs() }

        try {
            onStatus("Preparing local downloader…", 2)
            YoutubeDL.getInstance().init(context.applicationContext)

            val mediaTemplate = File(workDir, "media.%(ext)s").absolutePath
            val request = YoutubeDLRequest(instagramUrl)
            request.addOption("--no-playlist")
            request.addOption("--no-mtime")
            request.addOption("--print", "after_move:%(title)s|||%(uploader)s")
            request.addOption("-f", "bestaudio/best")
            request.addOption("-o", mediaTemplate)

            onStatus("Downloading Instagram media…", 5)
            val response = YoutubeDL.getInstance().execute(
                request,
                "insta-" + UUID.randomUUID().toString(),
                { progress, _, _ ->
                    onStatus(
                        "Downloading Instagram media…",
                        5 + (progress * 0.45f).toInt()
                    )
                    kotlin.Unit
                }
            )

            val media = workDir.listFiles()
                ?.firstOrNull {
                    it.name.startsWith("media.") &&
                        !it.name.endsWith(".part")
                }
                ?: error("Instagram media download completed but no media file was found.")

            check(response.exitCode == 0) {
                response.err.takeLast(1000).ifBlank {
                    "Instagram download failed."
                }
            }

            val metadata = response.out
                .lineSequence()
                .map { it.trim() }
                .filter { it.contains("|||") }
                .lastOrNull()

            val title = metadata
                ?.substringBefore("|||")
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: "Instagram Transcript"

            val account = metadata
                ?.substringAfter("|||")
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: ""

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

                val config = when (language) {
                    "ur" -> WhisperConfig(language = "ur")
                    "hi" -> WhisperConfig(language = "hi")
                    "en" -> WhisperConfig(language = "en")
                    else -> WhisperConfig()
                }

                val result = Whisper.transcribe(model, wav.absolutePath, config)
                val text = result.text.trim()
                check(text.isNotBlank()) {
                    "Whisper returned an empty transcript."
                }

                onStatus("Finished", 100)
                LocalTranscriptResult(
                    transcript = text,
                    detectedLanguage = language.takeUnless { it == "auto" },
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
}