package com.abdhazarvi.instatranscript

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TranscriptViewModel : ViewModel() {
    var isBusy by mutableStateOf(false)
        private set
    var progress by mutableStateOf(0)
        private set
    var status by mutableStateOf("Ready")
        private set
    var transcript by mutableStateOf<String?>(null)
        private set
    var title by mutableStateOf<String?>(null)
        private set
    var detectedLanguage by mutableStateOf<String?>(null)
        private set
    var filenameBase by mutableStateOf("instagram-transcript")
        private set
    var error by mutableStateOf<String?>(null)
        private set

    private var exportType = "txt"

    fun start(context: Context, instagramUrl: String, language: String) {
        error = null
        transcript = null
        title = null
        detectedLanguage = null
        filenameBase = "instagram-transcript"
        progress = 0
        status = "Starting…"
        isBusy = true

        viewModelScope.launch(Dispatchers.Default) {
            try {
                val result = LocalTranscriptionEngine(context.applicationContext)
                    .transcribe(instagramUrl.trim(), language) { message, p ->
                        progress = p.coerceIn(0, 100)
                        status = message
                    }

                transcript = result.transcript
                title = result.title
                detectedLanguage = result.detectedLanguage ?: language
                filenameBase = if (result.account.isBlank()) result.title else result.title + " by " + result.account
                isBusy = false
            } catch (t: Throwable) {
                isBusy = false
                error = t.message ?: "Transcription failed."
                status = "Failed"
            }
        }
    }

    fun setExportType(type: String) {
        exportType = type
    }

    fun safeFilename(extension: String): String =
        filenameBase.replace(Regex("[\\\\/:*?"<>|\\r\\n]+"), "_")
            .trim()
            .ifBlank { "instagram-transcript" } + "." + extension

    fun exportText(): String {
        val body = transcript.orEmpty()
        return when (exportType) {
            "md" -> "# " + title.orEmpty() + "\n\n" + body + "\n"
            "json" -> {
                val escapedTitle = title.orEmpty()
                    .replace("\\\\", "\\\\\\\\")
                    .replace(""", "\\\"")
                    .replace("\n", "\\\\n")
                    .replace("\r", "\\\\r")
                val escapedLanguage = detectedLanguage.orEmpty()
                    .replace(""", "\\\"")
                val escapedBody = body
                    .replace("\\\\", "\\\\\\\\")
                    .replace(""", "\\\"")
                    .replace("\n", "\\\\n")
                    .replace("\r", "\\\\r")
                """{"title":"$escapedTitle","detected_language":"$escapedLanguage","transcript":"$escapedBody"}"""
            }
            else -> body + "\n"
        }
    }

    fun clearError() {
        error = null
    }
}