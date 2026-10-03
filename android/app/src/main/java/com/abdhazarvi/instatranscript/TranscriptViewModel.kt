package com.abdhazarvi.instatranscript

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TranscriptViewModel : ViewModel() {
    private val api = TranscriptApi()

    var jobId by mutableStateOf<String?>(null)
        private set
    var isBusy by mutableStateOf(false)
        private set
    var progress by mutableStateOf(0)
        private set
    var status by mutableStateOf("Waiting…")
        private set
    var transcript by mutableStateOf<String?>(null)
        private set
    var title by mutableStateOf<String?>(null)
        private set
    var detectedLanguage by mutableStateOf<String?>(null)
        private set
    var filenameBase by mutableStateOf("transcript")
        private set
    var error by mutableStateOf<String?>(null)
        private set

    private var exportType by mutableStateOf("txt")

    fun start(serverUrl: String, instagramUrl: String, language: String) {
        error = null
        transcript = null
        title = null
        detectedLanguage = null
        filenameBase = "transcript"
        isBusy = true
        progress = 0
        status = "Submitting…"
        exportType = "txt"

        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    api.createJob(
                        serverUrl.trim().removeSuffix("/"),
                        instagramUrl.trim(),
                        language
                    )
                }
                jobId = result.jobId
            } catch (t: Throwable) {
                isBusy = false
                error = t.message ?: "Could not start transcription."
            }
        }
    }

    fun poll(serverUrl: String, id: String) {
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    api.getJob(serverUrl.trim().removeSuffix("/"), id)
                }

                progress = result.progress
                status = result.message.ifBlank { result.status }

                when (result.status) {
                    "completed" -> {
                        transcript = result.transcript.orEmpty()
                        title = result.title
                        detectedLanguage = result.detectedLanguage
                        filenameBase = result.filenameBase ?: "transcript"
                        isBusy = false
                        jobId = null
                    }
                    "error" -> {
                        isBusy = false
                        jobId = null
                        error = result.error ?: "Transcription failed."
                    }
                }
            } catch (t: Throwable) {
                isBusy = false
                jobId = null
                error = t.message ?: "Network error."
            }
        }
    }

    fun setExportType(type: String) {
        exportType = type
    }

    fun safeFilename(extension: String): String =
        filenameBase.replace(Regex("[\\\\/:*?\"<>|\\r\\n]+"), "_")
            .trim()
            .ifBlank { "transcript" } + "." + extension

    fun exportText(): String {
        val body = transcript.orEmpty()
        return when (exportType) {
            "md" -> "# " + title.orEmpty() + "\\n\\n" + body + "\\n"
            "json" -> {
                val escapedTitle = title.orEmpty()
                    .replace("\\\\", "\\\\\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                val escapedLanguage = detectedLanguage.orEmpty()
                    .replace("\"", "\\\"")
                val escapedBody = body
                    .replace("\\\\", "\\\\\\\\")
                    .replace("\"", "\\\"")
                    .replace("\\n", "\\\\n")
                    .replace("\\r", "\\\\r")
                """{"title":"$escapedTitle","detected_language":"$escapedLanguage","transcript":"$escapedBody"}"""
            }
            else -> body + "\\n"
        }
    }

    fun clearError() {
        error = null
    }
}
