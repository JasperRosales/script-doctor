package com.scriptdoctor.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** How readable the source image was, as judged by the model. */
@Serializable
enum class Legibility {
    @SerialName("clear") CLEAR,
    @SerialName("partially_legible") PARTIALLY_LEGIBLE,
    @SerialName("illegible") ILLEGIBLE,
}

@Serializable
enum class Confidence {
    @SerialName("high") HIGH,
    @SerialName("medium") MEDIUM,
    @SerialName("low") LOW,
}

/** One medication line decoded from the prescription. Field names mirror the JSON schema sent to Claude. */
@Serializable
data class Medication(
    @SerialName("raw_text") val rawText: String,
    @SerialName("drug_name") val drugName: String,
    val strength: String,
    @SerialName("dosage_form") val dosageForm: String,
    val route: String,
    val frequency: String,
    val duration: String,
    val quantity: String,
    @SerialName("instructions_plain") val instructionsPlain: String,
    val confidence: Confidence,
    @SerialName("alternative_readings") val alternativeReadings: List<String>,
    val concerns: List<String>,
)

/** Structured transcription returned by Claude. */
@Serializable
data class Transcription(
    @SerialName("is_prescription") val isPrescription: Boolean,
    val legibility: Legibility,
    @SerialName("patient_name") val patientName: String,
    @SerialName("prescriber_name") val prescriberName: String,
    val date: String,
    val medications: List<Medication>,
    @SerialName("general_notes") val generalNotes: List<String>,
)

enum class RiskLevel { LOW, MEDIUM, HIGH }

enum class FlagKind { ERROR_PRONE_ABBREVIATION, LOOK_ALIKE_SOUND_ALIKE, LOW_CONFIDENCE, DOSE_FORMAT, ILLEGIBLE, MISSING_FIELD }

/** A deterministic, locally computed safety finding. */
@Serializable
data class SafetyFlag(
    val kind: FlagKind,
    val severity: RiskLevel,
    val message: String,
    /** Index into [Transcription.medications], or -1 for document-level flags. */
    val medicationIndex: Int = -1,
)

/** A complete decode: model output plus local safety analysis. */
@Serializable
data class DecodeResult(
    val id: String,
    val createdAtEpochMs: Long,
    val transcription: Transcription,
    val flags: List<SafetyFlag>,
    val risk: RiskLevel,
    val model: String,
    val isSample: Boolean = false,
)
