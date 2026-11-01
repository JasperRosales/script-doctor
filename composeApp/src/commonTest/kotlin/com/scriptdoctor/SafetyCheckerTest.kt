package com.scriptdoctor

import com.scriptdoctor.model.Confidence
import com.scriptdoctor.model.FlagKind
import com.scriptdoctor.model.Legibility
import com.scriptdoctor.model.Medication
import com.scriptdoctor.model.RiskLevel
import com.scriptdoctor.model.Transcription
import com.scriptdoctor.safety.SafetyChecker
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SafetyCheckerTest {

    private fun med(
        raw: String = "",
        name: String = "Amoxicillin",
        strength: String = "500 mg",
        frequency: String = "three times daily",
        confidence: Confidence = Confidence.HIGH,
        alternatives: List<String> = emptyList(),
    ) = Medication(raw, name, strength, "capsule", "oral", frequency, "7 days", "21", "", confidence, alternatives, emptyList())

    private fun rx(vararg meds: Medication, legibility: Legibility = Legibility.CLEAR) =
        Transcription(true, legibility, "", "", "", meds.toList(), emptyList())

    private fun flagsFor(m: Medication) = SafetyChecker.check(rx(m))

    @Test
    fun cleanPrescriptionHasNoFlags() {
        val flags = flagsFor(med(raw = "Amoxicillin 500 mg cap TID x 7 days #21"))
        assertTrue(flags.isEmpty(), "unexpected: $flags")
        assertEquals(RiskLevel.LOW, SafetyChecker.overallRisk(flags))
    }

    @Test
    fun detectsQd() {
        val flags = flagsFor(med(raw = "Lisinopril 10mg QD", name = "Lisinopril", frequency = "QD"))
        assertTrue(flags.any { it.kind == FlagKind.ERROR_PRONE_ABBREVIATION && "QD" in it.message })
    }

    @Test
    fun qidIsNotFlaggedAsQd() {
        val flags = flagsFor(med(raw = "Amoxicillin 500mg QID", frequency = "QID"))
        assertTrue(flags.none { it.kind == FlagKind.ERROR_PRONE_ABBREVIATION }, "unexpected: $flags")
    }

    @Test
    fun detectsUnitsAbbreviation() {
        val flags = flagsFor(med(raw = "Insulin 10U SC", name = "Insulin", strength = "10 units"))
        assertTrue(flags.any { "units" in it.message && it.kind == FlagKind.ERROR_PRONE_ABBREVIATION })
        assertTrue(flags.any { "subcut" in it.message })
    }

    @Test
    fun detectsTrailingZeroAndNakedDecimal() {
        val trailing = flagsFor(med(raw = "Warfarin 1.0 mg daily", name = "Warfarin", strength = "1.0 mg"))
        assertTrue(trailing.any { it.kind == FlagKind.DOSE_FORMAT && it.severity == RiskLevel.HIGH })
        val naked = flagsFor(med(raw = "Colchicine .5 mg", name = "Colchicine", strength = ".5 mg"))
        assertTrue(naked.any { it.kind == FlagKind.DOSE_FORMAT && "leading zero" in it.message })
        val fine = flagsFor(med(raw = "Colchicine 0.5 mg", strength = "0.5 mg"))
        assertTrue(fine.none { it.kind == FlagKind.DOSE_FORMAT })
    }

    @Test
    fun detectsLookAlikeDrug() {
        val flags = flagsFor(med(name = "Hydroxyzine"))
        assertTrue(flags.any { it.kind == FlagKind.LOOK_ALIKE_SOUND_ALIKE && "Hydralazine" in it.message })
    }

    @Test
    fun modelAlternativesAreHighRisk() {
        val flags = flagsFor(med(name = "Celebrex", alternatives = listOf("Celexa")))
        assertEquals(RiskLevel.HIGH, SafetyChecker.overallRisk(flags))
    }

    @Test
    fun lowConfidenceIsHighRisk() {
        val flags = flagsFor(med(confidence = Confidence.LOW))
        assertEquals(RiskLevel.HIGH, SafetyChecker.overallRisk(flags))
    }

    @Test
    fun highAlertDrugWithMediumConfidenceIsHighRisk() {
        val flags = flagsFor(med(name = "Warfarin", confidence = Confidence.MEDIUM))
        assertEquals(RiskLevel.HIGH, SafetyChecker.overallRisk(flags))
    }

    @Test
    fun illegibleDocumentIsHighRisk() {
        val flags = SafetyChecker.check(rx(legibility = Legibility.ILLEGIBLE))
        assertEquals(RiskLevel.HIGH, SafetyChecker.overallRisk(flags))
    }

    @Test
    fun missingStrengthIsFlagged() {
        val flags = flagsFor(med(strength = ""))
        assertTrue(flags.any { it.kind == FlagKind.MISSING_FIELD })
    }

    @Test
    fun sampleDataProducesHighRisk() {
        val flags = SafetyChecker.check(SampleData.transcription)
        assertEquals(RiskLevel.HIGH, SafetyChecker.overallRisk(flags))
    }
}
