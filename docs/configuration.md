# Configuration

## API key

Open **Settings**, follow the link to obtain a Gemini API key, paste it, and select **Save key**. Use **Remove** to delete it. The Connected indicator only means a nonblank key is stored; it does not verify access. A sample result works without a key. Real scans require network access and a key authorized for the configured model.

The key is trimmed and stored in app-private `SharedPreferences` under `gemini_api_key`, without application-level encryption. Never commit a real key to the repository. A client-side key can be extracted by a sufficiently privileged party; production deployments should evaluate server-side credential management.

## Build and model settings

| Setting | Location | Current value |
| --- | --- | --- |
| SDK and dependency versions | `gradle/libs.versions.toml` | Android min 26, compile/target 35 |
| App ID, version, signing | `composeApp/build.gradle.kts` | `com.scriptdoctor`, 1.1.0 / 2, debug signing for release |
| Model and API revision | `GeminiClient.kt` | `gemini-3.8-flash`, `2026-05-20` |
| Image processing | `platform/Platform.kt` | 1,568-pixel maximum edge, JPEG quality 88 |
| SDK location | local `local.properties` | Machine-specific and ignored by Git |

Model and image settings are source constants, not user-facing options. See [External services](external-services.md) and [Android implementation](android.md) for details.

[Back to README](../README.md)

