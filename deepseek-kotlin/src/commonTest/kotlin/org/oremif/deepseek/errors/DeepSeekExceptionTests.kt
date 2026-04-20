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
}
