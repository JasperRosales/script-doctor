# Contributing

## Development workflow

Set up JDK 17 and Android SDK API 35, then use the build and test commands in [Getting started](getting-started.md). Keep changes focused, follow existing Kotlin style, and update documentation when behavior changes.

## Testing and release

`SafetyCheckerTest` exercises clean input, abbreviation boundaries, dose formatting, look-alike names, uncertainty, high-alert drugs, illegibility, missing fields, and the sample. `GeminiClientTest` uses Ktor's mock engine to check the request and response parsing, status handling, key errors, and missing keys. These are unit tests; they do not establish real-world transcription accuracy, clinical safety, or end-to-end device behavior.

CI runs `:composeApp:testDebugUnitTest` and `:composeApp:assembleDebug` on pushes to `main` and pull requests. The Release workflow runs the unit tests and `:composeApp:assembleRelease` for `v*` tags or a manually supplied tag, then creates or updates a GitHub release and uploads a named APK. The release build currently uses the debug signing configuration and disables minification; configure a proper release key before distribution through a production app store.


## Maintenance considerations

Keep the JSON schema and serializable models aligned when fields change. Update safety rules and tests together, and assess false positives and missed cases. Changes to stored serialized models require consideration of existing `history_v1` data; parsing failures currently discard the visible history on load. Test camera, photo picker, rotation, cancellation, error states, history, and UI on actual supported Android devices in addition to unit tests. Before clinical or production use, independent validation, security/privacy review, appropriate signing, and evaluation of applicable regulations are needed.

The project is licensed under the [MIT License](../LICENSE).

[Back to README](../README.md)

