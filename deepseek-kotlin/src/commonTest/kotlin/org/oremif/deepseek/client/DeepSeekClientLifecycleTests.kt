package org.oremif.deepseek.client

import io.kotest.matchers.booleans.shouldBeTrue
import kotlin.test.Test
import kotlinx.coroutines.job
import kotlinx.coroutines.test.runTest

class DeepSeekClientLifecycleTests {

    @Test
    fun `closeAndJoin completes the client coroutine scope`() = runTest {
        val client = DeepSeekClient("test-token")
        val job = client.client.coroutineContext.job
        client.closeAndJoin()
        job.isCompleted.shouldBeTrue()
    }

    @Test
    fun `closeAndJoin completes the stream client coroutine scope`() = runTest {
        val client = DeepSeekClientStream("test-token")
        val job = client.client.coroutineContext.job
        client.closeAndJoin()
        job.isCompleted.shouldBeTrue()
    }
}
