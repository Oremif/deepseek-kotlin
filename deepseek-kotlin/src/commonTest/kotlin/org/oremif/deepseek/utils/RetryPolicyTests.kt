package org.oremif.deepseek.utils

import kotlin.test.Test
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
}
