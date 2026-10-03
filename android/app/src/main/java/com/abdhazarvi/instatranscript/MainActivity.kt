package com.abdhazarvi.instatranscript

import android.content.Intent
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abdhazarvi.instatranscript.ui.theme.InstaTranscriptTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            InstaTranscriptTheme { TranscriptScreen() }
        }
    }
}

@Composable
private fun TranscriptScreen(vm: TranscriptViewModel = viewModel()) {
    val context = LocalContext.current
    var url by rememberSaveable { mutableStateOf("") }
    var language by rememberSaveable { mutableStateOf("auto") }
    var loggedIn by remember { mutableStateOf(InstagramCookieStore.hasCookies(context)) }

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
            "Instagram → on-device transcript",
            style = MaterialTheme.typography.bodyMedium
        )

        val loggedIn = InstagramCookieStore.hasCookies(context)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    if (loggedIn) "Instagram session: saved on this phone"
                    else "Instagram session: not connected",
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Instagram may block anonymous downloads. Login once here if a Reel fails.",
                    style = MaterialTheme.typography.bodySmall
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            context.startActivity(
                                Intent(context, InstagramLoginActivity::class.java)
                            )
                            loggedIn = InstagramCookieStore.hasCookies(context)
                        },
                        enabled = !vm.isBusy
                    ) {
                        Text(if (loggedIn) "Re-login" else "Instagram Login")
                    }
                    if (loggedIn) {
                        TextButton(
                            onClick = {
                                InstagramCookieStore.clear(context)
                                loggedIn = false
                            }
                        ) {
                            Text("Clear")
                        }
                    }
                }
            }
        }

        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Instagram video / reel URL") }
        )

        Text("Spoken language", fontWeight = FontWeight.SemiBold)

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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.selectable(
                        selected = language == item.first,
                        onClick = { language = item.first },
                        role = Role.RadioButton
                    )
                ) {
                    RadioButton(selected = language == item.first, onClick = null)
                    Text(item.second)
                }
            }
        }

        Button(
            onClick = { vm.start(context, url, language) },
            enabled = !vm.isBusy && url.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (vm.isBusy) "Working…" else "Transcribe")
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
                    LinearProgressIndicator(
                        progress = { vm.progress / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "First use downloads the multilingual Whisper model once. Transcription itself runs locally."
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
                    TextButton(onClick = vm::clearError) { Text("Dismiss") }
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
                        "Language: " + (vm.detectedLanguage ?: "auto"),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Divider()
                    Text(resultText)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            vm.setExportType("txt")
                            saveLauncher.launch(vm.safeFilename("txt"))
                        }) { Text("TXT") }

                        Button(onClick = {
                            vm.setExportType("md")
                            saveLauncher.launch(vm.safeFilename("md"))
                        }) { Text("MD") }

                        Button(onClick = {
                            vm.setExportType("json")
                            saveLauncher.launch(vm.safeFilename("json"))
                        }) { Text("JSON") }
                    }
                }
            }
        }

        Text(
            "No backend URL or cloud transcription is required. Instagram cookies, when used, remain in app-private storage.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
