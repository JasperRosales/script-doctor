# Getting started

| Item | Current configuration |
| --- | --- |
| Application ID / namespace | `com.scriptdoctor` |
| Version | `1.1.0` (version code `2`) |
| Supported platform | Android only; minimum API 26 (Android 8.0), target and compile API 35 |
| Language and UI | Kotlin 2.1.20, Kotlin Multiplatform, Compose Multiplatform 1.8.0, Material 3 |
| Build | Gradle wrapper, Android Gradle Plugin 8.9.1, JDK 17 |
| Networking and serialization | Ktor 3.1.2 with Android OkHttp engine; kotlinx.serialization 1.8.1 |
| Concurrency | Kotlin coroutines 1.10.2 |
| External service | Gemini Interactions API; default model `gemini-3.8-flash` |

The source is structured for shared Kotlin code, but the build config declares only an Android target. It does not currently build an iOS or desktop application.

## Installation and build

### Install and use

1. Install a compatible APK on an Android 8.0 or later device. The repository includes `release/ScriptDoctor.apk`; published releases may also be available through the link in the root README.
2. For a real scan, open **Settings**, obtain a Gemini API key using the provided link, paste it, and select **Save key**. A saved key is indicated as connected, but the app does not validate it until a request is made.
3. On **Scan**, take a photo or select an image. Review the preview, then select **Decode**. The request requires network access and an API key with access to the configured model.
4. Review the result and consult the prescriber or pharmacist. Use **History** to reopen or delete saved results.
5. To explore without an API key, select **Try a sample result** on the Scan screen. Sample results are marked fictional and are not saved in history.

### Build locally

Install JDK 17 and the Android SDK with API 35, configure the SDK path in Android Studio or a local `local.properties`, and run from the repository root:

```powershell
.\gradlew.bat :composeApp:assembleDebug
.\gradlew.bat :composeApp:installDebug
.\gradlew.bat :composeApp:testDebugUnitTest
```

On macOS or Linux, use `./gradlew` instead. The debug APK is written under `composeApp/build/outputs/apk/debug/`. Installation requires a connected device or running emulator. Gradle may need access to configured dependency repositories on the first build. `local.properties` is ignored by Git and should remain machine-specific.

[Back to README](../README.md)

