# script-doctor
It helps read a doctor's handwriting and highlights selected risks. Built for DSH Hacks V2 (AI x Healthcare).

Script Doctor is an **Android** app built with **Kotlin Multiplatform + Compose Multiplatform** (Android target only). Photograph a handwritten prescription and it:

1. **Attempts to transcribe medication lines** (drug, strength, form, route, frequency, duration, quantity) using Google's Gemini vision model, including its reading of the original line.
2. **Explains the directions in plain language** a patient can follow.
3. **Admits uncertainty**: each medication gets a confidence rating, and other plausible readings of the handwriting are listed (e.g. *Celebrex* vs. *Celexa*).
4. **Runs on-device safety checks** that don't rely on the model:
   - ISMP error-prone abbreviations (`QD`, `QOD`, `U`/`IU`, `cc`, `µg`, `SC`, `HS`, `D/C`, `MSO4`, …)
   - Dose-format traps: trailing zeros (`1.0 mg`) and missing leading zeros (`.5 mg`), which cause 10x errors
   - Look-alike/sound-alike drug pairs (hydroxyzine/hydralazine, tramadol/trazodone, …)
   - High-alert drugs (insulin, warfarin, opioids, …) read with less than high confidence
   - Missing strength or frequency, illegible pages, and images that aren't prescriptions
5. **Rolls everything up into a risk level**: *Low risk*, *Verify before use*, or *High risk — confirm with prescriber*.

> ⚠️ Script Doctor is a reading aid, not a medical device. Always confirm with the prescriber or a licensed pharmacist before taking or dispensing any medication.

## Documentation

- [Getting started](docs/getting-started.md) — requirements, installation, and building from source.
- [Features](docs/features.md) — capabilities, safety rules, and their limits.
- [Usage](docs/usage.md) — quick scan workflow and troubleshooting.
- [User guide](docs/user-guide.md) — screens, results, history, and privacy.
- [Architecture](docs/architecture.md) — data flow, components, project layout, and models.
- [Android](docs/android.md) — platform integration, storage, and permissions.
- [Configuration](docs/configuration.md) — API key, build, and model settings.
- [External services](docs/external-services.md) — Gemini requests, responses, and errors.
- [Contributing](docs/contributing.md) — development, testing, and release workflow.
## Download

Get the builds from [Releases](https://github.com/JasperRosales/script-doctor/releases):

- **Android**: `ScriptDoctor-vX.Y.Z-android.apk`. Sideload it on Android 8.0 or later. You may need to allow installs from unknown sources.

On first launch, open **Settings** and paste a free [Gemini API key](https://aistudio.google.com/apikey). Tap **Try a sample result** to see the app without a key.

## How it works

```
 Camera / Photos ──► downscale to ≤1568 px JPEG ──► Gemini Interactions API (gemini-3.8-flash)
                                                      • image + system instruction
                                                      • structured outputs (JSON schema)
                                                      • thinking_level: high
                                                      ▼
                         Transcription (typed, validated) ──► SafetyChecker (on-device rules)
                                                      ▼
                         Risk banner · medication cards · flags ──► local history
```

- **Shared code (`composeApp/src/commonMain`)**: UI, state, Gemini client (Ktor), JSON schema, safety rules, history. Nearly all of the app lives here.
- **Android (`androidMain`)**: photo picker, camera via `FileProvider`, EXIF-aware downscaling, SharedPreferences.

The Gemini client lives in `commonMain`, so it calls `POST /v1beta/interactions` over HTTP with Ktor instead of using a platform SDK. The request:
- constrains the reply to the transcription schema with `response_format` (structured outputs),
- sets `generation_config.thinking_level: "high"`, since accuracy matters more than speed here,
- sets `store: false` in the interaction request,
- handles failed/incomplete responses and maps HTTP errors (bad key, quota) to readable messages.

### Privacy

Photos go from the phone to the Gemini API when you tap **Decode**. Camera captures are written to the app cache, and the app does not explicitly delete that cache file after processing. Decoded text is saved in local history until you delete it.

On Gemini's **free tier**, Google may use submitted prompts and images to improve its products. Use a paid-tier key for real patient prescriptions.

The API key is stored in app-private storage on the device. That's fine for a hackathon demo, but a production deployment should send requests through a backend so no key ships to the phone.

## Build from source

Requirements: JDK 17 and the Android SDK (API 35).

```bash
./gradlew :composeApp:assembleDebug            # APK → composeApp/build/outputs/apk/debug/
./gradlew :composeApp:installDebug             # install on a connected device/emulator

# Tests (shared logic: safety rules + API client)
./gradlew :composeApp:testDebugUnitTest
```

## Releasing

Push a tag like `v1.0.1`. The `Release` workflow runs the tests, builds the release APK, and attaches it to the GitHub release.

The release APK is signed with the debug key so it installs out of the box. Add a real signing config before publishing to the Play Store.

## Project layout

```
composeApp/
  src/commonMain/   shared UI, Gemini client, safety checker, models
  src/commonTest/   SafetyChecker + GeminiClient tests
  src/androidMain/  MainActivity, image picker/camera, storage
.github/workflows/  CI and tag-triggered release
```

## License

MIT, see [LICENSE](LICENSE).

