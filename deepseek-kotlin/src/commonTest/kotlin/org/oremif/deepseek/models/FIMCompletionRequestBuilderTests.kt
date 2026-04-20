package org.oremif.deepseek.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FIMCompletionRequestBuilderTests {

    @Test
    fun builderFailsWhenPromptIsNotCalled() {
        val ex = assertFailsWith<IllegalArgumentException> {
            FIMCompletionRequest.Builder().build()
        }
        assertEquals("prompt(...) must be called", ex.message)
    }

    @Test
    fun streamBuilderFailsWhenPromptIsNotCalled() {
        val ex = assertFailsWith<IllegalArgumentException> {
            FIMCompletionRequest.StreamBuilder().build()
        }
        assertEquals("prompt(...) must be called", ex.message)
    }

    @Test
    fun builderSucceedsWhenPromptIsCalled() {
        val request = FIMCompletionRequest.Builder().apply {
            prompt("def fib(n):")
        }.build()
        assertEquals("def fib(n):", request.prompt)
    }

    @Test
    fun streamBuilderSucceedsWhenPromptIsCalled() {
        val request = FIMCompletionRequest.StreamBuilder().apply {
            prompt("def fib(n):")
        }.build()
        assertEquals("def fib(n):", request.prompt)
    }
}
