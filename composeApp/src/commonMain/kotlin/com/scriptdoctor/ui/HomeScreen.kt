package com.scriptdoctor.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.scriptdoctor.AppState
import com.scriptdoctor.Screen
import com.scriptdoctor.platform.rememberImagePicker
import com.scriptdoctor.platform.toImageBitmap

@Composable
fun HomeScreen(state: AppState, modifier: Modifier = Modifier) {
    val picker = rememberImagePicker(onImage = state::onImage, onError = { state.error = it })
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        val image = state.image
        if (image == null) {
            Intro()
        } else {
            val bitmap = remember(image) { runCatching { image.toImageBitmap() }.getOrNull() }
            Box {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = "Selected prescription",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp).clip(RoundedCornerShape(12.dp)),
                    )
                }
                if (!state.decoding) {
                    IconButton(onClick = state::clearImage, modifier = Modifier.align(Alignment.TopEnd)) {
                        Icon(Icons.Default.Close, contentDescription = "Remove image")
                    }
                }
            }
        }

        if (!state.hasApiKey) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Add an Anthropic API key to decode real prescriptions.", style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = { state.navigate(Screen.Settings) }) { Text("Open Settings") }
                }
            }
        }

        if (state.decoding) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator(Modifier.size(28.dp))
                Column(Modifier.weight(1f)) {
                    Text("Reading the handwriting…", style = MaterialTheme.typography.titleSmall)
                    Text("This can take up to a minute.", style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick = state::cancel) { Text("Cancel") }
            }
        } else if (image != null) {
            Button(onClick = state::decode, modifier = Modifier.fillMaxWidth(), enabled = state.hasApiKey) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Decode prescription")
            }
        }

        if (!state.decoding) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(onClick = picker.takePhoto, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (image == null) "Take photo" else "Retake")
                }
                FilledTonalButton(onClick = picker.pickFromGallery, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Gallery")
                }
            }
            OutlinedButton(onClick = state::loadSample, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Science, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Try a sample result")
            }
        }

        Disclaimer()
    }
}

@Composable
private fun Intro() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)) {
        Text("Decode a doctor's handwriting", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.size(8.dp))
        Text(
            "Photograph a handwritten prescription. Script Doctor transcribes each medication, explains the directions in plain language, and flags anything that could be misread.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun Disclaimer(modifier: Modifier = Modifier) {
    Text(
        "Script Doctor is a reading aid, not a medical device. Always confirm with the prescriber or a licensed pharmacist before taking or dispensing any medication.",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}
