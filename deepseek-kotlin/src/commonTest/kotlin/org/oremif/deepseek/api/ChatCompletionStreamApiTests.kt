package org.oremif.deepseek.api

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.oremif.deepseek.models.*
import org.oremif.deepseek.testing.sseMockEngine
import org.oremif.deepseek.testing.testStreamClient
import kotlin.test.Test

class ChatCompletionStreamApiTests {

    private val request = ChatCompletionRequest(
        messages = listOf(UserMessage("Hi")),
        model = ChatModel.DEEPSEEK_V4_FLASH,
        stream = true,
    )

    private fun sseBody(vararg events: String): String =
        events.joinToString(separator = "") { "data: $it\n\n" }

    @Test
    fun `chat stream delivers chunks and ignores DONE marker`() = runTest {
        val chunks = arrayOf(
            """{"id":"c1","choices":[{"delta":{"role":"assistant","content":""},"index":0,"finish_reason":null}],"created":1,"model":"deepseek-v4-flash","object":"chat.completion.chunk"}""",
            """{"id":"c1","choices":[{"delta":{"content":"Hello"},"index":0,"finish_reason":null}],"created":1,"model":"deepseek-v4-flash","object":"chat.completion.chunk"}""",
            """{"id":"c1","choices":[{"delta":{"content":"!"},"index":0,"finish_reason":"stop"}],"created":1,"model":"deepseek-v4-flash","object":"chat.completion.chunk"}""",
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
    fun `chat stream with include_usage reports usage on the final content chunk`() = runTest {
        val chunks = arrayOf(
            """{"id":"c1","choices":[{"delta":{"content":"Hi"},"index":0,"finish_reason":null}],"created":1,"model":"deepseek-v4-flash","object":"chat.completion.chunk","usage":null}""",
            """{"id":"c1","choices":[{"delta":{},"index":0,"finish_reason":"stop"}],"created":1,"model":"deepseek-v4-flash","object":"chat.completion.chunk","usage":{"prompt_tokens":3,"completion_tokens":2,"prompt_cache_hit_tokens":0,"prompt_cache_miss_tokens":3,"total_tokens":5,"completion_tokens_details":{"reasoning_tokens":1}}}""",
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

        received shouldHaveSize 2
        received[0].usage.shouldBeNull()
        val last = received.last()
        last.choices.single().finishReason shouldBe FinishReason.STOP
        val usage = last.usage.shouldNotBeNull()
        usage.promptTokens shouldBe 3
        usage.completionTokens shouldBe 2
        usage.promptCacheHitTokens shouldBe 0
        usage.promptCacheMissTokens shouldBe 3
        usage.completionTokensDetails.shouldNotBeNull().reasoningTokens shouldBe 1
        usage.totalTokens shouldBe 5
    }

    @Test
    fun `chat stream consumes the documented SSE sequence`() = runTest {
        val fp = """"system_fingerprint": "fp_a49d71b8a1""""
        val id = """"id": "1f633d8bfc032625086f14113c411638""""

        fun chunk(delta: String, finishReason: String = "null", tail: String = "") =
            """{$id, "choices": [{"index": 0, "delta": {$delta}, "finish_reason": $finishReason, "logprobs": null}], """ +
                    """"created": 1718345013, "model": "deepseek-v4-pro", $fp, "object": "chat.completion.chunk"$tail}"""

        val words = listOf("Hello", "!", " How", " can", " I", " assist", " you", " today", "?")
        val chunks = buildList {
            add(chunk(""""content": "", "role": "assistant"""", tail = """, "usage": null"""))
            words.forEach { add(chunk(""""content": "$it", "role": "assistant"""")) }
            add(
                chunk(
                    """"content": "", "role": null""",
                    finishReason = "\"stop\"",
                    tail = """, "usage": {"completion_tokens": 9, "prompt_tokens": 17, "total_tokens": 26}""",
                )
            )
            add("[DONE]")
        }

        val engine = sseMockEngine {
            respond(
                content = sseBody(*chunks.toTypedArray()),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Text.EventStream.toString()),
            )
        }
        val client = testStreamClient(engine)

        val received = client.chatCompletionStream(request).toList()

        received shouldHaveSize 11
        received.joinToString("") { it.choices.single().delta.content.orEmpty() } shouldBe
                "Hello! How can I assist you today?"
        received.forEach { it.systemFingerprint shouldBe "fp_a49d71b8a1" }

        val first = received.first()
        first.usage.shouldBeNull()
        first.choices.single().delta.role shouldBe "assistant"
        first.choices.single().finishReason.shouldBeNull()

        val last = received.last()
        last.choices.single().finishReason shouldBe FinishReason.STOP
        last.choices.single().delta.role.shouldBeNull()
        last.choices.single().delta.content shouldBe ""
        val usage = last.usage.shouldNotBeNull()
        usage.promptTokens shouldBe 17
        usage.completionTokens shouldBe 9
        usage.totalTokens shouldBe 26
    }

    @Test
    fun `chat stream still accepts a legacy usage-only chunk with empty choices`() = runTest {
        val chunks = arrayOf(
            """{"id":"c1","choices":[{"delta":{"content":"Hi"},"index":0,"finish_reason":"stop"}],"created":1,"model":"deepseek-v4-flash","object":"chat.completion.chunk"}""",
            """{"id":"c1","choices":[],"created":1,"model":"deepseek-v4-flash","object":"chat.completion.chunk","usage":{"prompt_tokens":3,"completion_tokens":2,"total_tokens":5}}""",
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

        received shouldHaveSize 2
        val last = received.last()
        last.choices.shouldBeEmpty()
        last.usage.shouldNotBeNull().totalTokens shouldBe 5
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
            """{"id":"c1","choices":[{"delta":{"role":"assistant","tool_calls":[{"index":0,"id":"call_1","type":"function","function":{"name":"sum","arguments":""}}]},"index":0,"finish_reason":null}],"created":1,"model":"deepseek-v4-flash","object":"chat.completion.chunk"}""",
            """{"id":"c1","choices":[{"delta":{"tool_calls":[{"index":0,"function":{"arguments":"{\"a\":1,\"b\":2}"}}]},"index":0,"finish_reason":null}],"created":1,"model":"deepseek-v4-flash","object":"chat.completion.chunk"}""",
            """{"id":"c1","choices":[{"delta":{},"index":0,"finish_reason":"tool_calls"}],"created":1,"model":"deepseek-v4-flash","object":"chat.completion.chunk"}""",
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
