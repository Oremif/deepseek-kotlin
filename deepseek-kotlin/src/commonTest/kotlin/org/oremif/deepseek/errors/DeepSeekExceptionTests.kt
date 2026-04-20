package org.oremif.deepseek.errors

import io.ktor.http.Headers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeepSeekExceptionTests {

    @Test
    fun `OverloadServerException exposes 503 as statusCode`() {
        val ex = DeepSeekException.OverloadServerException(Headers.Empty, null, "overloaded")
        assertEquals(503, ex.statusCode)
    }

    @Test
    fun `from with 4 args maps 503 to OverloadServerException`() {
        val ex = DeepSeekException.from(503, Headers.Empty, null, "Service Unavailable")
        assertTrue(
            ex is DeepSeekException.OverloadServerException,
            "expected OverloadServerException, got ${ex::class.simpleName}"
        )
        assertEquals(503, ex.statusCode)
    }

    @Test
    fun `both from overloads return OverloadServerException for 503`() {
        val fromShort = DeepSeekException.from(503, Headers.Empty, null)
        val fromLong = DeepSeekException.from(503, Headers.Empty, null, "Service Unavailable")
        assertEquals(fromShort::class, fromLong::class)
    }

    @Test
    fun `message has no leading newline when error is null`() {
        val ex = DeepSeekException.from(401, Headers.Empty, null, "Please check your API key.")
        assertEquals("Please check your API key.", ex.message)
    }

    @Test
    fun `message is empty when both error and fallback message are absent`() {
        val ex = DeepSeekException.UnexpectedStatusCodeException(418, Headers.Empty, null, null)
        assertEquals("", ex.message)
    }

    @Test
    fun `message contains only error message when fallback message is null`() {
        val error = DeepSeekError(DeepSeekError.Error(message = "Invalid API key"))
        val ex = DeepSeekException.UnexpectedStatusCodeException(418, Headers.Empty, error, null)
        assertEquals("Invalid API key", ex.message)
    }

    @Test
    fun `message joins error and fallback with newline when both present`() {
        val error = DeepSeekError(DeepSeekError.Error(message = "Invalid API key"))
        val ex = DeepSeekException.from(401, Headers.Empty, error, "Please check your API key.")
        assertEquals("Invalid API key\nPlease check your API key.", ex.message)
    }
}
