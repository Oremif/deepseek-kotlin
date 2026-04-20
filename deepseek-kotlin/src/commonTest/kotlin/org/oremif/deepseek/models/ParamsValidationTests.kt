package org.oremif.deepseek.models

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test

class ParamsValidationTests {

    @Test
    fun `chat params accept boundary values`() {
        shouldNotThrowAny {
            chatCompletionParams {
                temperature = 0.0
                topP = 0.0
                frequencyPenalty = -2.0
                presencePenalty = 2.0
                maxTokens = 1
                topLogprobs = 0
            }
            chatCompletionParams {
                temperature = 2.0
                topP = 1.0
                frequencyPenalty = 2.0
                presencePenalty = -2.0
                maxTokens = 8192
                topLogprobs = 20
            }
        }
    }

    @Test
    fun `chat params reject topP above 1_0`() {
        val ex = shouldThrow<IllegalArgumentException> {
            chatCompletionParams { topP = 1.5 }
        }
        ex.message!! shouldContain "topP"
    }

    @Test
    fun `chat params reject negative topP`() {
        shouldThrow<IllegalArgumentException> {
            chatCompletionParams { topP = -0.1 }
        }
    }

    @Test
    fun `chat params reject temperature above 2_0`() {
        val ex = shouldThrow<IllegalArgumentException> {
            chatCompletionParams { temperature = 2.5 }
        }
        ex.message!! shouldContain "temperature"
    }

    @Test
    fun `chat params reject maxTokens of 0`() {
        shouldThrow<IllegalArgumentException> {
            chatCompletionParams { maxTokens = 0 }
        }
    }

    @Test
    fun `chat params reject maxTokens above 8192`() {
        shouldThrow<IllegalArgumentException> {
            chatCompletionParams { maxTokens = 9000 }
        }
    }

    @Test
    fun `chat params reject topLogprobs above 20`() {
        shouldThrow<IllegalArgumentException> {
            chatCompletionParams { topLogprobs = 21 }
        }
    }

    @Test
    fun `chat stream params validate the same boundaries`() {
        shouldThrow<IllegalArgumentException> {
            chatCompletionStreamParams { topP = 1.01 }
        }
        shouldThrow<IllegalArgumentException> {
            chatCompletionStreamParams { frequencyPenalty = -3.0 }
        }
    }

    @Test
    fun `chat stream params force stream = true`() {
        val params = chatCompletionStreamParams { temperature = 0.7 }
        params.stream shouldBe true
    }

    @Test
    fun `fim params reject logprobs above 20`() {
        shouldThrow<IllegalArgumentException> {
            fimCompletionParams { logprobs = 21 }
        }
    }

    @Test
    fun `fim params reject topP above 1_0`() {
        val ex = shouldThrow<IllegalArgumentException> {
            fimCompletionParams { topP = 1.1 }
        }
        ex.message!! shouldContain "topP"
    }

    @Test
    fun `fim params accept boundary values`() {
        shouldNotThrowAny {
            fimCompletionParams {
                temperature = 0.0
                topP = 1.0
                frequencyPenalty = -2.0
                presencePenalty = 2.0
                maxTokens = 1
                logprobs = 20
            }
        }
    }

    @Test
    fun `fim stream params force stream = true`() {
        val params = fimCompletionStreamParams { maxTokens = 100 }
        params.stream shouldBe true
    }
}
