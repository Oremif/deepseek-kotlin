package org.oremif.deepseek.errors

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.ktor.http.*
import kotlin.test.Test

class DeepSeekHeadersTests {

    @Test
    fun `get returns first value case-insensitively`() {
        val headers = DeepSeekHeaders(mapOf("Content-Type" to listOf("application/json")))
        headers["Content-Type"] shouldBe "application/json"
        headers["content-type"] shouldBe "application/json"
        headers["CONTENT-TYPE"] shouldBe "application/json"
    }

    @Test
    fun `get returns null when header is absent`() {
        val headers = DeepSeekHeaders(mapOf("X-Request-Id" to listOf("abc")))
        headers["Content-Type"].shouldBeNull()
    }

    @Test
    fun `getAll returns every value for a multi-valued header`() {
        val headers = DeepSeekHeaders(mapOf("Set-Cookie" to listOf("a=1", "b=2")))
        headers.getAll("set-cookie") shouldBe listOf("a=1", "b=2")
    }

    @Test
    fun `getAll returns empty list when header is absent`() {
        val headers = DeepSeekHeaders(emptyMap())
        headers.getAll("Content-Type") shouldBe emptyList()
    }

    @Test
    fun `contains is case-insensitive`() {
        val headers = DeepSeekHeaders(mapOf("Retry-After" to listOf("30")))
        ("retry-after" in headers).shouldBeTrue()
        ("X-Missing" in headers).shouldBeFalse()
    }

    @Test
    fun `Empty reports isEmpty and has no names`() {
        DeepSeekHeaders.Empty.isEmpty().shouldBeTrue()
        DeepSeekHeaders.Empty.names() shouldBe emptySet()
    }

    @Test
    fun `equals and hashCode match when entries match`() {
        val a = DeepSeekHeaders(mapOf("X-A" to listOf("1")))
        val b = DeepSeekHeaders(mapOf("X-A" to listOf("1")))
        a shouldBe b
        a.hashCode() shouldBe b.hashCode()
    }

    @Test
    fun `toDeepSeekHeaders maps Ktor headers preserving case of first occurrence`() {
        val ktorHeaders = headersOf(
            "Content-Type" to listOf("application/json"),
            "X-Request-Id" to listOf("abc-123"),
        )
        val mapped = ktorHeaders.toDeepSeekHeaders()
        mapped["content-type"] shouldBe "application/json"
        mapped["x-request-id"] shouldBe "abc-123"
    }

    @Test
    fun `toDeepSeekHeaders preserves multi-valued headers`() {
        val ktorHeaders = HeadersBuilder().apply {
            append("Set-Cookie", "a=1")
            append("Set-Cookie", "b=2")
        }.build()
        val mapped = ktorHeaders.toDeepSeekHeaders()
        mapped.getAll("Set-Cookie") shouldBe listOf("a=1", "b=2")
    }

    @Test
    fun `toDeepSeekHeaders on empty returns the shared Empty instance`() {
        val mapped = Headers.Empty.toDeepSeekHeaders()
        mapped shouldBeSameInstanceAs DeepSeekHeaders.Empty
    }
}
