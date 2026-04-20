package org.oremif.deepseek.utils

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.longs.shouldBeGreaterThanOrEqual
import io.kotest.matchers.longs.shouldBeLessThan
import io.kotest.matchers.shouldBe
import kotlin.random.Random
import kotlin.test.Test

class RetryPolicyTests {

    @Test
    fun `retries 408 Request Timeout`() {
        isRetryableStatus(408).shouldBeTrue()
    }

    @Test
    fun `retries 425 Too Early`() {
        isRetryableStatus(425).shouldBeTrue()
    }

    @Test
    fun `retries 429 Too Many Requests`() {
        isRetryableStatus(429).shouldBeTrue()
    }

    @Test
    fun `retries 5xx server errors`() {
        isRetryableStatus(500).shouldBeTrue()
        isRetryableStatus(502).shouldBeTrue()
        isRetryableStatus(503).shouldBeTrue()
        isRetryableStatus(504).shouldBeTrue()
        isRetryableStatus(599).shouldBeTrue()
    }

    @Test
    fun `does not retry 400 Bad Request`() {
        isRetryableStatus(400).shouldBeFalse()
    }

    @Test
    fun `does not retry 401 Unauthorized`() {
        isRetryableStatus(401).shouldBeFalse()
    }

    @Test
    fun `does not retry 402 Insufficient Balance`() {
        isRetryableStatus(402).shouldBeFalse()
    }

    @Test
    fun `does not retry 403 Forbidden`() {
        isRetryableStatus(403).shouldBeFalse()
    }

    @Test
    fun `does not retry 404 Not Found`() {
        isRetryableStatus(404).shouldBeFalse()
    }

    @Test
    fun `does not retry 422 Unprocessable Entity`() {
        isRetryableStatus(422).shouldBeFalse()
    }

    @Test
    fun `does not retry 2xx success`() {
        isRetryableStatus(200).shouldBeFalse()
        isRetryableStatus(201).shouldBeFalse()
        isRetryableStatus(204).shouldBeFalse()
    }

    @Test
    fun `does not retry 3xx redirects`() {
        isRetryableStatus(301).shouldBeFalse()
        isRetryableStatus(302).shouldBeFalse()
        isRetryableStatus(304).shouldBeFalse()
    }

    @Test
    fun `delay grows exponentially with retry count`() {
        val base = 500L
        computeRetryDelayMillis(retry = 1, baseMillis = base, jitterMillis = 0L) shouldBe 1_000L
        computeRetryDelayMillis(retry = 2, baseMillis = base, jitterMillis = 0L) shouldBe 2_000L
        computeRetryDelayMillis(retry = 3, baseMillis = base, jitterMillis = 0L) shouldBe 4_000L
    }

    @Test
    fun `delay is capped at maxMillis`() {
        computeRetryDelayMillis(
            retry = 10,
            baseMillis = 500L,
            maxMillis = 5_000L,
            jitterMillis = 0L,
        ) shouldBe 5_000L
    }

    @Test
    fun `delay does not overflow for large retry counts`() {
        computeRetryDelayMillis(
            retry = 60,
            baseMillis = 500L,
            maxMillis = 30_000L,
            jitterMillis = 0L,
        ) shouldBe 30_000L
    }

    @Test
    fun `jitter stays within configured bound`() {
        val base = 500L
        val jitter = 250L
        repeat(50) { seed ->
            val d = computeRetryDelayMillis(
                retry = 1,
                baseMillis = base,
                jitterMillis = jitter,
                random = Random(seed.toLong()),
            )
            d.shouldBeGreaterThanOrEqual(1_000L)
            d.shouldBeLessThan(1_000L + jitter)
        }
    }

    @Test
    fun `zero jitter is deterministic`() {
        val d1 = computeRetryDelayMillis(retry = 2, baseMillis = 500L, jitterMillis = 0L)
        val d2 = computeRetryDelayMillis(retry = 2, baseMillis = 500L, jitterMillis = 0L)
        d1 shouldBe d2
        d1 shouldBe 2_000L
    }

    @Test
    fun `same seed produces same delay`() {
        val seed = 42L
        val d1 = computeRetryDelayMillis(
            retry = 2, baseMillis = 500L, jitterMillis = 250L, random = Random(seed),
        )
        val d2 = computeRetryDelayMillis(
            retry = 2, baseMillis = 500L, jitterMillis = 250L, random = Random(seed),
        )
        d1 shouldBe d2
    }
}
