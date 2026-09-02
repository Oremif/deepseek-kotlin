package org.oremif.deepseek.api

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldEndWith
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import org.oremif.deepseek.errors.DeepSeekException
import org.oremif.deepseek.models.ChatModel
import org.oremif.deepseek.models.FinishReason
import org.oremif.deepseek.models.fimCompletionParams
import org.oremif.deepseek.testing.mockEngine
import org.oremif.deepseek.testing.testClient
import kotlin.test.Test

class FIMCompletionApiTests {

    private val successBody = """
        {
            "id": "fim-1",
            "choices": [
                {
                    "text": "    return a + b\n",
                    "index": 0,
                    "finish_reason": "stop"
                }
            ],
            "created": 1705651092,
            "model": "deepseek-v4-pro",
            "object": "text_completion",
            "usage": {
                "completion_tokens": 5,
                "prompt_tokens": 7,
                "total_tokens": 12
            }
        }
    """.trimIndent()

    @Test
    fun `fim posts to beta completions endpoint with prompt in body`() = runTest {
        var capturedMethod: HttpMethod? = null
        var capturedPath: String? = null
        var capturedBody: String? = null
        val engine = mockEngine { request ->
            capturedMethod = request.method
            capturedPath = request.url.encodedPath
            capturedBody = request.body.toByteArray().decodeToString()
            respond(
                content = successBody,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val client = testClient(engine)

        client.fim("def add(a, b):")

        capturedMethod shouldBe HttpMethod.Post
        capturedPath.shouldNotBeNull().shouldEndWith("/beta/completions")
        val body = capturedBody.shouldNotBeNull()
        body shouldContain "\"prompt\":\"def add(a, b):\""
        body shouldContain "\"model\":\"deepseek-v4-pro\""
    }

    @Test
    fun `fim parses a successful response`() = runTest {
        val engine = mockEngine {
            respond(
                content = successBody,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val client = testClient(engine)

        val response = client.fim("def add(a, b):")

        response.id shouldBe "fim-1"
        response.`object` shouldBe "text_completion"
        response.choices shouldHaveSize 1
        response.choices[0].text shouldBe "    return a + b\n"
        response.choices[0].finishReason shouldBe FinishReason.STOP
        response.usage.shouldNotBeNull().totalTokens shouldBe 12
    }

    @Test
    fun `fim sends suffix when provided via params`() = runTest {
        var capturedBody: String? = null
        val engine = mockEngine { request ->
            capturedBody = request.body.toByteArray().decodeToString()
            respond(
                content = successBody,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val client = testClient(engine)

        client.fim(
            fimCompletionParams {
                suffix = "\n# end"
                maxTokens = 64
            },
            "def add(a, b):",
        )

        val body = capturedBody.shouldNotBeNull()
        body shouldContain "\"suffix\":\"\\n# end\""
        body shouldContain "\"max_tokens\":64"
    }

    @Test
    fun `fim sends the model configured in params`() = runTest {
        var capturedBody: String? = null
        val engine = mockEngine { request ->
            capturedBody = request.body.toByteArray().decodeToString()
            respond(
                content = successBody,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val client = testClient(engine)

        client.fim(
            fimCompletionParams { model = ChatModel("deepseek-v5-pro") },
            "def add(a, b):",
        )

        capturedBody.shouldNotBeNull() shouldContain "\"model\":\"deepseek-v5-pro\""
    }

    @Test
    fun `fim parses the documented response shape with logprobs and cache usage`() = runTest {
        val engine = mockEngine {
            respond(
                content = """
                    {
                      "id": "fim-1",
                      "choices": [
                        {
                          "finish_reason": "stop",
                          "index": 0,
                          "logprobs": {
                            "text_offset": [0],
                            "token_logprobs": [-0.25],
                            "tokens": ["    return a + b"],
                            "top_logprobs": [{"    return a + b": -0.25, " return": -9999.0}]
                          },
                          "text": "    return a + b"
                        }
                      ],
                      "created": 1705651092,
                      "model": "deepseek-v4-pro",
                      "system_fingerprint": "fp_a49d71b8a1",
                      "object": "text_completion",
                      "usage": {
                        "completion_tokens": 5,
                        "prompt_tokens": 7,
                        "prompt_cache_hit_tokens": 4,
                        "prompt_cache_miss_tokens": 3,
                        "total_tokens": 12,
                        "completion_tokens_details": {"reasoning_tokens": 2}
                      }
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val client = testClient(engine)

        val response = client.fim("def add(a, b):")

        response.systemFingerprint shouldBe "fp_a49d71b8a1"
        val choice = response.choices.single()
        choice.text shouldBe "    return a + b"
        choice.finishReason shouldBe FinishReason.STOP
        val logprobs = choice.logprobs.shouldNotBeNull()
        logprobs.textOffset shouldBe listOf(0)
        logprobs.tokenLogprobs shouldBe listOf(-0.25)
        logprobs.tokens shouldBe listOf("    return a + b")
        logprobs.topLogprobs.shouldNotBeNull().single()[" return"] shouldBe -9999.0

        val usage = response.usage.shouldNotBeNull()
        usage.promptCacheHitTokens shouldBe 4
        usage.promptCacheMissTokens shouldBe 3
        usage.promptTokensDetails.shouldBeNull()
        usage.completionTokensDetails.shouldNotBeNull().reasoningTokens shouldBe 2
    }

    @Test
    fun `fim maps 402 to InsufficientBalanceException`() = runTest {
        val engine = mockEngine {
            respond(
                content = """{"error":{"message":"Insufficient Balance","type":"insufficient_balance_error"}}""",
                status = HttpStatusCode.PaymentRequired,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val client = testClient(engine)

        val ex = shouldThrow<DeepSeekException.InsufficientBalanceException> {
            client.fim("def foo():")
        }
        ex.statusCode shouldBe 402
        ex.error?.error?.message shouldBe "Insufficient Balance"
    }
}
