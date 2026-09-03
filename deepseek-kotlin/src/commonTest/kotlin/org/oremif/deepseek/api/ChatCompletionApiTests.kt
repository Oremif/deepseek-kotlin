package org.oremif.deepseek.api

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldEndWith
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.oremif.deepseek.errors.DeepSeekException
import org.oremif.deepseek.models.*
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
            "model": "deepseek-v4-flash",
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
        body shouldContain "\"model\":\"deepseek-v4-flash\""
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
        response.model shouldBe "deepseek-v4-flash"
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
            params { model = ChatModel.DEEPSEEK_V4_PRO }
            messages {
                system("You are helpful")
                user("Hi")
            }
        }

        val body = capturedBody.shouldNotBeNull()
        body shouldContain "\"model\":\"deepseek-v4-pro\""
        body shouldContain "\"role\":\"system\""
        body shouldContain "\"role\":\"user\""
    }

    @Test
    fun `chatCompletion DSL sends reasoning_effort user_id and strict tools`() = runTest {
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
            params {
                model = ChatModel.DEEPSEEK_V4_PRO
                thinking = Thinking(ThinkingType.ENABLED)
                reasoningEffort = ReasoningEffort.MAX
                userId = "user-42"
                tools = listOf(
                    Tool(
                        type = ToolCallType.FUNCTION,
                        function = FunctionRequest(
                            name = "get_weather",
                            description = "Get the weather",
                            parameters = buildJsonObject { put("type", JsonPrimitive("object")) },
                            strict = true,
                        ),
                    )
                )
            }
            messages { user("Hi") }
        }

        val body = capturedBody.shouldNotBeNull()
        body shouldContain "\"thinking\":{\"type\":\"enabled\"}"
        body shouldContain "\"reasoning_effort\":\"max\""
        body shouldContain "\"user_id\":\"user-42\""
        body shouldContain "\"strict\":true"
    }

    @Test
    fun `chat DSL sends a multimodal user message as content parts`() = runTest {
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

        client.chat {
            user {
                text("What is in this image?")
                image("https://example.com/cat.jpg", ImageDetail.LOW)
                imageFile("file-api-abc123")
                imageData("data:image/png;base64,AAAA", filename = "inline.png")
            }
        }

        val body = capturedBody.shouldNotBeNull()
        body shouldContain """"content":[{"type":"text","text":"What is in this image?"}"""
        body shouldContain """{"type":"image_url","image_url":{"url":"https://example.com/cat.jpg","detail":"low"}}"""
        body shouldContain """{"type":"file","file_id":"file-api-abc123"}"""
        body shouldContain """{"type":"file","file_data":"data:image/png;base64,AAAA","filename":"inline.png"}"""
    }

    @Test
    fun `chat DSL rejects a user message with no content parts`() = runTest {
        val engine = mockEngine {
            respond(
                content = successBody,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val client = testClient(engine)

        shouldThrow<IllegalArgumentException> {
            client.chat { user { } }
        }
    }

    @Test
    fun `chat routes a trailing prefix message to the beta endpoint`() = runTest {
        var capturedPath: String? = null
        val engine = mockEngine { request ->
            capturedPath = request.url.encodedPath
            respond(
                content = successBody,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val client = testClient(engine)

        client.chat(
            listOf(
                UserMessage("Write a Kotlin function that sums two numbers"),
                AssistantMessage("fun sum(a: Int, b: Int) =", prefix = true),
            )
        )

        capturedPath shouldBe "/beta/chat/completions"
    }

    @Test
    fun `chat keeps the standard endpoint when the prefix message is not last`() = runTest {
        var capturedPath: String? = null
        val engine = mockEngine { request ->
            capturedPath = request.url.encodedPath
            respond(
                content = successBody,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val client = testClient(engine)

        client.chat(
            listOf(
                AssistantMessage("fun sum(a: Int, b: Int) =", prefix = true),
                UserMessage("Now explain it"),
            )
        )

        capturedPath shouldBe "/chat/completions"
    }

    @Test
    fun `chat keeps the standard endpoint for an assistant message without prefix`() = runTest {
        var capturedPath: String? = null
        val engine = mockEngine { request ->
            capturedPath = request.url.encodedPath
            respond(
                content = successBody,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val client = testClient(engine)

        client.chat(listOf(UserMessage("Hi"), AssistantMessage("Hello!")))

        capturedPath shouldBe "/chat/completions"
    }
}
