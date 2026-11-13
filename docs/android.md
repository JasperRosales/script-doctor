# Android implementation

Script Doctor has one Android target. The application ID is `com.scriptdoctor`; minimum API is 26 and compile and target SDK are 35. The current version is 1.1.0 (version code 2).

## Platform integration

`ScriptDoctorApp` initializes the application context. `MainActivity` hosts the Compose UI, configures system bars, and delegates back navigation to `AppState`. Android implementations provide storage, timestamps, bitmap decoding, and image selection. Image selection and processing are described in [Architecture](architecture.md).

## Storage, privacy, and permissions

- The API key and serialized result history are stored in app-private Android `SharedPreferences` named `script_doctor`, under `gemini_api_key` and `history_v1`. This is ordinary preferences storage, not application-level encrypted storage.
- Saving history writes at most the newest 50 results. The in-memory list is not truncated at insertion, so it can temporarily show more until reloaded. Invalid or unreadable stored history loads as an empty list.
- The app does not persist selected images as part of history. Camera capture does write `capture.jpg` in the app's cache; the implementation does not explicitly delete that file after processing. Gallery images remain in their original provider. The processed JPEG stays in memory for preview and upload.
- When Decode is selected, the image and prompt are sent directly to Google. `store: false` is sent in the request; this alone should not be interpreted as a complete guarantee about provider retention or data use. Review the provider's current terms and the applicable account tier before using sensitive data.
- Android backup is disabled in the manifest. The app declares Internet permission, marks camera hardware optional, and exposes a non-exported `FileProvider` with URI grants. The camera is launched through another app; no direct camera permission is declared.
- A key on a client device can be extracted by a sufficiently privileged party. A production system should evaluate server-side credential management, encryption, access controls, retention, auditability, and applicable privacy requirements.

See [Getting started](getting-started.md) for build commands and [Contributing](contributing.md) for release details.

[Back to README](../README.md)

