package com.abdhazarvi.instatranscript

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

data class CreateJobResult(val jobId: String)

data class JobResult(
    val status: String,
    val progress: Int,
    val message: String,
    val transcript: String?,
    val title: String?,
    val detectedLanguage: String?,
    val filenameBase: String?,
    val error: String?
)

class TranscriptApi {
    private val client = OkHttpClient()

    fun createJob(baseUrl: String, url: String, language: String): CreateJobResult {
        val json = JSONObject()
            .put("url", url)
            .put("language", language)
            .put("export_formats", JSONArray().put("txt").put("md").put("json"))

        val request = Request.Builder()
            .url("$baseUrl/v1/transcriptions")
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IllegalStateException(parseError(raw, response.code))
            }
            return CreateJobResult(JSONObject(raw).getString("job_id"))
        }
    }

    fun getJob(baseUrl: String, jobId: String): JobResult {
        val request = Request.Builder()
            .url("$baseUrl/v1/transcriptions/$jobId")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IllegalStateException(parseError(raw, response.code))
            }

            val o = JSONObject(raw)
            return JobResult(
                status = o.optString("status", "unknown"),
                progress = o.optInt("progress", 0),
                message = o.optString("message", ""),
                transcript = o.optString("transcript", null),
                title = o.optString("title", null),
                detectedLanguage = o.optString("detected_language", null),
                filenameBase = o.optString("filename_base", null),
                error = o.optString("error", null)
            )
        }
    }

    private fun parseError(raw: String, code: Int): String =
        try {
            JSONObject(raw).optString("detail").ifBlank { "HTTP $code" }
        } catch (_: Throwable) {
            "HTTP $code"
        }
}
