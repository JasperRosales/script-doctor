package com.scriptdoctor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.scriptdoctor.AppState
import com.scriptdoctor.Screen
import com.scriptdoctor.api.GeminiClient
import com.scriptdoctor.platform.formatTimestamp

@Composable
fun HistoryScreen(state: AppState, modifier: Modifier = Modifier) {
    var confirmClear by remember { mutableStateOf(false) }
    if (state.history.isEmpty()) {
        Column(
            modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            IconBubble(Icons.Default.History, MaterialTheme.colorScheme.primary, size = 88)
            Spacer(Modifier.height(20.dp))
            Text("No scans yet", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(6.dp))
            Text(
                "Prescriptions you decode are saved here, on this device only.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            Button(onClick = { state.navigate(Screen.Home) }, modifier = Modifier.height(52.dp)) {
                Icon(Icons.Default.DocumentScanner, contentDescription = null)
                Spacer(Modifier.width(8.dp)); Text("Scan a prescription")
            }
        }
        return
    }
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(Modifier.padding(start = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${state.history.size} saved scan${if (state.history.size == 1) "" else "s"}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = { confirmClear = true }) { Text("Clear all") }
            }
        }
        items(state.history, key = { it.id }) { item ->
            Card(
                onClick = { state.navigate(Screen.Result(item)) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                shape = MaterialTheme.shapes.medium,
            ) {
                Row(Modifier.padding(start = 14.dp, top = 14.dp, bottom = 14.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconBubble(item.risk.icon(), item.risk.color(), size = 44)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        val names = item.transcription.medications.map { it.drugName.ifBlank { "?" } }
                        Text(names.joinToString(", ").ifBlank { "No medications" }, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            item.risk.label(),
                            style = MaterialTheme.typography.labelMedium,
                            color = item.risk.color(),
                        )
                        Text(formatTimestamp(item.createdAtEpochMs), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { state.deleteHistory(item.id) }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear history?") },
            text = { Text("This removes all saved scans from this device.") },
            confirmButton = { TextButton(onClick = { state.clearHistory(); confirmClear = false }) { Text("Clear") } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
        )
    }
}

@Composable
fun SettingsScreen(state: AppState, modifier: Modifier = Modifier) {
    var key by remember { mutableStateOf(state.currentApiKey()) }
    var visible by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }
    val uri = LocalUriHandler.current
    val cs = MaterialTheme.colorScheme
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SoftCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBubble(Icons.Default.Key, cs.primary, size = 44)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Gemini API key", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (state.hasApiKey) "Connected" else "Not set",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (state.hasApiKey) RiskLevelColors.ok else cs.onSurfaceVariant,
                    )
                }
                if (state.hasApiKey) Icon(Icons.Default.CheckCircle, contentDescription = null, tint = RiskLevelColors.ok)
            }
            Text(
                "Script Doctor uses Google's Gemini to read handwriting. Keys are free: sign in, create one, and paste it here.",
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant,
            )
            OutlinedButton(onClick = { uri.openUri("https://aistudio.google.com/apikey") }, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                Text("Get a free key")
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
            }
            OutlinedTextField(
                value = key,
                onValueChange = { key = it; saved = false },
                label = { Text("Paste key (AIza…)") },
                singleLine = true,
                shape = MaterialTheme.shapes.small,
                visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
                trailingIcon = {
                    IconButton(onClick = { visible = !visible }) {
                        Icon(if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = "Toggle visibility")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { state.saveApiKey(key); saved = true }, modifier = Modifier.weight(1f).height(50.dp)) { Text("Save key") }
                if (state.hasApiKey) {
                    TextButton(onClick = { key = ""; state.saveApiKey(""); saved = true }) { Text("Remove") }
                }
            }
            if (saved) Text(if (state.hasApiKey) "Saved. You're ready to scan." else "Key removed.", color = cs.primary, style = MaterialTheme.typography.bodyMedium)
        }

        SoftCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBubble(Icons.Default.Lock, cs.secondary, size = 44)
                Spacer(Modifier.width(12.dp))
                Text("Privacy", style = MaterialTheme.typography.titleMedium)
            }
            PrivacyLine("Photos go to Google only when you tap Decode, and are never saved on this phone.")
            PrivacyLine("On Gemini's free tier, Google may use what you send to improve its products. Use a paid-tier key for real patient prescriptions.")
            PrivacyLine("Results and your key stay in this app's private storage.")
        }

        SoftCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBubble(Icons.Default.Info, cs.tertiary, size = 44)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("About", style = MaterialTheme.typography.titleMedium)
                    Text("Model: ${GeminiClient.DEFAULT_MODEL}", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                }
            }
            Disclaimer()
        }
    }
}

@Composable
private fun PrivacyLine(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(Modifier.padding(top = 7.dp).size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

private object RiskLevelColors {
    val ok = com.scriptdoctor.model.RiskLevel.LOW.color()
}
