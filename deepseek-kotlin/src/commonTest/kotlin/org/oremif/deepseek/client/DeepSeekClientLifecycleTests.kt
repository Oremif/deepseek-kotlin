package org.oremif.deepseek.client

import kotlinx.coroutines.job
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class DeepSeekClientLifecycleTests {

    @Test
    fun `closeAndJoin completes the client coroutine scope`() = runTest {
        val client = DeepSeekClient("test-token")
        val job = client.client.coroutineContext.job
        client.closeAndJoin()
        assertTrue(job.isCompleted, "Client coroutine scope must complete after closeAndJoin")
    }

    @Test
    fun `closeAndJoin completes the stream client coroutine scope`() = runTest {
        val client = DeepSeekClientStream("test-token")
        val job = client.client.coroutineContext.job
        client.closeAndJoin()
        assertTrue(
            job.isCompleted,
            "Stream client coroutine scope must complete after closeAndJoin"
        )
    }
}
