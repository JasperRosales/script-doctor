# External services

The client in `api/GeminiClient.kt` sends `POST https://generativelanguage.googleapis.com/v1beta/interactions` with the API key in `x-goog-api-key` and `Api-Revision: 2026-05-20`. The request contains a text prompt, the base64-encoded JPEG, a system instruction, and a JSON schema in `response_format`. It requests `thinking_level: high`, up to 16,000 output tokens, and `store: false`. The default request and socket timeouts are 300 seconds; connection timeout is 30 seconds.

The prompt asks the model to transcribe what is written rather than silently correct it, leave unknown fields empty, list plausible alternative readings, supply plain-language instructions and concerns, and identify non-prescription images. These are instructions to a probabilistic model, not guarantees of accuracy. The schema requires fields and constrains enum values; deserialization rejects incompatible output but does not independently verify the transcription against the image.

The client accepts nested `model_output` text or legacy flat text output, ignores unknown JSON keys, and handles missing output, malformed data, incomplete or blocked responses, and HTTP or network errors. Specific messages are provided for rejected keys, forbidden access, oversized images, quota/rate limits, and server errors. API behavior, model availability, and access are external dependencies and can change.


See [Configuration](configuration.md) for credentials and [Architecture](architecture.md) for response data.

[Back to README](../README.md)

