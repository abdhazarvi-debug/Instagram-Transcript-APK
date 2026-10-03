package com.abdhazarvi.instatranscript

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

object ModelManager {
    private const val MODEL_NAME = "ggml-base.bin"
    private const val MODEL_URL =
        "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.bin?download=true"

    suspend fun ensureModel(context: Context, onProgress: (Int) -> Unit): File =
        withContext(Dispatchers.IO) {
            val dir = File(context.filesDir, "models").apply { mkdirs() }
            val target = File(dir, MODEL_NAME)
            if (target.exists() && target.length() > 100_000_000L) {
                onProgress(100)
                return@withContext target
            }

            val temp = File(dir, MODEL_NAME + ".part")
            val request = Request.Builder().url(MODEL_URL).build()
            OkHttpClient().newCall(request).execute().use { response ->
                check(response.isSuccessful) {
                    "Whisper model download failed: HTTP " + response.code
                }
                val body = response.body ?: error("Whisper model response was empty.")
                val total = body.contentLength()
                body.byteStream().use { input ->
                    temp.outputStream().use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE * 4)
                        var downloaded = 0L
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            output.write(buffer, 0, read)
                            downloaded += read
                            if (total > 0) {
                                onProgress(((downloaded * 100) / total).toInt().coerceIn(0, 100))
                            }
                        }
                    }
                }
            }

            check(temp.length() > 100_000_000L) {
                "Downloaded Whisper model is incomplete."
            }
            if (target.exists()) target.delete()
            check(temp.renameTo(target)) { "Could not finalize Whisper model." }
            target
        }
}