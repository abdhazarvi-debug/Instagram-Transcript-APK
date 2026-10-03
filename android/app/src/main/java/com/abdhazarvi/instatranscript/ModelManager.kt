package com.abdhazarvi.instatranscript

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

object ModelManager {
    private const val MODEL_NAME = "ggml-small.bin"
    private const val MODEL_VERSION = "small-v1"
    private const val MODEL_URL =
        "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-small.bin?download=true"

    suspend fun ensureModel(context: Context, onProgress: (Int) -> Unit): File =
        withContext(Dispatchers.IO) {
            val dir = File(context.filesDir, "models").apply { mkdirs() }
            val target = File(dir, MODEL_NAME)
            val marker = File(dir, MODEL_NAME + ".version")

            if (
                target.exists() &&
                target.length() > 400_000_000L &&
                marker.exists() &&
                marker.readText(Charsets.UTF_8).trim() == MODEL_VERSION
            ) {
                onProgress(100)
                return@withContext target
            }

            if (target.exists()) target.delete()
            if (marker.exists()) marker.delete()

            val temp = File(dir, MODEL_NAME + ".part")
            if (temp.exists()) temp.delete()

            val request = Request.Builder()
                .url(MODEL_URL)
                .header("Accept", "application/octet-stream")
                .build()

            OkHttpClient().newCall(request).execute().use { response ->
                check(response.isSuccessful) {
                    "Whisper small model download failed: HTTP " + response.code
                }

                val body = response.body ?: error("Whisper model response was empty.")
                val total = body.contentLength()

                body.byteStream().use { input ->
                    temp.outputStream().use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE * 8)
                        var downloaded = 0L

                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break

                            output.write(buffer, 0, read)
                            downloaded += read

                            if (total > 0) {
                                onProgress(
                                    ((downloaded * 100) / total)
                                        .toInt()
                                        .coerceIn(0, 100)
                                )
                            }
                        }
                    }
                }
            }

            check(temp.length() > 400_000_000L) {
                "Downloaded Whisper small model is incomplete."
            }

            check(temp.renameTo(target)) {
                "Could not finalize Whisper small model."
            }

            marker.writeText(MODEL_VERSION, Charsets.UTF_8)
            onProgress(100)
            target
        }
}