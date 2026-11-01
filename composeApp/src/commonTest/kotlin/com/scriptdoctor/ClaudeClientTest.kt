package com.scriptdoctor

import com.scriptdoctor.api.ClaudeClient
import com.scriptdoctor.api.DecodeException
import com.scriptdoctor.model.Confidence
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ClaudeClientTest {

    private val transcriptionJson = """
        {"is_prescription":true,"legibility":"clear","patient_name":"","prescriber_name":"Dr. A","date":"",
         "medications":[{"raw_text":"Amoxicillin 500mg TID","drug_name":"Amoxicillin","strength":"500 mg",
         "dosage_form":"capsule","route":"oral","frequency":"three times daily","duration":"","quantity":"",
         "instructions_plain":"Take one capsule three times a day.","confidence":"high",
         "alternative_readings":[],"concerns":[]}],"general_notes":[]}
    """.trimIndent()

    private fun apiResponse(text: String, stopReason: String = "end_turn"): String {
        val escaped = Json.encodeToString(kotlinx.serialization.json.JsonPrimitive(text))
        return """{"id":"msg_1","type":"message","role":"assistant","model":"claude-opus-5-5",
            "content":[{"type":"thinking","thinking":"","signature":"x"},{"type":"text","text":$escaped}],
            "stop_reason":"$stopReason","stop_details":null}"""
    }

    private fun client(status: HttpStatusCode = HttpStatusCode.OK, body: String, onRequest: (JsonObject, Map<String, String?>) -> Unit = { _, _ -> }) =
        ClaudeClient(
            HttpClient(MockEngine { req ->
                val sent = Json.parseToJsonElement(req.body.toByteArray().decodeToString()).jsonObject
                onRequest(sent, listOf("x-api-key", "anthropic-version", "anthropic-beta").associateWith { req.headers[it] })
                respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
            }),
            apiKeyProvider = { "sk-test" },
        )

    @Test
    fun sendsImageWithStructuredOutputAndParsesReply() = runTest {
        var captured: JsonObject? = null
        var headers: Map<String, String?> = emptyMap()
        val c = client(body = apiResponse(transcriptionJson)) { body, h -> captured = body; headers = h }

        val result = c.decode(byteArrayOf(1, 2, 3))

        assertEquals("Amoxicillin", result.transcription.medications.single().drugName)
        assertEquals(Confidence.HIGH, result.transcription.medications.single().confidence)
        assertEquals("claude-opus-5-5", result.model)

        val body = captured!!
        assertEquals("claude-opus-5-5", body["model"]!!.jsonPrimitive.content)
        assertEquals("json_schema", body["output_config"]!!.jsonObject["format"]!!.jsonObject["type"]!!.jsonPrimitive.content)
        val image = body["messages"]!!.jsonArray[0].jsonObject["content"]!!.jsonArray[0].jsonObject
        assertEquals("image", image["type"]!!.jsonPrimitive.content)
        assertEquals("AQID", image["source"]!!.jsonObject["data"]!!.jsonPrimitive.content)
        assertEquals("sk-test", headers["x-api-key"])
        assertEquals(ClaudeClient.API_VERSION, headers["anthropic-version"])
        assertTrue(headers["anthropic-beta"]!!.contains("server-side-fallback"))
    }

    @Test
    fun refusalIsReported() = runTest {
        val c = client(body = """{"model":"claude-opus-5-5","content":[],"stop_reason":"refusal","stop_details":{"type":"refusal","category":null,"explanation":"nope"}}""")
        assertFailsWith<DecodeException.Refused> { c.decode(byteArrayOf(1)) }
    }

    @Test
    fun maxTokensIsReported() = runTest {
        val c = client(body = apiResponse("{\"is_pre", stopReason = "max_tokens"))
        assertFailsWith<DecodeException.Truncated> { c.decode(byteArrayOf(1)) }
    }

    @Test
    fun httpErrorsAreMapped() = runTest {
        val c = client(HttpStatusCode.Unauthorized, """{"type":"error","error":{"type":"authentication_error","message":"invalid x-api-key"}}""")
        val e = assertFailsWith<DecodeException.Http> { c.decode(byteArrayOf(1)) }
        assertEquals(401, e.status)
    }

    @Test
    fun missingKeyFailsFast() = runTest {
        val c = ClaudeClient(HttpClient(MockEngine { error("should not be called") }), apiKeyProvider = { " " })
        assertFailsWith<DecodeException.MissingApiKey> { c.decode(byteArrayOf(1)) }
    }
}
