package com.scriptdoctor

import com.scriptdoctor.api.GeminiClient
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

class GeminiClientTest {

    private val transcriptionJson = """
        {"is_prescription":true,"legibility":"clear","patient_name":"","prescriber_name":"Dr. A","date":"",
         "medications":[{"raw_text":"Amoxicillin 500mg TID","drug_name":"Amoxicillin","strength":"500 mg",
         "dosage_form":"capsule","route":"oral","frequency":"three times daily","duration":"","quantity":"",
         "instructions_plain":"Take one capsule three times a day.","confidence":"high",
         "alternative_readings":[],"concerns":[]}],"general_notes":[]}
    """.trimIndent()

    private fun apiResponse(text: String, status: String = "completed"): String {
        val escaped = Json.encodeToString(kotlinx.serialization.json.JsonPrimitive(text))
        return """{"id":"i_1","model":"gemini-3.8-flash","status":"$status",
            "steps":[{"type":"thought","signature":"x"},{"type":"model_output","content":[{"type":"text","text":$escaped}]}]}"""
    }

    private fun client(status: HttpStatusCode = HttpStatusCode.OK, body: String, onRequest: (JsonObject, Map<String, String?>) -> Unit = { _, _ -> }) =
        GeminiClient(
            HttpClient(MockEngine { req ->
                val sent = Json.parseToJsonElement(req.body.toByteArray().decodeToString()).jsonObject
                onRequest(sent, listOf("x-goog-api-key", "Api-Revision").associateWith { req.headers[it] })
                respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
            }),
            apiKeyProvider = { "AIza-test" },
        )

    @Test
    fun sendsImageWithStructuredOutputAndParsesReply() = runTest {
        var captured: JsonObject? = null
        var headers: Map<String, String?> = emptyMap()
        val c = client(body = apiResponse(transcriptionJson)) { body, h -> captured = body; headers = h }

        val result = c.decode(byteArrayOf(1, 2, 3))

        assertEquals("Amoxicillin", result.transcription.medications.single().drugName)
        assertEquals(Confidence.HIGH, result.transcription.medications.single().confidence)
        assertEquals("gemini-3.8-flash", result.model)

        val body = captured!!
        assertEquals(GeminiClient.DEFAULT_MODEL, body["model"]!!.jsonPrimitive.content)
        assertEquals("application/json", body["response_format"]!!.jsonObject["mime_type"]!!.jsonPrimitive.content)
        assertEquals("object", body["response_format"]!!.jsonObject["schema"]!!.jsonObject["type"]!!.jsonPrimitive.content)
        val image = body["input"]!!.jsonArray[1].jsonObject
        assertEquals("image", image["type"]!!.jsonPrimitive.content)
        assertEquals("AQID", image["data"]!!.jsonPrimitive.content)
        assertEquals("false", body["store"]!!.jsonPrimitive.content)
        assertEquals("AIza-test", headers["x-goog-api-key"])
        assertEquals(GeminiClient.API_REVISION, headers["Api-Revision"])
    }

    @Test
    fun blockedIsReported() = runTest {
        val c = client(body = """{"model":"gemini-3.8-flash","status":"failed","steps":[]}""")
        assertFailsWith<DecodeException.Refused> { c.decode(byteArrayOf(1)) }
    }

    @Test
    fun maxTokensIsReported() = runTest {
        val c = client(body = """{"status":"incomplete","incomplete_details":{"reason":"max_output_tokens"},"steps":[]}""")
        assertFailsWith<DecodeException.Truncated> { c.decode(byteArrayOf(1)) }
    }

    @Test
    fun truncatedJsonIsReported() = runTest {
        val c = client(body = apiResponse("{\"is_pre", status = "incomplete"))
        assertFailsWith<DecodeException> { c.decode(byteArrayOf(1)) }
    }

    @Test
    fun httpErrorsAreMapped() = runTest {
        val c = client(HttpStatusCode.BadRequest, """{"error":{"code":400,"message":"API key not valid. Please pass a valid API key.","status":"INVALID_ARGUMENT"}}""")
        val e = assertFailsWith<DecodeException.Http> { c.decode(byteArrayOf(1)) }
        assertEquals(400, e.status)
        assertTrue(e.message!!.contains("rejected"))
    }

    @Test
    fun arrayWrappedErrorIsMapped() = runTest {
        val c = client(HttpStatusCode.BadRequest, """[{"error":{"code":400,"message":"API key not valid. Please pass a valid API key.","status":"INVALID_ARGUMENT"}}]""")
        val e = assertFailsWith<DecodeException.Http> { c.decode(byteArrayOf(1)) }
        assertTrue(e.message!!.contains("rejected"))
    }

    @Test
    fun missingKeyFailsFast() = runTest {
        val c = GeminiClient(HttpClient(MockEngine { error("should not be called") }), apiKeyProvider = { " " })
        assertFailsWith<DecodeException.MissingApiKey> { c.decode(byteArrayOf(1)) }
    }
}
