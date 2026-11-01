# script-doctor
It decodes a doctor's handwriting before it causes harm. Built for DSH Hacks V2 (AI x Healthcare).

Script Doctor is a **Kotlin Multiplatform + Compose Multiplatform** app for **Android and iOS**. Photograph a handwritten prescription and it:

1. **Transcribes every medication** (drug, strength, form, route, frequency, duration, quantity) using Claude's vision model, keeping the line exactly as written.
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

## Download

Get the builds from [Releases](https://github.com/JasperRosales/script-doctor/releases):

- **Android**: `ScriptDoctor-vX.Y.Z-android.apk`. Sideload it (Android 8.0+).
- **iOS**: `ScriptDoctor-vX.Y.Z-ios-simulator.zip` is an unsigned simulator build. Unzip it and drag `ScriptDoctor.app` onto a booted simulator, or run `xcrun simctl install booted ScriptDoctor.app`. To run on a real iPhone, build from source with your own signing team (see below).

On first launch, open **Settings** and paste an [Anthropic API key](https://console.anthropic.com/). Tap **Try a sample result** to see the app without a key.

## How it works

```
 Camera / Photos ──► downscale to ≤1568 px JPEG ──► Claude Messages API (claude-opus-5-5)
                                                      • image + system prompt
                                                      • structured outputs (JSON schema)
                                                      • effort: high
                                                      ▼
                         Transcription (typed, validated) ──► SafetyChecker (on-device rules)
                                                      ▼
                         Risk banner · medication cards · flags ──► local history
```

- **Shared code (`composeApp/src/commonMain`)**: UI, state, Claude client (Ktor), JSON schema, safety rules, history. Nearly all of the app lives here.
- **Android (`androidMain`)**: photo picker, camera via `FileProvider`, EXIF-aware downscaling, SharedPreferences.
- **iOS (`iosMain` + `iosApp/`)**: `PHPickerViewController`, `UIImagePickerController` camera, NSUserDefaults, and a SwiftUI host for the Compose UI.

The Claude call uses raw HTTP because no official Anthropic SDK runs on Kotlin/Native (iOS). The request:
- constrains the reply to the transcription schema with `output_config.format` (structured outputs),
- sets `output_config.effort: "high"`, since accuracy matters more than speed here,
- enables server-side refusal fallback (`fallbacks: "default"`, beta `server-side-fallback-2026-07-01`),
- handles `refusal` and `max_tokens` stop reasons and maps HTTP errors to readable messages.

### Privacy

Photos go straight from the phone to the Anthropic API when you tap **Decode**. They are never written to disk except a temporary camera capture in the app cache on Android. Decoded text is saved in local history until you delete it.

The API key is stored in app-private storage on the device. That's fine for a hackathon demo, but a production deployment should send requests through a backend so no key ships to the phone.

## Build from source

Requirements: JDK 17, Android SDK (API 35). For iOS you also need macOS with Xcode 16+ and [XcodeGen](https://github.com/yonaskolb/XcodeGen).

```bash
# Android
./gradlew :composeApp:assembleDebug            # APK → composeApp/build/outputs/apk/debug/
./gradlew :composeApp:installDebug             # install on a connected device/emulator

# Tests (shared logic: safety rules + API client)
./gradlew :composeApp:testDebugUnitTest
./gradlew :composeApp:iosSimulatorArm64Test    # macOS only

# iOS
brew install xcodegen
cd iosApp && xcodegen generate && open iosApp.xcodeproj
# In Xcode: select the iosApp target → Signing & Capabilities → pick your team → Run
```

## Releasing

Push a tag like `v1.0.1`. The `Release` workflow builds the Android APK on Ubuntu and the iOS simulator app on macOS, then attaches both to the GitHub release.

The release APK is signed with the debug key so it installs out of the box. Add a real signing config before publishing to the Play Store.

## Project layout

```
composeApp/
  src/commonMain/   shared UI, Claude client, safety checker, models
  src/commonTest/   SafetyChecker + ClaudeClient tests (run on Android & iOS)
  src/androidMain/  MainActivity, image picker/camera, storage
  src/iosMain/      MainViewController, PHPicker/camera, storage
iosApp/             XcodeGen spec + SwiftUI entry point
.github/workflows/  CI (Android + iOS) and tag-triggered release
```

## License

MIT, see [LICENSE](LICENSE).
