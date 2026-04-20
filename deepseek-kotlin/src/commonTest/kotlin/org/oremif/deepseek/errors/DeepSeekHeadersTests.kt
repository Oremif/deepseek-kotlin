package org.oremif.deepseek.errors

import io.ktor.http.HeadersBuilder
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeepSeekHeadersTests {

    @Test
    fun `get returns first value case-insensitively`() {
        val headers = DeepSeekHeaders(mapOf("Content-Type" to listOf("application/json")))
        assertEquals("application/json", headers["Content-Type"])
        assertEquals("application/json", headers["content-type"])
        assertEquals("application/json", headers["CONTENT-TYPE"])
    }

    @Test
    fun `get returns null when header is absent`() {
        val headers = DeepSeekHeaders(mapOf("X-Request-Id" to listOf("abc")))
        assertNull(headers["Content-Type"])
    }

    @Test
    fun `getAll returns every value for a multi-valued header`() {
        val headers = DeepSeekHeaders(mapOf("Set-Cookie" to listOf("a=1", "b=2")))
        assertEquals(listOf("a=1", "b=2"), headers.getAll("set-cookie"))
    }

    @Test
    fun `getAll returns empty list when header is absent`() {
        val headers = DeepSeekHeaders(emptyMap())
        assertEquals(emptyList(), headers.getAll("Content-Type"))
    }

    @Test
    fun `contains is case-insensitive`() {
        val headers = DeepSeekHeaders(mapOf("Retry-After" to listOf("30")))
        assertTrue("retry-after" in headers)
        assertFalse("X-Missing" in headers)
    }

    @Test
    fun `Empty reports isEmpty and has no names`() {
        assertTrue(DeepSeekHeaders.Empty.isEmpty())
        assertEquals(emptySet(), DeepSeekHeaders.Empty.names())
    }

    @Test
    fun `equals and hashCode match when entries match`() {
        val a = DeepSeekHeaders(mapOf("X-A" to listOf("1")))
        val b = DeepSeekHeaders(mapOf("X-A" to listOf("1")))
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `toDeepSeekHeaders maps Ktor headers preserving case of first occurrence`() {
        val ktorHeaders = headersOf(
            "Content-Type" to listOf("application/json"),
            "X-Request-Id" to listOf("abc-123"),
        )
        val mapped = ktorHeaders.toDeepSeekHeaders()
        assertEquals("application/json", mapped["content-type"])
        assertEquals("abc-123", mapped["x-request-id"])
    }

    @Test
    fun `toDeepSeekHeaders preserves multi-valued headers`() {
        val ktorHeaders = HeadersBuilder().apply {
            append("Set-Cookie", "a=1")
            append("Set-Cookie", "b=2")
        }.build()
        val mapped = ktorHeaders.toDeepSeekHeaders()
        assertEquals(listOf("a=1", "b=2"), mapped.getAll("Set-Cookie"))
    }

    @Test
    fun `toDeepSeekHeaders on empty returns the shared Empty instance`() {
        val mapped = io.ktor.http.Headers.Empty.toDeepSeekHeaders()
        assertTrue(mapped === DeepSeekHeaders.Empty)
    }
}
