package org.oremif.deepseek.models

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class FIMCompletionRequestBuilderTests {

    @Test
    fun builderFailsWhenPromptIsNotCalled() {
        val ex = shouldThrow<IllegalArgumentException> {
            FIMCompletionRequest.Builder().build()
        }
        ex.message shouldBe "prompt(...) must be called"
    }

    @Test
    fun streamBuilderFailsWhenPromptIsNotCalled() {
        val ex = shouldThrow<IllegalArgumentException> {
            FIMCompletionRequest.StreamBuilder().build()
        }
        ex.message shouldBe "prompt(...) must be called"
    }

    @Test
    fun builderSucceedsWhenPromptIsCalled() {
        val request = FIMCompletionRequest.Builder().apply {
            prompt("def fib(n):")
        }.build()
        request.prompt shouldBe "def fib(n):"
    }

    @Test
    fun streamBuilderSucceedsWhenPromptIsCalled() {
        val request = FIMCompletionRequest.StreamBuilder().apply {
            prompt("def fib(n):")
        }.build()
        request.prompt shouldBe "def fib(n):"
    }
}
