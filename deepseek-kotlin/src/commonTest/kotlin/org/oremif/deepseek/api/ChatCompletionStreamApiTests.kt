package org.oremif.deepseek.api

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.oremif.deepseek.models.ChatCompletionRequest
import org.oremif.deepseek.models.ChatModel
import org.oremif.deepseek.models.FinishReason
import org.oremif.deepseek.models.StreamOptions
import org.oremif.deepseek.models.UserMessage
import org.oremif.deepseek.models.chatCompletionStreamParams
import org.oremif.deepseek.testing.sseMockEngine
import org.oremif.deepseek.testing.testStreamClient
import kotlin.test.Test

class ChatCompletionStreamApiTests {

    private val request = ChatCompletionRequest(
        messages = listOf(UserMessage("Hi")),
        model = ChatModel.DEEPSEEK_CHAT,
        stream = true,
    )

    private fun sseBody(vararg events: String): String =
        events.joinToString(separator = "") { "data: $it\n\n" }

    @Test
    fun `chat stream delivers chunks and ignores DONE marker`() = runTest {
        val chunks = arrayOf(
            """{"id":"c1","choices":[{"delta":{"role":"assistant","content":""},"index":0,"finish_reason":null}],"created":1,"model":"deepseek-chat","object":"chat.completion.chunk"}""",
            """{"id":"c1","choices":[{"delta":{"content":"Hello"},"index":0,"finish_reason":null}],"created":1,"model":"deepseek-chat","object":"chat.completion.chunk"}""",
            """{"id":"c1","choices":[{"delta":{"content":"!"},"index":0,"finish_reason":"stop"}],"created":1,"model":"deepseek-chat","object":"chat.completion.chunk"}""",
            "[DONE]",
        )
        val engine = sseMockEngine {
            respond(
                content = sseBody(*chunks),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Text.EventStream.toString()),
            )
        }
        val client = testStreamClient(engine)

        val received = client.chatCompletionStream(request).toList()

        received shouldHaveSize 3
        received[0].choices[0].delta.role shouldBe "assistant"
        received[1].choices[0].delta.content shouldBe "Hello"
        received[2].choices[0].delta.content shouldBe "!"
        received[2].choices[0].finishReason shouldBe FinishReason.STOP
    }

    @Test
    fun `chat stream with include_usage yields final usage-only chunk before DONE`() = runTest {
        val chunks = arrayOf(
            """{"id":"c1","choices":[{"delta":{"content":"Hi"},"index":0,"finish_reason":null}],"created":1,"model":"deepseek-chat","object":"chat.completion.chunk"}""",
            """{"id":"c1","choices":[{"delta":{},"index":0,"finish_reason":"stop"}],"created":1,"model":"deepseek-chat","object":"chat.completion.chunk"}""",
            """{"id":"c1","choices":[],"created":1,"model":"deepseek-chat","object":"chat.completion.chunk","usage":{"prompt_tokens":3,"completion_tokens":2,"total_tokens":5}}""",
            "[DONE]",
        )
        val engine = sseMockEngine {
            respond(
                content = sseBody(*chunks),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Text.EventStream.toString()),
            )
        }
        val client = testStreamClient(engine)

        val received = client.chat(
            chatCompletionStreamParams {
                streamOptions = StreamOptions(includeUsage = true)
            },
            listOf(UserMessage("Hi")),
        ).toList()

        received shouldHaveSize 3
        val last = received.last()
        last.choices.shouldBeEmpty()
        val usage = last.usage.shouldNotBeNull()
        usage.promptTokens shouldBe 3
        usage.completionTokens shouldBe 2
        usage.totalTokens shouldBe 5
    }

    @Test
    fun `chat stream flow completes without items when server sends only DONE`() = runTest {
        val engine = sseMockEngine {
            respond(
                content = sseBody("[DONE]"),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Text.EventStream.toString()),
            )
        }
        val client = testStreamClient(engine)

        val received = client.chatCompletionStream(request).toList()

        received.shouldBeEmpty()
    }

    @Test
    fun `chat stream preserves tool_calls delta fields`() = runTest {
        val chunks = arrayOf(
            """{"id":"c1","choices":[{"delta":{"role":"assistant","tool_calls":[{"index":0,"id":"call_1","type":"function","function":{"name":"sum","arguments":""}}]},"index":0,"finish_reason":null}],"created":1,"model":"deepseek-chat","object":"chat.completion.chunk"}""",
            """{"id":"c1","choices":[{"delta":{"tool_calls":[{"index":0,"function":{"arguments":"{\"a\":1,\"b\":2}"}}]},"index":0,"finish_reason":null}],"created":1,"model":"deepseek-chat","object":"chat.completion.chunk"}""",
            """{"id":"c1","choices":[{"delta":{},"index":0,"finish_reason":"tool_calls"}],"created":1,"model":"deepseek-chat","object":"chat.completion.chunk"}""",
            "[DONE]",
        )
        val engine = sseMockEngine {
            respond(
                content = sseBody(*chunks),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Text.EventStream.toString()),
            )
        }
        val client = testStreamClient(engine)

        val received = client.chatCompletionStream(request).toList()

        received shouldHaveSize 3
        val first = received[0].choices[0].delta.toolCalls.shouldNotBeNull().single()
        first.id shouldBe "call_1"
        first.function?.name shouldBe "sum"
        val second = received[1].choices[0].delta.toolCalls.shouldNotBeNull().single()
        second.function?.arguments shouldBe """{"a":1,"b":2}"""
        received[2].choices[0].finishReason shouldBe FinishReason.TOOL_CALLS
        received[2].choices[0].delta.toolCalls.shouldBeNull()
    }
}
