package com.scriptdoctor.safety

import com.scriptdoctor.model.Confidence
import com.scriptdoctor.model.FlagKind
import com.scriptdoctor.model.Legibility
import com.scriptdoctor.model.Medication
import com.scriptdoctor.model.RiskLevel
import com.scriptdoctor.model.SafetyFlag
import com.scriptdoctor.model.Transcription

/**
 * Deterministic safety rules applied on-device after the model returns.
 *
 * The model reads the handwriting; these rules do not depend on the model and catch
 * well-known prescribing error patterns (based on the ISMP list of error-prone
 * abbreviations and common look-alike/sound-alike drug pairs).
 */
object SafetyChecker {

    private data class AbbrevRule(val pattern: Regex, val message: String)

    private fun word(token: String) = Regex("""(?<![A-Za-z])$token(?![A-Za-z])""")

    private val abbreviationRules = listOf(
        AbbrevRule(word("""Q\.?D\.?"""), "\"QD\" (daily) is easily misread as \"QID\" (four times daily). Write \"daily\"."),
        AbbrevRule(word("""Q\.?O\.?D\.?"""), "\"QOD\" (every other day) is easily misread as \"QD\" or \"QID\". Write \"every other day\"."),
        AbbrevRule(Regex("""\d\s*(U|IU)(?![A-Za-z])"""), "\"U\"/\"IU\" for units can be misread as 0, 4, or \"IV\". Write \"units\"."),
        AbbrevRule(Regex("""(?i)\d\s*cc(?![A-Za-z])"""), "\"cc\" can be misread as \"U\" (units). Write \"mL\"."),
        AbbrevRule(Regex("""\d\s*(µg|μg|ug)(?![A-Za-z])"""), "\"µg\" can be misread as \"mg\" (1000x overdose). Write \"mcg\"."),
        AbbrevRule(word("""(SC|SQ|sub\s?q)"""), "\"SC\"/\"SQ\" can be misread as \"SL\" or \"5 every\". Write \"subcut\"."),
        AbbrevRule(word("""H\.?S\.?"""), "\"HS\" is ambiguous (half-strength vs. bedtime). Write \"bedtime\" or \"half-strength\"."),
        AbbrevRule(word("""T\.?I\.?W\.?"""), "\"TIW\" (three times a week) is misread as \"three times a day\". Write it out."),
        AbbrevRule(word("""D/C"""), "\"D/C\" can mean discharge or discontinue. Write the intended word."),
        AbbrevRule(word("""(AD|AS|AU|OD|OS|OU)"""), "Ear/eye abbreviations (AD/AS/AU/OD/OS/OU) are frequently confused. Write \"right ear\", \"left eye\", etc."),
        AbbrevRule(word("""(MS|MSO4|MgSO4)"""), "\"MS\"/\"MSO4\"/\"MgSO4\" can mean morphine or magnesium sulfate. Write the full drug name."),
        AbbrevRule(word("""(HCT|HCTZ)"""), "\"HCT\"/\"HCTZ\" can be confused with hydrocortisone. Write \"hydrochlorothiazide\"."),
        AbbrevRule(word("""(AZT|ZnSO4|PTU|MTX|CPZ|HCl)"""), "Drug-name abbreviations are error-prone. Write the full drug name."),
        AbbrevRule(word("""(>|<)"""), "\">\"/\"<\" symbols are often read as the opposite. Write \"greater than\"/\"less than\"."),
    )

    private val trailingZero = Regex("""(?<![\d.])\d+\.0(?!\d)\s*(mg|mcg|g|mL|units?)""", RegexOption.IGNORE_CASE)
    private val nakedDecimal = Regex("""(?<![\d.])\.\d+\s*(mg|mcg|g|mL|units?)""", RegexOption.IGNORE_CASE)

    /** Look-alike/sound-alike groups; any member decoded triggers a check against the others. */
    internal val lookAlikeGroups: List<Set<String>> = listOf(
        setOf("hydroxyzine", "hydralazine"),
        setOf("celebrex", "celexa", "cerebyx"),
        setOf("lamictal", "lamisil"),
        setOf("zyrtec", "zyprexa"),
        setOf("clonidine", "klonopin", "clonazepam"),
        setOf("metformin", "metronidazole"),
        setOf("tramadol", "trazodone"),
        setOf("losartan", "valsartan"),
        setOf("glipizide", "glyburide", "glimepiride"),
        setOf("prednisone", "prednisolone"),
        setOf("hydrocodone", "oxycodone"),
        setOf("morphine", "hydromorphone"),
        setOf("risperidone", "ropinirole"),
        setOf("sitagliptin", "sumatriptan"),
        setOf("carbamazepine", "oxcarbazepine"),
        setOf("amlodipine", "amiodarone"),
        setOf("lisinopril", "fosinopril"),
        setOf("paroxetine", "fluoxetine", "duloxetine"),
        setOf("quinine", "quinidine"),
        setOf("novolog", "novolin"),
        setOf("humalog", "humulin"),
        setOf("methotrexate", "metolazone"),
        setOf("chlorpromazine", "chlorpropamide"),
        setOf("cefuroxime", "cefotaxime", "ceftriaxone"),
        setOf("dopamine", "dobutamine"),
        setOf("vinblastine", "vincristine"),
        setOf("labetalol", "lamotrigine"),
        setOf("levetiracetam", "levofloxacin"),
        setOf("tizanidine", "tiagabine"),
        setOf("bupropion", "buspirone"),
    )

    /** Drug names with a narrow therapeutic index or high-alert status, where a misread is especially dangerous. */
    internal val highAlertDrugs = setOf(
        "insulin", "warfarin", "heparin", "enoxaparin", "methotrexate", "digoxin", "morphine",
        "hydromorphone", "fentanyl", "oxycodone", "lithium", "phenytoin", "carbamazepine",
        "amiodarone", "potassium chloride", "colchicine", "levothyroxine", "tacrolimus",
    )

    fun check(t: Transcription): List<SafetyFlag> {
        val flags = mutableListOf<SafetyFlag>()

        if (!t.isPrescription) {
            flags += SafetyFlag(FlagKind.ILLEGIBLE, RiskLevel.HIGH, "This image does not appear to be a prescription.")
        }
        when (t.legibility) {
            Legibility.ILLEGIBLE -> flags += SafetyFlag(FlagKind.ILLEGIBLE, RiskLevel.HIGH, "The prescription is largely illegible. Contact the prescriber before dispensing.")
            Legibility.PARTIALLY_LEGIBLE -> flags += SafetyFlag(FlagKind.ILLEGIBLE, RiskLevel.MEDIUM, "Parts of the prescription are hard to read. Verify uncertain fields.")
            Legibility.CLEAR -> Unit
        }

        t.medications.forEachIndexed { i, med -> flags += checkMedication(i, med) }
        return flags.distinct()
    }

    private fun checkMedication(index: Int, med: Medication): List<SafetyFlag> {
        val flags = mutableListOf<SafetyFlag>()
        val text = listOf(med.rawText, med.strength, med.frequency, med.route, med.duration, med.quantity).joinToString(" | ")
        val name = med.drugName.lowercase().trim()
        val label = med.drugName.ifBlank { "Medication ${index + 1}" }

        for (rule in abbreviationRules) {
            if (rule.pattern.containsMatchIn(text)) {
                flags += SafetyFlag(FlagKind.ERROR_PRONE_ABBREVIATION, RiskLevel.MEDIUM, "$label: ${rule.message}", index)
            }
        }
        if (trailingZero.containsMatchIn(text)) {
            flags += SafetyFlag(FlagKind.DOSE_FORMAT, RiskLevel.HIGH, "$label: trailing zero (e.g. \"1.0 mg\") can be misread as 10x the dose.", index)
        }
        if (nakedDecimal.containsMatchIn(text)) {
            flags += SafetyFlag(FlagKind.DOSE_FORMAT, RiskLevel.HIGH, "$label: missing leading zero (e.g. \".5 mg\") can be misread as 10x the dose.", index)
        }

        val candidates = (listOf(name) + med.alternativeReadings.map { it.lowercase() })
        for (group in lookAlikeGroups) {
            val hit = group.firstOrNull { drug -> candidates.any { it.contains(drug) } } ?: continue
            val others = group - hit
            flags += SafetyFlag(
                FlagKind.LOOK_ALIKE_SOUND_ALIKE,
                RiskLevel.MEDIUM,
                "$label: \"${hit.cap()}\" is a known look-alike/sound-alike of ${others.joinToString(" / ") { it.cap() }}. Confirm the intended drug.",
                index,
            )
        }

        val altNames = med.alternativeReadings.filter { it.isNotBlank() && !it.equals(med.drugName, ignoreCase = true) }
        if (altNames.isNotEmpty()) {
            flags += SafetyFlag(FlagKind.LOOK_ALIKE_SOUND_ALIKE, RiskLevel.HIGH, "$label: the handwriting could also read as ${altNames.joinToString(", ")}.", index)
        }

        when (med.confidence) {
            Confidence.LOW -> flags += SafetyFlag(FlagKind.LOW_CONFIDENCE, RiskLevel.HIGH, "$label: low reading confidence. Do not dispense without confirming with the prescriber.", index)
            Confidence.MEDIUM -> flags += SafetyFlag(FlagKind.LOW_CONFIDENCE, RiskLevel.MEDIUM, "$label: moderate reading confidence. Double-check against the original.", index)
            Confidence.HIGH -> Unit
        }

        if (highAlertDrugs.any { name.contains(it) } && med.confidence != Confidence.HIGH) {
            flags += SafetyFlag(FlagKind.LOW_CONFIDENCE, RiskLevel.HIGH, "$label is a high-alert medication read with less than high confidence.", index)
        }

        val missing = buildList {
            if (med.strength.isBlank()) add("strength")
            if (med.frequency.isBlank()) add("frequency")
        }
        if (missing.isNotEmpty()) {
            flags += SafetyFlag(FlagKind.MISSING_FIELD, RiskLevel.MEDIUM, "$label: no ${missing.joinToString(" or ")} could be read.", index)
        }
        return flags
    }

    fun overallRisk(flags: List<SafetyFlag>): RiskLevel = flags.maxOfOrNull { it.severity } ?: RiskLevel.LOW

    private fun String.cap() = replaceFirstChar { it.uppercase() }
}
