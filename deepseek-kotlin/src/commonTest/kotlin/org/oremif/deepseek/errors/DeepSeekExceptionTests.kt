package org.oremif.deepseek.errors

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeepSeekExceptionTests {

    @Test
    fun `DeepSeekError deserializes string param as String`() {
        val json = """{"error":{"message":"Invalid value","type":"invalid_request_error","param":"max_tokens","code":"bad_request"}}"""
        val error = Json.decodeFromString(DeepSeekError.serializer(), json)
        assertEquals("max_tokens", error.error.param)
    }

    @Test
    fun `DeepSeekError deserializes null param as null`() {
        val json = """{"error":{"message":"Something failed","type":"server_error","param":null,"code":"internal_error"}}"""
        val error = Json.decodeFromString(DeepSeekError.serializer(), json)
        assertNull(error.error.param)
    }

    @Test
    fun `DeepSeekError deserializes missing param as null`() {
        val json = """{"error":{"message":"Something failed","type":"server_error","code":"internal_error"}}"""
        val error = Json.decodeFromString(DeepSeekError.serializer(), json)
        assertNull(error.error.param)
    }

    @Test
    fun `OverloadServerException exposes 503 as statusCode`() {
        val ex = DeepSeekException.OverloadServerException(DeepSeekHeaders.Empty, null, "overloaded")
        assertEquals(503, ex.statusCode)
    }

    @Test
    fun `from with 4 args maps 503 to OverloadServerException`() {
        val ex = DeepSeekException.from(503, DeepSeekHeaders.Empty, null, "Service Unavailable")
        assertTrue(
            ex is DeepSeekException.OverloadServerException,
            "expected OverloadServerException, got ${ex::class.simpleName}"
        )
        assertEquals(503, ex.statusCode)
    }

    @Test
    fun `both from overloads return OverloadServerException for 503`() {
        val fromShort = DeepSeekException.from(503, DeepSeekHeaders.Empty, null)
        val fromLong = DeepSeekException.from(503, DeepSeekHeaders.Empty, null, "Service Unavailable")
        assertEquals(fromShort::class, fromLong::class)
    }

    @Test
    fun `message has no leading newline when error is null`() {
        val ex = DeepSeekException.from(401, DeepSeekHeaders.Empty, null, "Please check your API key.")
        assertEquals("Please check your API key.", ex.message)
    }

    @Test
    fun `message is empty when both error and fallback message are absent`() {
        val ex = DeepSeekException.UnexpectedStatusCodeException(418, DeepSeekHeaders.Empty, null, null)
        assertEquals("", ex.message)
    }

    @Test
    fun `message contains only error message when fallback message is null`() {
        val error = DeepSeekError(DeepSeekError.Error(message = "Invalid API key"))
        val ex = DeepSeekException.UnexpectedStatusCodeException(418, DeepSeekHeaders.Empty, error, null)
        assertEquals("Invalid API key", ex.message)
    }

    @Test
    fun `message joins error and fallback with newline when both present`() {
        val error = DeepSeekError(DeepSeekError.Error(message = "Invalid API key"))
        val ex = DeepSeekException.from(401, DeepSeekHeaders.Empty, error, "Please check your API key.")
        assertEquals("Invalid API key\nPlease check your API key.", ex.message)
    }
}
