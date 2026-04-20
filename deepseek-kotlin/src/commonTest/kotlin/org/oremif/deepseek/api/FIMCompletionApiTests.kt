package org.oremif.deepseek.api

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldEndWith
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.oremif.deepseek.errors.DeepSeekException
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
            "model": "deepseek-chat",
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
        body shouldContain "\"model\":\"deepseek-chat\""
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
