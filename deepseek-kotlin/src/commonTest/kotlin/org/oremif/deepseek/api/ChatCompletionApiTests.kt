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
import org.oremif.deepseek.models.ChatModel
import org.oremif.deepseek.models.FinishReason
import org.oremif.deepseek.models.UserMessage
import org.oremif.deepseek.testing.mockEngine
import org.oremif.deepseek.testing.testClient
import kotlin.test.Test

class ChatCompletionApiTests {

    private val successBody = """
        {
            "id": "abc-123",
            "choices": [
                {
                    "finish_reason": "stop",
                    "index": 0,
                    "message": {
                        "content": "Hello!",
                        "role": "assistant"
                    }
                }
            ],
            "created": 1705651092,
            "model": "deepseek-chat",
            "object": "chat.completion",
            "usage": {
                "completion_tokens": 3,
                "prompt_tokens": 8,
                "total_tokens": 11
            }
        }
    """.trimIndent()

    @Test
    fun `chat posts to chat completions endpoint with JSON body`() = runTest {
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

        client.chat("Hi")

        capturedMethod shouldBe HttpMethod.Post
        capturedPath.shouldNotBeNull().shouldEndWith("/chat/completions")
        val body = capturedBody.shouldNotBeNull()
        body shouldContain "\"model\":\"deepseek-chat\""
        body shouldContain "\"content\":\"Hi\""
    }

    @Test
    fun `chat parses a successful response`() = runTest {
        val engine = mockEngine {
            respond(
                content = successBody,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val client = testClient(engine)

        val response = client.chat(listOf(UserMessage("Hi")))

        response.id shouldBe "abc-123"
        response.model shouldBe "deepseek-chat"
        response.choices shouldHaveSize 1
        response.choices[0].finishReason shouldBe FinishReason.STOP
        response.choices[0].message.content shouldBe "Hello!"
        response.usage.shouldNotBeNull().totalTokens shouldBe 11
    }

    @Test
    fun `chat maps 401 to UnauthorizedException with parsed error`() = runTest {
        val engine = mockEngine {
            respond(
                content = """{"error":{"message":"Invalid API key","type":"authentication_error"}}""",
                status = HttpStatusCode.Unauthorized,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val client = testClient(engine)

        val ex = shouldThrow<DeepSeekException.UnauthorizedException> {
            client.chat("Hi")
        }
        ex.statusCode shouldBe 401
        ex.error?.error?.message shouldBe "Invalid API key"
    }

    @Test
    fun `chat maps 503 to OverloadServerException`() = runTest {
        val engine = mockEngine {
            respond(
                content = """{"error":{"message":"overloaded"}}""",
                status = HttpStatusCode.ServiceUnavailable,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val client = testClient(engine)

        val ex = shouldThrow<DeepSeekException.OverloadServerException> {
            client.chat("Hi")
        }
        ex.statusCode shouldBe 503
    }

    @Test
    fun `chatCompletion DSL builder sends messages and model`() = runTest {
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

        client.chatCompletion {
            params { model = ChatModel.DEEPSEEK_REASONER }
            messages {
                system("You are helpful")
                user("Hi")
            }
        }

        val body = capturedBody.shouldNotBeNull()
        body shouldContain "\"model\":\"deepseek-reasoner\""
        body shouldContain "\"role\":\"system\""
        body shouldContain "\"role\":\"user\""
    }
}
