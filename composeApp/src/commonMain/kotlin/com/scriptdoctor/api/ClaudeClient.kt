package com.scriptdoctor.api

import com.scriptdoctor.model.Transcription
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

sealed class DecodeException(message: String) : Exception(message) {
    class MissingApiKey : DecodeException("Add your Anthropic API key in Settings first.")
    class Http(val status: Int, detail: String) : DecodeException(httpMessage(status, detail))
    class Refused(detail: String) : DecodeException("Claude declined to read this image. $detail".trim())
    class Truncated : DecodeException("The response was cut off before it finished. Try again with a tighter crop.")
    class Malformed(detail: String) : DecodeException("Could not understand the response: $detail")
    class Network(detail: String) : DecodeException("Network error: $detail")
}

private fun httpMessage(status: Int, detail: String) = when (status) {
    401 -> "The API key was rejected. Check it in Settings."
    403 -> "This API key is not allowed to use the model."
    413 -> "The image is too large. Try a smaller photo."
    429 -> "Rate limited by the API. Wait a moment and try again."
    in 500..599 -> "The Claude API is having trouble (HTTP $status). Try again shortly."
    else -> "Request failed (HTTP $status). $detail"
}

data class ClaudeResponse(val transcription: Transcription, val model: String)

/**
 * Minimal Claude Messages API client for Kotlin Multiplatform.
 *
 * Anthropic has no official SDK that runs on Kotlin/Native (iOS), so this shared client
 * calls `POST /v1/messages` directly with Ktor. It sends the photo as a base64 image block
 * and constrains the reply to [TranscriptionSchema] with structured outputs.
 */
class ClaudeClient(
    private val http: HttpClient,
    private val apiKeyProvider: () -> String?,
    private val model: String = DEFAULT_MODEL,
    private val baseUrl: String = "https://api.anthropic.com",
) {
    private val json = Json { ignoreUnknownKeys = true }

    @OptIn(ExperimentalEncodingApi::class)
    suspend fun decode(jpeg: ByteArray): ClaudeResponse {
        val key = apiKeyProvider()?.trim().orEmpty()
        if (key.isEmpty()) throw DecodeException.MissingApiKey()

        val body = buildRequest(Base64.encode(jpeg))
        val response = try {
            http.post("$baseUrl/v1/messages") {
                header("x-api-key", key)
                header("anthropic-version", API_VERSION)
                // Server-side refusal fallback: if a safety classifier declines, the API
                // retries on a fallback model inside the same call.
                header("anthropic-beta", FALLBACK_BETA)
                contentType(ContentType.Application.Json)
                setBody(body.toString())
            }
        } catch (e: Exception) {
            throw DecodeException.Network(e.message ?: e::class.simpleName.orEmpty())
        }

        val text = response.bodyAsText()
        if (!response.status.isSuccess()) {
            val detail = runCatching {
                json.parseToJsonElement(text).jsonObject["error"]?.jsonObject?.get("message")?.jsonPrimitive?.contentOrNull
            }.getOrNull().orEmpty()
            throw DecodeException.Http(response.status.value, detail)
        }
        return parseResponse(text)
    }

    internal fun buildRequest(imageBase64: String): JsonObject = buildJsonObject {
        put("model", model)
        put("max_tokens", 16000)
        put("fallbacks", "default")
        put("system", SYSTEM_PROMPT)
        putJsonObject("output_config") {
            // Misreading a prescription can hurt someone; spend the extra reasoning.
            put("effort", "high")
            putJsonObject("format") {
                put("type", "json_schema")
                put("schema", TranscriptionSchema.schema)
            }
        }
        putJsonArray("messages") {
            add(buildJsonObject {
                put("role", "user")
                putJsonArray("content") {
                    add(buildJsonObject {
                        put("type", "image")
                        putJsonObject("source") {
                            put("type", "base64")
                            put("media_type", "image/jpeg")
                            put("data", imageBase64)
                        }
                    })
                    add(buildJsonObject {
                        put("type", "text")
                        put("text", USER_PROMPT)
                    })
                }
            })
        }
    }

    internal fun parseResponse(raw: String): ClaudeResponse {
        val root = try {
            json.parseToJsonElement(raw).jsonObject
        } catch (e: Exception) {
            throw DecodeException.Malformed("not JSON")
        }
        val modelUsed = root["model"]?.jsonPrimitive?.contentOrNull ?: model
        when (root["stop_reason"]?.jsonPrimitive?.contentOrNull) {
            "refusal" -> {
                val explanation = root["stop_details"]?.let { d ->
                    (d as? JsonObject)?.get("explanation")?.jsonPrimitive?.contentOrNull
                }.orEmpty()
                throw DecodeException.Refused(explanation)
            }
            "max_tokens" -> throw DecodeException.Truncated()
        }
        val content = root["content"] as? JsonArray ?: throw DecodeException.Malformed("no content")
        val text = content
            .mapNotNull { it as? JsonObject }
            .filter { it["type"]?.jsonPrimitive?.contentOrNull == "text" }
            .joinToString("") { it["text"]?.jsonPrimitive?.contentOrNull.orEmpty() }
        if (text.isBlank()) throw DecodeException.Malformed("empty reply")
        val transcription = try {
            json.decodeFromString(Transcription.serializer(), text)
        } catch (e: Exception) {
            throw DecodeException.Malformed(e.message?.take(200) ?: "schema mismatch")
        }
        return ClaudeResponse(transcription, modelUsed)
    }

    companion object {
        const val DEFAULT_MODEL = "claude-opus-5-5"
        const val API_VERSION = "2026-10-21"
        const val FALLBACK_BETA = "server-side-fallback-2026-07-01"

        fun defaultHttpClient(): HttpClient = HttpClient {
            install(HttpTimeout) {
                requestTimeoutMillis = 300_000
                socketTimeoutMillis = 300_000
                connectTimeoutMillis = 30_000
            }
        }
    }
}

internal val SYSTEM_PROMPT = """
You transcribe handwritten medical prescriptions for pharmacists and patients. A wrong reading can lead to the wrong drug or dose being dispensed, so accuracy and honesty about uncertainty matter more than completeness.

Report exactly what is written. Do not correct, complete, or "improve" the prescription: if the prescriber wrote an unusual dose, report that dose and raise it as a concern rather than substituting a typical one. When a word could be read more than one way, give your best reading in drug_name and list every plausible alternative (especially look-alike/sound-alike drug names) in alternative_readings. Set confidence to "low" whenever a misreading is plausible.

In raw_text, copy the medication line character-for-character as written, including abbreviations such as QD, BID, U, cc, or µg. In instructions_plain, explain the directions in plain language a patient would understand. In concerns, note anything a pharmacist should verify: unusual doses, ambiguous abbreviations, missing information, possible interactions between the listed drugs, or doses outside the usual range.

Use an empty string for any field that is not present or cannot be read. If the image is not a prescription, set is_prescription to false and return an empty medications list.
""".trimIndent()

internal const val USER_PROMPT = "Transcribe this prescription."

/** JSON schema for structured outputs. Mirrors [Transcription]. */
internal object TranscriptionSchema {
    private fun str() = buildJsonObject { put("type", "string") }
    private fun strArray() = buildJsonObject {
        put("type", "array")
        put("items", str())
    }
    private fun enumOf(vararg values: String) = buildJsonObject {
        put("type", "string")
        put("enum", buildJsonArray { values.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) } })
    }
    private fun obj(vararg props: Pair<String, JsonObject>) = buildJsonObject {
        put("type", "object")
        put("properties", JsonObject(props.toMap()))
        put("required", buildJsonArray { props.forEach { add(kotlinx.serialization.json.JsonPrimitive(it.first)) } })
        put("additionalProperties", false)
    }

    val medication = obj(
        "raw_text" to str(),
        "drug_name" to str(),
        "strength" to str(),
        "dosage_form" to str(),
        "route" to str(),
        "frequency" to str(),
        "duration" to str(),
        "quantity" to str(),
        "instructions_plain" to str(),
        "confidence" to enumOf("high", "medium", "low"),
        "alternative_readings" to strArray(),
        "concerns" to strArray(),
    )

    val schema = obj(
        "is_prescription" to buildJsonObject { put("type", "boolean") },
        "legibility" to enumOf("clear", "partially_legible", "illegible"),
        "patient_name" to str(),
        "prescriber_name" to str(),
        "date" to str(),
        "medications" to buildJsonObject {
            put("type", "array")
            put("items", medication)
        },
        "general_notes" to strArray(),
    )
}
