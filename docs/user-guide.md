# User guide

Script Doctor is a reading aid, not a medical device. A model may misread or omit text, and even a low risk result does not confirm a safe drug or dose. Consult the prescriber or a licensed pharmacist before taking or dispensing medication.

## Screens

- **Scan:** setup prompt when no key is saved; camera and gallery selection; preview, replacement, removal, Decode, cancellation, and fictional sample.
- **Result:** overall risk and flag count; patient, prescriber, date, legibility, timestamp, and model when available; medication cards with confidence, alternatives, directions, raw text, local flags, and model concerns. Document flags and general notes appear in Notes.
- **History:** non-sample results in newest-first order; reopen a result, delete one entry, or clear all after confirmation. Images are not available from history.
- **Settings:** add, show/hide, save, or remove the API key; view privacy and model information. The connected indicator means a nonblank key is stored, not that network access has been verified.

## Reading a result

Each medication card can show the line as transcribed, strength and form, directions, confidence, alternative readings, and things to check. Model-generated concerns and local safety flags are displayed, but only local flags determine the overall risk banner. No flags means LOW; it does not establish clinical safety. See [Features](features.md) for the rules and their limits.

## Privacy and history

Results and the key are stored on the device. The app sends the photo to Google when Decode is selected. History does not include the image; camera capture can remain in app cache. Delete scans in History and remove the key in Settings. See [Android implementation](android.md) for storage details.

[Back to README](../README.md)

