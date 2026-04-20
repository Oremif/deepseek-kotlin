package org.oremif.deepseek.models

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import kotlin.test.Test

class ChatCompletionTests {

    @OptIn(ExperimentalSerializationApi::class)
    private val jsonConfig = Json {
        prettyPrint = true
        namingStrategy = JsonNamingStrategy.SnakeCase
    }

    val jsonRequest = """
            {
                "messages": [
                    {
                        "role": "system",
                        "content": "You are a helpful assistant"
                    },
                    {
                        "role": "user",
                        "content": "Hi"
                    }
                ],
                "model": "deepseek-chat",
                "frequency_penalty": 0.0,
                "max_tokens": 2048,
                "presence_penalty": 0.0,
                "response_format": {
                    "type": "text"
                },
                "stream": false,
                "temperature": 1.0,
                "top_p": 1.0,
                "tool_choice": "none",
                "logprobs": false
            }
        """.trimIndent()

    val jsonResponse = """
            {
                "id": "930c60df-bf64-41c9-a88e-3ec75f81e00e",
                "choices": [
                    {
                        "finish_reason": "stop",
                        "index": 0,
                        "message": {
                            "content": "Hello! How can I help you today?",
                            "role": "assistant"
                        }
                    }
                ],
                "created": 1705651092,
                "model": "deepseek-chat",
                "object": "chat.completion",
                "usage": {
                    "completion_tokens": 10,
                    "prompt_tokens": 16,
                    "total_tokens": 26
                }
            }
    """.trimIndent()

    @Test
    fun `chat completion request model test`() {
        val request = ChatCompletionRequest(
            messages = listOf(
                SystemMessage("You are a helpful assistant"),
                UserMessage("Hi"),
            ),
            model = ChatModel.DEEPSEEK_CHAT,
            frequencyPenalty = .0,
            maxTokens = 2048,
            presencePenalty = .0,
            responseFormat = ResponseFormat.text,
            stop = null,
            stream = false,
            streamOptions = null,
            temperature = 1.0,
            topP = 1.0,
            tools = null,
            toolChoice = ChatCompletionToolChoice.NONE,
            logprobs = false,
            topLogprobs = null
        )

        val expected = jsonConfig.decodeFromString<ChatCompletionRequest>(jsonRequest)
        expected.messages.size shouldBe 2
        expected.maxTokens shouldBe 2048
        expected.responseFormat shouldBe ResponseFormat.text

        jsonConfig.encodeToString(request).trimIndent() shouldBe jsonRequest
    }

    @Test
    fun `chat completion response model test`() {
        val response = ChatCompletion(
            id = "930c60df-bf64-41c9-a88e-3ec75f81e00e",
            choices = listOf(
                ChatChoice(
                    finishReason = FinishReason.STOP,
                    index = 0,
                    message = ChatCompletionMessage("Hello! How can I help you today?")
                )
            ),
            created = 1705651092L,
            model = "deepseek-chat",
            `object` = "chat.completion",
            usage = Usage(
                completionTokens = 10,
                promptTokens = 16,
                totalTokens = 26
            )
        )

        val expected = jsonConfig.decodeFromString<ChatCompletion>(jsonResponse)
        expected.choices.size shouldBe 1
        expected.choices[0].finishReason shouldBe FinishReason.STOP
        expected.choices[0].message.content shouldBe "Hello! How can I help you today?"
        expected.created shouldBe 1705651092L
        jsonConfig.encodeToString(response).trimIndent() shouldBe jsonResponse
    }

    @OptIn(ExperimentalSerializationApi::class)
    private val streamingJsonConfig = Json {
        ignoreUnknownKeys = true
        namingStrategy = JsonNamingStrategy.SnakeCase
    }

    @Test
    fun `streaming delta tool_calls first chunk parses with id and name and empty arguments`() {
        val json = """
            {
                "id": "chunk-1",
                "choices": [
                    {
                        "delta": {
                            "role": "assistant",
                            "content": null,
                            "tool_calls": [
                                {
                                    "index": 0,
                                    "id": "call_abc",
                                    "type": "function",
                                    "function": {"name": "get_weather", "arguments": ""}
                                }
                            ]
                        },
                        "finish_reason": null,
                        "index": 0
                    }
                ],
                "created": 1705651092,
                "model": "deepseek-chat",
                "object": "chat.completion.chunk"
            }
        """.trimIndent()

        val chunk = streamingJsonConfig.decodeFromString<ChatCompletionChunk>(json)
        val delta = chunk.choices.single().delta
        delta.role shouldBe "assistant"
        delta.content.shouldBeNull()
        val toolCall = delta.toolCalls.shouldNotBeNull().single()
        toolCall.index shouldBe 0
        toolCall.id shouldBe "call_abc"
        toolCall.type shouldBe ToolCallType.FUNCTION
        toolCall.function?.name shouldBe "get_weather"
        toolCall.function?.arguments shouldBe ""
    }

    @Test
    fun `streaming delta tool_calls continuation chunk carries partial arguments`() {
        val json = """
            {
                "id": "chunk-2",
                "choices": [
                    {
                        "delta": {
                            "tool_calls": [
                                {
                                    "index": 0,
                                    "function": {"arguments": "{\"location\":"}
                                }
                            ]
                        },
                        "finish_reason": null,
                        "index": 0
                    }
                ],
                "created": 1705651092,
                "model": "deepseek-chat",
                "object": "chat.completion.chunk"
            }
        """.trimIndent()

        val chunk = streamingJsonConfig.decodeFromString<ChatCompletionChunk>(json)
        val toolCall = chunk.choices.single().delta.toolCalls.shouldNotBeNull().single()
        toolCall.index shouldBe 0
        toolCall.id.shouldBeNull()
        toolCall.type.shouldBeNull()
        toolCall.function?.arguments shouldBe """{"location":"""
    }

    @Test
    fun `streaming delta terminal chunk has finish_reason tool_calls and empty delta`() {
        val json = """
            {
                "id": "chunk-final",
                "choices": [
                    {
                        "delta": {},
                        "finish_reason": "tool_calls",
                        "index": 0
                    }
                ],
                "created": 1705651092,
                "model": "deepseek-chat",
                "object": "chat.completion.chunk"
            }
        """.trimIndent()

        val chunk = streamingJsonConfig.decodeFromString<ChatCompletionChunk>(json)
        val choice = chunk.choices.single()
        choice.finishReason shouldBe FinishReason.TOOL_CALLS
        choice.delta.content.shouldBeNull()
        choice.delta.toolCalls.shouldBeNull()
    }

    @Test
    fun `streaming delta reasoning_content is preserved`() {
        val json = """
            {
                "id": "chunk-reasoning",
                "choices": [
                    {
                        "delta": {"reasoning_content": "thinking..."},
                        "finish_reason": null,
                        "index": 0
                    }
                ],
                "created": 1705651092,
                "model": "deepseek-reasoner",
                "object": "chat.completion.chunk"
            }
        """.trimIndent()

        val chunk = streamingJsonConfig.decodeFromString<ChatCompletionChunk>(json)
        chunk.choices.single().delta.reasoningContent shouldBe "thinking..."
    }

    @Test
    fun `ChatCompletionMessage toolCalls is null when tool_calls key is absent`() {
        val json = """
            {
                "role": "assistant",
                "content": "Hello!"
            }
        """.trimIndent()

        val message = jsonConfig.decodeFromString<ChatCompletionMessage>(json)
        message.content shouldBe "Hello!"
        message.toolCalls.shouldBeNull()
    }

    @Test
    fun `ChatCompletionMessage toolCalls is null when tool_calls is JsonNull`() {
        val json = """
            {
                "role": "assistant",
                "content": "Hello!",
                "tool_calls": null
            }
        """.trimIndent()

        val message = jsonConfig.decodeFromString<ChatCompletionMessage>(json)
        message.content shouldBe "Hello!"
        message.toolCalls.shouldBeNull()
    }

    @Test
    fun `ChatCompletionMessage toolCalls is deserialized when present`() {
        val json = """
            {
                "role": "assistant",
                "content": null,
                "tool_calls": [
                    {
                        "id": "call_abc",
                        "type": "function",
                        "function": {"name": "get_weather", "arguments": {}}
                    }
                ]
            }
        """.trimIndent()

        val message = jsonConfig.decodeFromString<ChatCompletionMessage>(json)
        val toolCalls = message.toolCalls.shouldNotBeNull()
        toolCalls.size shouldBe 1
        toolCalls[0].id shouldBe "call_abc"
        toolCalls[0].function.name shouldBe "get_weather"
    }
}
