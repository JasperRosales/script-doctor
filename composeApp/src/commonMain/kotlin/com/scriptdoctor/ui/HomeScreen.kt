package com.scriptdoctor.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.scriptdoctor.AppState
import com.scriptdoctor.Screen
import com.scriptdoctor.platform.rememberImagePicker
import com.scriptdoctor.platform.toImageBitmap

@Composable
fun HomeScreen(state: AppState, modifier: Modifier = Modifier) {
    val picker = rememberImagePicker(onImage = state::onImage, onError = { state.error = it })
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Hero()
        Column(
            Modifier.padding(horizontal = 20.dp).padding(top = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (!state.hasApiKey) SetupCard(onClick = { state.navigate(Screen.Settings) })

            val image = state.image
            when {
                image == null -> {
                    ScanCard(onCamera = picker.takePhoto, onGallery = picker.pickFromGallery)
                    HowItWorks()
                    SampleLink(onClick = state::loadSample)
                }
                else -> PhotoCard(state, image, onRetake = picker.takePhoto, onGallery = picker.pickFromGallery)
            }
            Disclaimer()
        }
    }
}

@Composable
private fun Hero() {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF00796B), Color(0xFF004D40))))
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 28.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Default.LocalPharmacy, contentDescription = null, tint = Color.White) }
                Spacer(Modifier.width(12.dp))
                Text("Script Doctor", style = MaterialTheme.typography.titleLarge, color = Color.White)
            }
            Spacer(Modifier.height(20.dp))
            Text("Can't read your\nprescription?", style = MaterialTheme.typography.headlineMedium, color = Color.White)
            Spacer(Modifier.height(8.dp))
            Text(
                "Snap a photo. We'll read the handwriting, explain it simply, and flag anything risky.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
            )
        }
    }
}

@Composable
private fun SetupCard(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBubble(Icons.Default.Key, MaterialTheme.colorScheme.tertiary)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Finish setup", style = MaterialTheme.typography.titleMedium)
                Text("Add a free Gemini API key to decode real prescriptions.", style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Open Settings")
        }
    }
}

@Composable
private fun ScanCard(onCamera: () -> Unit, onGallery: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Card(
        colors = CardDefaults.cardColors(containerColor = cs.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(cs.primaryContainer.copy(alpha = 0.45f))
                    .border(2.dp, cs.primary.copy(alpha = 0.35f), MaterialTheme.shapes.medium)
                    .clickable(onClick = onCamera),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(64.dp).clip(CircleShape).background(cs.primary), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = cs.onPrimary, modifier = Modifier.size(30.dp))
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("Tap to scan a prescription", style = MaterialTheme.typography.titleMedium, color = cs.onPrimaryContainer)
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = onCamera, modifier = Modifier.fillMaxWidth().height(54.dp)) {
                Icon(Icons.Default.PhotoCamera, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text("Take photo", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onGallery, modifier = Modifier.fillMaxWidth().height(54.dp)) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text("Choose from gallery", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun HowItWorks() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("How it works", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Step(Icons.Default.PhotoCamera, "1", "Snap", Modifier.weight(1f))
            Step(Icons.Default.TextSnippet, "2", "Read", Modifier.weight(1f))
            Step(Icons.Default.Shield, "3", "Check", Modifier.weight(1f))
        }
    }
}

@Composable
private fun Step(icon: ImageVector, n: String, label: String, modifier: Modifier) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier.clip(MaterialTheme.shapes.medium).background(cs.surfaceContainerLowest).padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconBubble(icon, cs.primary)
        Spacer(Modifier.height(8.dp))
        Text("$n. $label", style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun SampleLink(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBubble(Icons.Default.Science, MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Try a sample", style = MaterialTheme.typography.titleMedium)
                Text("See a decoded prescription without a key.", style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
        }
    }
}

@Composable
private fun PhotoCard(state: AppState, image: ByteArray, onRetake: () -> Unit, onGallery: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val bitmap = remember(image) { runCatching { image.toImageBitmap() }.getOrNull() }
    Card(
        colors = CardDefaults.cardColors(containerColor = cs.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.padding(16.dp)) {
            Box {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = "Selected prescription",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp).clip(MaterialTheme.shapes.medium),
                    )
                }
                if (!state.decoding) {
                    FilledIconButton(
                        onClick = state::clearImage,
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.Black.copy(alpha = 0.55f), contentColor = Color.White),
                    ) { Icon(Icons.Default.Close, contentDescription = "Remove image") }
                }
            }
            Spacer(Modifier.height(16.dp))
            if (state.decoding) {
                Text("Reading the handwriting…", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text("Checking each medication and dose. This can take up to a minute.", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape))
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = state::cancel, modifier = Modifier.align(Alignment.End)) { Text("Cancel") }
            } else {
                Button(
                    onClick = state::decode,
                    enabled = state.hasApiKey,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(),
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(Modifier.width(10.dp))
                    Text("Decode prescription", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onRetake, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp)); Text("Retake")
                    }
                    TextButton(onClick = onGallery, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp)); Text("Gallery")
                    }
                }
            }
        }
    }
}

@Composable
internal fun IconBubble(icon: ImageVector, tint: Color, size: Int = 40) {
    Box(Modifier.size(size.dp).clip(CircleShape).background(tint.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size((size * 0.55f).dp))
    }
}

@Composable
fun Disclaimer(modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.surfaceContainer).padding(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp).padding(top = 1.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            "Script Doctor is a reading aid, not a medical device. Always confirm with your doctor or a licensed pharmacist before taking or dispensing any medication.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
