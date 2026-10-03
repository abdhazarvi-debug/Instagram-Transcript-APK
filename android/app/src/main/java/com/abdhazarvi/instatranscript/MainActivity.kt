package com.abdhazarvi.instatranscript

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abdhazarvi.instatranscript.ui.theme.InstaTranscriptTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            InstaTranscriptTheme {
                TranscriptScreen()
            }
        }
    }
}

@Composable
private fun TranscriptScreen(vm: TranscriptViewModel = viewModel()) {
    val context = LocalContext.current
    var serverUrl by rememberSaveable { mutableStateOf(Preferences.getServerUrl(context)) }
    var url by rememberSaveable { mutableStateOf("") }
    var language by rememberSaveable { mutableStateOf("auto") }

    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(vm.exportText().toByteArray(Charsets.UTF_8))
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "InstaTranscript",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Instagram video → transcript",
            style = MaterialTheme.typography.bodyMedium
        )

        OutlinedTextField(
            value = serverUrl,
            onValueChange = { serverUrl = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Backend URL") },
            singleLine = true,
            supportingText = {
                Text("Example: http://192.168.1.10:8000")
            }
        )

        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Instagram video / reel URL") }
        )

        Text("Language", fontWeight = FontWeight.SemiBold)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf(
                "auto" to "Auto",
                "ur" to "Urdu",
                "hi" to "Hindi",
                "en" to "English"
            ).forEach { item ->
                val code = item.first
                val label = item.second
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.selectable(
                        selected = language == code,
                        onClick = { language = code },
                        role = Role.RadioButton
                    )
                ) {
                    RadioButton(
                        selected = language == code,
                        onClick = null
                    )
                    Text(label)
                }
            }
        }

        Button(
            onClick = {
                Preferences.setServerUrl(context, serverUrl)
                vm.start(serverUrl, url, language)
            },
            enabled = !vm.isBusy && serverUrl.isNotBlank() && url.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (vm.isBusy) "Working…" else "Transcribe Video")
        }

        if (vm.isBusy) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator()
                        Column {
                            Text(vm.status)
                            Text(vm.progress.toString() + "%")
                        }
                    }
                    Divider()
                    Text(
                        "Long videos are processed as background jobs. Keep this screen open."
                    )
                }
            }
        }

        vm.error?.let { message ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Error", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(message)
                    TextButton(onClick = vm::clearError) {
                        Text("Dismiss")
                    }
                }
            }
        }

        vm.transcript?.let { resultText ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        vm.title ?: "Transcript",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Detected: " + (vm.detectedLanguage ?: "unknown"),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Divider()
                    Text(resultText)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                vm.setExportType("txt")
                                saveLauncher.launch(vm.safeFilename("txt"))
                            }
                        ) {
                            Text("TXT")
                        }
                        Button(
                            onClick = {
                                vm.setExportType("md")
                                saveLauncher.launch(vm.safeFilename("md"))
                            }
                        ) {
                            Text("MD")
                        }
                        Button(
                            onClick = {
                                vm.setExportType("json")
                                saveLauncher.launch(vm.safeFilename("json"))
                            }
                        ) {
                            Text("JSON")
                        }
                    }
                }
            }
        }

        Text(
            "The service transcribes spoken language; it does not intentionally translate or transliterate it.",
            style = MaterialTheme.typography.bodySmall
        )
    }

    LaunchedEffect(vm.jobId, vm.isBusy, serverUrl) {
        val id = vm.jobId ?: return@LaunchedEffect
        while (vm.isBusy && vm.jobId == id) {
            vm.poll(serverUrl, id)
            if (vm.isBusy) {
                delay(2000)
            }
        }
    }
}

private object Preferences {
    private const val PREFS = "insta_transcript"
    private const val SERVER = "server_url"

    fun getServerUrl(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(SERVER, "http://10.0.2.2:8000")
            ?: "http://10.0.2.2:8000"

    fun setServerUrl(context: Context, value: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(SERVER, value.trim().removeSuffix("/"))
            .apply()
    }
}
