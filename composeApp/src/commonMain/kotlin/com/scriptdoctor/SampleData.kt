package com.scriptdoctor

import com.scriptdoctor.model.Confidence
import com.scriptdoctor.model.Legibility
import com.scriptdoctor.model.Medication
import com.scriptdoctor.model.Transcription

/** A canned, fictional result so the app can be demoed without an API key or a real prescription. */
object SampleData {
    val transcription = Transcription(
        isPrescription = true,
        legibility = Legibility.PARTIALLY_LEGIBLE,
        patientName = "Juan Dela Cruz (sample)",
        prescriberName = "Dr. M. Santos",
        date = "2026-10-28",
        medications = listOf(
            Medication(
                rawText = "Celebrex 200mg cap 1 QD x 14d #14",
                drugName = "Celebrex",
                strength = "200 mg",
                dosageForm = "capsule",
                route = "oral",
                frequency = "QD",
                duration = "14 days",
                quantity = "14",
                instructionsPlain = "Take one 200 mg capsule by mouth once a day for 14 days.",
                confidence = Confidence.MEDIUM,
                alternativeReadings = listOf("Celexa"),
                concerns = listOf("The first letters are cramped; \"Celexa\" (an antidepressant) is a plausible reading."),
            ),
            Medication(
                rawText = "Metformin 500mg tab BID c meals",
                drugName = "Metformin",
                strength = "500 mg",
                dosageForm = "tablet",
                route = "oral",
                frequency = "twice daily with meals",
                duration = "",
                quantity = "",
                instructionsPlain = "Take one 500 mg tablet by mouth twice a day with meals.",
                confidence = Confidence.HIGH,
                alternativeReadings = emptyList(),
                concerns = listOf("No duration or quantity written."),
            ),
            Medication(
                rawText = "Insulin glargine 10U SC HS",
                drugName = "Insulin glargine",
                strength = "10 units",
                dosageForm = "injection",
                route = "subcutaneous",
                frequency = "at bedtime",
                duration = "",
                quantity = "",
                instructionsPlain = "Inject 10 units under the skin at bedtime.",
                confidence = Confidence.LOW,
                alternativeReadings = emptyList(),
                concerns = listOf("\"10U\" could be read as \"100\". Confirm the dose with the prescriber."),
            ),
        ),
        generalNotes = listOf("Prescriber signature present; license number partially obscured."),
    )
}
