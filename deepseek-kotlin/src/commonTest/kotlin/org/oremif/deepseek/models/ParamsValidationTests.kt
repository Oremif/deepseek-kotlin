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
                maxTokens = 1
                topLogprobs = 0
            }
            chatCompletionParams {
                temperature = 2.0
                topP = 1.0
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
    fun `chat params accept maxTokens beyond the retired 8192 cap`() {
        shouldNotThrowAny {
            chatCompletionParams { maxTokens = 384_000 }
            chatCompletionStreamParams { maxTokens = 384_000 }
            fimCompletionParams { maxTokens = 384_000 }
            fimCompletionStreamParams { maxTokens = 384_000 }
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
            chatCompletionStreamParams { maxTokens = 0 }
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

    @Test
    fun `fim params default to the only model the endpoint accepts`() {
        fimCompletionParams { }.model shouldBe ChatModel.DEEPSEEK_V4_PRO
        fimCompletionStreamParams { }.model shouldBe ChatModel.DEEPSEEK_V4_PRO
    }

    @Test
    fun `fim params reject the vision model`() {
        val ex = shouldThrow<IllegalArgumentException> {
            fimCompletionParams { model = ChatModel.DEEPSEEK_V4_FLASH_VISION_EXP }
        }
        ex.message!! shouldContain "deepseek-v4-flash-vision-exp"
        shouldThrow<IllegalArgumentException> {
            fimCompletionStreamParams { model = ChatModel.DEEPSEEK_V4_FLASH_VISION_EXP }
        }
    }

    @Test
    fun `chat params default to the flash model`() {
        chatCompletionParams { }.model shouldBe ChatModel.DEEPSEEK_V4_FLASH
        chatCompletionStreamParams { }.model shouldBe ChatModel.DEEPSEEK_V4_FLASH
    }
}
