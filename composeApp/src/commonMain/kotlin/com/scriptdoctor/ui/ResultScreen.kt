package com.scriptdoctor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.scriptdoctor.model.Confidence
import com.scriptdoctor.model.DecodeResult
import com.scriptdoctor.model.Medication
import com.scriptdoctor.model.RiskLevel
import com.scriptdoctor.model.SafetyFlag
import com.scriptdoctor.platform.formatTimestamp

internal fun RiskLevel.color(): Color = when (this) {
    RiskLevel.LOW -> Color(0xFF2E7D32)
    RiskLevel.MEDIUM -> Color(0xFFE65100)
    RiskLevel.HIGH -> Color(0xFFC62828)
}

internal fun RiskLevel.label(): String = when (this) {
    RiskLevel.LOW -> "Low risk"
    RiskLevel.MEDIUM -> "Verify before use"
    RiskLevel.HIGH -> "High risk"
}

internal fun RiskLevel.advice(): String = when (this) {
    RiskLevel.LOW -> "Nothing unusual found. Still check with your pharmacist."
    RiskLevel.MEDIUM -> "Some parts need a second look. Ask your pharmacist to confirm."
    RiskLevel.HIGH -> "Don't take or dispense yet. Confirm with the prescriber first."
}

internal fun RiskLevel.icon(): ImageVector = when (this) {
    RiskLevel.LOW -> Icons.Default.CheckCircle
    RiskLevel.MEDIUM -> Icons.Default.Warning
    RiskLevel.HIGH -> Icons.Default.Error
}

@Composable
fun ResultScreen(result: DecodeResult, modifier: Modifier = Modifier) {
    val t = result.transcription
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { RiskHero(result) }
        item { DetailsCard(result) }
        if (t.medications.isNotEmpty()) {
            item {
                Text(
                    "Medications (${t.medications.size})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 4.dp, top = 6.dp),
                )
            }
        } else {
            item {
                SoftCard {
                    Text(
                        if (t.isPrescription) "No medications could be read." else "This doesn't look like a prescription.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
        itemsIndexed(t.medications) { i, med ->
            MedicationCard(i, med, result.flags.filter { it.medicationIndex == i })
        }
        val docFlags = result.flags.filter { it.medicationIndex < 0 }
        if (docFlags.isNotEmpty() || t.generalNotes.isNotEmpty()) {
            item {
                SoftCard {
                    Text("Notes", style = MaterialTheme.typography.titleMedium)
                    docFlags.forEach { FlagRow(it) }
                    t.generalNotes.forEach { Bullet(it) }
                }
            }
        }
        item { Disclaimer() }
    }
}

@Composable
private fun RiskHero(result: DecodeResult) {
    val c = result.risk.color()
    Card(
        colors = CardDefaults.cardColors(containerColor = c),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                    Icon(result.risk.icon(), contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(result.risk.label(), style = MaterialTheme.typography.headlineSmall, color = Color.White)
                    val n = result.flags.size
                    Text(
                        if (n == 0) "No safety flags" else "$n safety flag${if (n == 1) "" else "s"} found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                result.risk.advice(),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).background(Color.Black.copy(alpha = 0.15f)).padding(12.dp),
            )
        }
    }
}

@Composable
private fun DetailsCard(result: DecodeResult) {
    val t = result.transcription
    SoftCard {
        if (result.isSample) {
            Text(
                "SAMPLE · fictional data",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.tertiaryContainer).padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
        InfoRow(Icons.Default.Person, "Patient", t.patientName)
        InfoRow(Icons.Default.MedicalServices, "Prescriber", t.prescriberName)
        InfoRow(Icons.Default.CalendarToday, "Date written", t.date)
        InfoRow(Icons.Default.Visibility, "Handwriting", t.legibility.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() })
        Text(
            "Decoded ${formatTimestamp(result.createdAtEpochMs)}" + if (!result.isSample) " · ${result.model}" else "",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    if (value.isBlank()) return
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconBubble(icon, MaterialTheme.colorScheme.primary, size = 34)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MedicationCard(index: Int, med: Medication, flags: List<SafetyFlag>) {
    val cs = MaterialTheme.colorScheme
    SoftCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBubble(Icons.Default.Medication, cs.primary, size = 44)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(med.drugName.ifBlank { "Unreadable drug name" }, style = MaterialTheme.typography.titleLarge)
                Text(
                    listOf(med.strength, med.dosageForm).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "Medication ${index + 1}" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurfaceVariant,
                )
            }
            ConfidenceBadge(med.confidence)
        }

        if (med.alternativeReadings.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Could also be:", style = MaterialTheme.typography.labelLarge, color = RiskLevel.MEDIUM.color(), modifier = Modifier.padding(vertical = 4.dp))
                med.alternativeReadings.forEach { Pill(it, RiskLevel.MEDIUM.color()) }
            }
        }

        if (med.instructionsPlain.isNotBlank()) {
            Column(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).background(cs.primaryContainer.copy(alpha = 0.5f)).padding(14.dp)) {
                Text("HOW TO TAKE", style = MaterialTheme.typography.labelSmall, color = cs.primary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(med.instructionsPlain, style = MaterialTheme.typography.bodyLarge, color = cs.onPrimaryContainer)
            }
        }

        val stats = listOf("Route" to med.route, "How often" to med.frequency, "For" to med.duration, "Quantity" to med.quantity)
            .filter { it.second.isNotBlank() }
        if (stats.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                stats.forEach { (k, v) ->
                    Column(Modifier.clip(MaterialTheme.shapes.small).background(cs.surfaceContainer).padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text(k, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                        Text(v, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        if (med.rawText.isNotBlank()) {
            Column {
                Text("As written", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                Text(med.rawText, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
            }
        }

        if (med.concerns.isNotEmpty() || flags.isNotEmpty()) {
            HorizontalDivider(color = cs.outlineVariant)
            Text("Things to check", style = MaterialTheme.typography.labelLarge)
            flags.sortedByDescending { it.severity }.forEach { FlagRow(it) }
            med.concerns.forEach { NoteRow(it) }
        }
    }
}

@Composable
private fun ConfidenceBadge(confidence: Confidence) {
    val (text, risk) = when (confidence) {
        Confidence.HIGH -> "Clear" to RiskLevel.LOW
        Confidence.MEDIUM -> "Unsure" to RiskLevel.MEDIUM
        Confidence.LOW -> "Hard to read" to RiskLevel.HIGH
    }
    Pill(text, risk.color())
}

@Composable
private fun Pill(text: String, color: Color) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = color,
        modifier = Modifier.clip(CircleShape).background(color.copy(alpha = 0.12f)).padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

@Composable
private fun FlagRow(flag: SafetyFlag) {
    val c = flag.severity.color()
    Row(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).background(c.copy(alpha = 0.08f)).padding(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(flag.severity.icon(), contentDescription = flag.severity.name, tint = c, modifier = Modifier.size(18.dp).padding(top = 1.dp))
        Spacer(Modifier.width(10.dp))
        Text(flag.message, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun NoteRow(text: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp), verticalAlignment = Alignment.Top) {
        Icon(Icons.Default.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp).padding(top = 1.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Bullet(text: String) {
    Row(Modifier.padding(start = 4.dp)) {
        Text("•", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
internal fun SoftCard(content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { content() }
    }
}
