# Features

Script Doctor is an Android prescription-reading aid built for DSH Hacks V2. A user photographs or selects an image of a handwritten prescription. The app sends a compressed image to Google's Gemini Interactions API, displays the structured transcription and plain-language directions, and applies local rules to highlight selected reading and prescribing hazards. A fictional sample demonstrates the result without an API key or network request.

**Clinical boundary:** This is a reading aid, not a medical device or a substitute for a prescriber or licensed pharmacist. The model can misread or omit information; a low risk result does not establish that a prescription, dose, interaction, or patient-specific treatment is safe. Confirm the original prescription with a qualified professional before taking or dispensing medication.

### Implemented capabilities

- Camera capture through an external camera application or selection through the Android system photo picker.
- Image orientation correction, downsampling, scaling, and JPEG encoding before upload.
- Gemini transcription of prescription metadata and medication lines, including confidence, alternative readings, and model-generated concerns.
- Deterministic local checks for selected error-prone abbreviations, dose formats, look-alike/sound-alike names, uncertainty, missing fields, and legibility.
- A result screen with a risk banner, medication details, model concerns, and local flags.
- Local scan history with individual deletion and clear-all; an API key can be saved or removed in Settings.
- Light and dark themes following the system setting.

## Safety checks

`SafetyChecker` runs after a successful transcription and does not make a network call. It works on the model's text, not the original image, so a model omission or misreading can also prevent a rule from triggering. Its lists and patterns are finite and are not a comprehensive clinical decision-support system.

| Condition | Severity |
| --- | --- |
| Image marked not a prescription or largely illegible | HIGH |
| Partially legible image | MEDIUM |
| Selected error-prone abbreviations in medication text | MEDIUM |
| Trailing zero or missing leading zero with recognized dose units | HIGH |
| A name matching a configured look-alike/sound-alike group | MEDIUM |
| Nonblank alternative reading different from the drug name | HIGH |
| Low / medium medication confidence | HIGH / MEDIUM |
| Configured high-alert drug with less than high confidence | HIGH |
| Missing strength or frequency | MEDIUM |

The abbreviation patterns cover selected forms of QD, QOD, U/IU, cc, microgram abbreviations, SC/SQ, HS, TIW, D/C, eye/ear abbreviations, and selected drug abbreviations and symbols. Dose-format checks apply to `mg`, `mcg`, `g`, `mL`, and units. Name matching uses configured substrings and can produce false positives or miss variants. The source contains the complete current lists and patterns. Flags are deduplicated, and a high flag takes precedence over medium and low for the overall banner. No flag means LOW, not a clinical clearance.

[Back to README](../README.md)

