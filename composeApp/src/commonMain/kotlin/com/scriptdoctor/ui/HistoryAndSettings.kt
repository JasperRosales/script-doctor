package com.scriptdoctor.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.scriptdoctor.AppState
import com.scriptdoctor.Screen
import com.scriptdoctor.api.ClaudeClient
import com.scriptdoctor.platform.formatTimestamp

@Composable
fun HistoryScreen(state: AppState, modifier: Modifier = Modifier) {
    var confirmClear by remember { mutableStateOf(false) }
    if (state.history.isEmpty()) {
        Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text("No decoded prescriptions yet.", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(state.history, key = { it.id }) { item ->
            Card(Modifier.fillMaxWidth().clickable { state.navigate(Screen.Result(item)) }) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        val names = item.transcription.medications.map { it.drugName.ifBlank { "?" } }
                        Text(names.joinToString(", ").ifBlank { "No medications" }, style = MaterialTheme.typography.titleMedium)
                        Text(formatTimestamp(item.createdAtEpochMs), style = MaterialTheme.typography.bodySmall)
                        Text(item.risk.label(), style = MaterialTheme.typography.labelMedium, color = item.risk.color())
                    }
                    IconButton(onClick = { state.deleteHistory(item.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            }
        }
        item {
            TextButton(onClick = { confirmClear = true }) { Text("Clear all history") }
        }
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear history?") },
            text = { Text("This removes all saved decodes from this device.") },
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
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Anthropic API key", style = MaterialTheme.typography.titleMedium)
        Text(
            "Script Doctor sends the prescription photo directly from this device to the Claude API. Create a key at console.anthropic.com.",
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedTextField(
            value = key,
            onValueChange = { key = it; saved = false },
            label = { Text("sk-ant-…") },
            singleLine = true,
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
            trailingIcon = {
                IconButton(onClick = { visible = !visible }) {
                    Icon(if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = "Toggle visibility")
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { state.saveApiKey(key); saved = true }) { Text("Save") }
            TextButton(onClick = { key = ""; state.saveApiKey(""); saved = true }) { Text("Remove") }
        }
        if (saved) Text("Saved.", color = MaterialTheme.colorScheme.primary)

        Text("Privacy", style = MaterialTheme.typography.titleMedium)
        Text(
            "Photos are sent to Anthropic only when you tap Decode and are never stored on this device. Decoded text is kept in local history until you delete it. The key is stored in app-private storage on this device.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text("Model: ${ClaudeClient.DEFAULT_MODEL}", style = MaterialTheme.typography.labelMedium)
    }
}
