package com.scriptdoctor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    RiskLevel.MEDIUM -> Color(0xFFEF6C00)
    RiskLevel.HIGH -> Color(0xFFC62828)
}

internal fun RiskLevel.label(): String = when (this) {
    RiskLevel.LOW -> "Low risk"
    RiskLevel.MEDIUM -> "Verify before use"
    RiskLevel.HIGH -> "High risk — confirm with prescriber"
}

@Composable
fun ResultScreen(result: DecodeResult, modifier: Modifier = Modifier) {
    val t = result.transcription
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { RiskBanner(result) }
        item { HeaderCard(result) }
        if (t.medications.isEmpty()) {
            item {
                Text(
                    if (t.isPrescription) "No medications could be read." else "This doesn't look like a prescription.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        itemsIndexed(t.medications) { i, med ->
            MedicationCard(i, med, result.flags.filter { it.medicationIndex == i })
        }
        val docFlags = result.flags.filter { it.medicationIndex < 0 }
        if (docFlags.isNotEmpty() || t.generalNotes.isNotEmpty()) {
            item {
                Card {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Notes", style = MaterialTheme.typography.titleMedium)
                        docFlags.forEach { FlagRow(it) }
                        t.generalNotes.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
                    }
                }
            }
        }
        item { Disclaimer(Modifier.padding(top = 8.dp)) }
    }
}

@Composable
private fun RiskBanner(result: DecodeResult) {
    val c = result.risk.color()
    Card(colors = CardDefaults.cardColors(containerColor = c.copy(alpha = 0.12f))) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                when (result.risk) {
                    RiskLevel.LOW -> Icons.Default.CheckCircle
                    RiskLevel.MEDIUM -> Icons.Default.Warning
                    RiskLevel.HIGH -> Icons.Default.Error
                },
                contentDescription = null,
                tint = c,
                modifier = Modifier.size(32.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(result.risk.label(), style = MaterialTheme.typography.titleMedium, color = c, fontWeight = FontWeight.Bold)
                val n = result.flags.size
                Text(
                    if (n == 0) "No safety flags raised." else "$n safety flag${if (n == 1) "" else "s"} raised.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun HeaderCard(result: DecodeResult) {
    val t = result.transcription
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (result.isSample) {
                Text("SAMPLE — fictional data", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
            }
            LabeledValue("Patient", t.patientName)
            LabeledValue("Prescriber", t.prescriberName)
            LabeledValue("Date written", t.date)
            LabeledValue("Legibility", t.legibility.name.lowercase().replace('_', ' '))
            Text(
                "Decoded ${formatTimestamp(result.createdAtEpochMs)}" + if (!result.isSample) " · ${result.model}" else "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MedicationCard(index: Int, med: Medication, flags: List<SafetyFlag>) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${index + 1}. ${med.drugName.ifBlank { "Unreadable drug name" }}",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                ConfidenceChip(med.confidence)
            }
            if (med.instructionsPlain.isNotBlank()) {
                Text(med.instructionsPlain, style = MaterialTheme.typography.bodyLarge)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LabeledValue("Strength", med.strength)
                LabeledValue("Form", med.dosageForm)
                LabeledValue("Route", med.route)
                LabeledValue("Frequency", med.frequency)
                LabeledValue("Duration", med.duration)
                LabeledValue("Quantity", med.quantity)
            }
            if (med.rawText.isNotBlank()) {
                Text("As written: ${med.rawText}", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
            }
            if (med.concerns.isNotEmpty()) {
                HorizontalDivider()
                Text("Reader notes", style = MaterialTheme.typography.labelLarge)
                med.concerns.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
            }
            if (flags.isNotEmpty()) {
                HorizontalDivider()
                Text("Safety checks", style = MaterialTheme.typography.labelLarge)
                flags.sortedByDescending { it.severity }.forEach { FlagRow(it) }
            }
        }
    }
}

@Composable
private fun ConfidenceChip(confidence: Confidence) {
    val (text, risk) = when (confidence) {
        Confidence.HIGH -> "High confidence" to RiskLevel.LOW
        Confidence.MEDIUM -> "Medium confidence" to RiskLevel.MEDIUM
        Confidence.LOW -> "Low confidence" to RiskLevel.HIGH
    }
    AssistChip(onClick = {}, label = { Text(text, color = risk.color()) })
}

@Composable
private fun FlagRow(flag: SafetyFlag) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            if (flag.severity == RiskLevel.HIGH) Icons.Default.Error else Icons.Default.Warning,
            contentDescription = flag.severity.name,
            tint = flag.severity.color(),
            modifier = Modifier.size(18.dp).padding(top = 2.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(flag.message, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun LabeledValue(label: String, value: String) {
    if (value.isBlank()) return
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
