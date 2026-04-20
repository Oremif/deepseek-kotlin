package org.oremif.deepseek.utils

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RetryPolicyTests {

    @Test
    fun `retries 408 Request Timeout`() {
        assertTrue(isRetryableStatus(408))
    }

    @Test
    fun `retries 425 Too Early`() {
        assertTrue(isRetryableStatus(425))
    }

    @Test
    fun `retries 429 Too Many Requests`() {
        assertTrue(isRetryableStatus(429))
    }

    @Test
    fun `retries 5xx server errors`() {
        assertTrue(isRetryableStatus(500), "500 Internal Server Error must retry")
        assertTrue(isRetryableStatus(502), "502 Bad Gateway must retry")
        assertTrue(isRetryableStatus(503), "503 Service Unavailable must retry")
        assertTrue(isRetryableStatus(504), "504 Gateway Timeout must retry")
        assertTrue(isRetryableStatus(599), "any 5xx must retry")
    }

    @Test
    fun `does not retry 400 Bad Request`() {
        assertFalse(isRetryableStatus(400))
    }

    @Test
    fun `does not retry 401 Unauthorized`() {
        assertFalse(isRetryableStatus(401))
    }

    @Test
    fun `does not retry 402 Insufficient Balance`() {
        assertFalse(isRetryableStatus(402))
    }

    @Test
    fun `does not retry 403 Forbidden`() {
        assertFalse(isRetryableStatus(403))
    }

    @Test
    fun `does not retry 404 Not Found`() {
        assertFalse(isRetryableStatus(404))
    }

    @Test
    fun `does not retry 422 Unprocessable Entity`() {
        assertFalse(isRetryableStatus(422))
    }

    @Test
    fun `does not retry 2xx success`() {
        assertFalse(isRetryableStatus(200))
        assertFalse(isRetryableStatus(201))
        assertFalse(isRetryableStatus(204))
    }

    @Test
    fun `does not retry 3xx redirects`() {
        assertFalse(isRetryableStatus(301))
        assertFalse(isRetryableStatus(302))
        assertFalse(isRetryableStatus(304))
    }

    @Test
    fun `delay grows exponentially with retry count`() {
        val base = 500L
        val d1 = computeRetryDelayMillis(retry = 1, baseMillis = base, jitterMillis = 0L)
        val d2 = computeRetryDelayMillis(retry = 2, baseMillis = base, jitterMillis = 0L)
        val d3 = computeRetryDelayMillis(retry = 3, baseMillis = base, jitterMillis = 0L)
        assertEquals(1_000L, d1, "retry=1 should yield base * 2^1")
        assertEquals(2_000L, d2, "retry=2 should yield base * 2^2")
        assertEquals(4_000L, d3, "retry=3 should yield base * 2^3")
    }

    @Test
    fun `delay is capped at maxMillis`() {
        val d = computeRetryDelayMillis(
            retry = 10,
            baseMillis = 500L,
            maxMillis = 5_000L,
            jitterMillis = 0L,
        )
        assertEquals(5_000L, d, "exponential term must be clamped to maxMillis")
    }

    @Test
    fun `delay does not overflow for large retry counts`() {
        val d = computeRetryDelayMillis(
            retry = 60,
            baseMillis = 500L,
            maxMillis = 30_000L,
            jitterMillis = 0L,
        )
        assertEquals(30_000L, d, "large retry counts must still clamp to maxMillis, not overflow")
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
            assertTrue(d >= 1_000L, "delay $d must be >= exponential floor 1000 (seed=$seed)")
            assertTrue(d < 1_000L + jitter, "delay $d must be < 1000 + jitter=$jitter (seed=$seed)")
        }
    }

    @Test
    fun `zero jitter is deterministic`() {
        val d1 = computeRetryDelayMillis(retry = 2, baseMillis = 500L, jitterMillis = 0L)
        val d2 = computeRetryDelayMillis(retry = 2, baseMillis = 500L, jitterMillis = 0L)
        assertEquals(d1, d2)
        assertEquals(2_000L, d1)
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
        assertEquals(d1, d2, "same seed must produce deterministic delay")
    }
}
