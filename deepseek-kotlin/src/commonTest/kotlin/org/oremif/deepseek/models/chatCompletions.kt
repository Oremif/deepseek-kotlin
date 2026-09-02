package org.oremif.deepseek.models

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.*
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
                "model": "deepseek-v4-pro",
                "max_tokens": 4096,
                "response_format": {
                    "type": "text"
                },
                "stream": false,
                "temperature": 1.0,
                "top_p": 1.0,
                "tool_choice": "none",
                "logprobs": false,
                "thinking": {
                    "type": "enabled"
                }
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
                "model": "deepseek-v4-pro",
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
            model = ChatModel.DEEPSEEK_V4_PRO,
            maxTokens = 4096,
            responseFormat = ResponseFormat.text,
            stop = null,
            stream = false,
            streamOptions = null,
            temperature = 1.0,
            topP = 1.0,
            tools = null,
            toolChoice = ChatCompletionToolChoice.NONE,
            logprobs = false,
            topLogprobs = null,
            thinking = Thinking(ThinkingType.ENABLED),
        )

        val expected = jsonConfig.decodeFromString<ChatCompletionRequest>(jsonRequest)
        expected.messages.size shouldBe 2
        expected.maxTokens shouldBe 4096
        expected.responseFormat shouldBe ResponseFormat.text
        expected.thinking?.type shouldBe ThinkingType.ENABLED

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
            model = "deepseek-v4-pro",
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
    private val clientJsonConfig = Json {
        ignoreUnknownKeys = true
        namingStrategy = JsonNamingStrategy.SnakeCase
    }

    @Test
    fun `documented request payload decodes with explicit nulls and unknown fields`() {
        val json = """
            {
              "messages": [
                {"content": "You are a helpful assistant", "role": "system"},
                {"content": "Hi", "role": "user"}
              ],
              "model": "deepseek-v4-pro",
              "thinking": {"type": "enabled"},
              "reasoning_effort": "low",
              "max_tokens": 4096,
              "response_format": {"type": "text"},
              "stop": null,
              "stream": false,
              "stream_options": null,
              "temperature": 1,
              "top_p": 1,
              "tools": null,
              "tool_choice": "none",
              "logprobs": false,
              "top_logprobs": null
            }
        """.trimIndent()

        val request = clientJsonConfig.decodeFromString<ChatCompletionRequest>(json)

        request.messages shouldBe listOf(SystemMessage("You are a helpful assistant"), UserMessage("Hi"))
        request.model shouldBe ChatModel.DEEPSEEK_V4_PRO
        request.thinking?.type shouldBe ThinkingType.ENABLED
        request.reasoningEffort shouldBe ReasoningEffort.LOW
        request.maxTokens shouldBe 4096
        request.responseFormat shouldBe ResponseFormat.text
        request.temperature shouldBe 1.0
        request.topP shouldBe 1.0
        request.toolChoice shouldBe ChatCompletionToolChoice.NONE
        request.logprobs shouldBe false
        request.stream shouldBe false
        request.stop.shouldBeNull()
        request.streamOptions.shouldBeNull()
        request.tools.shouldBeNull()
        request.topLogprobs.shouldBeNull()
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
                "model": "deepseek-v4-flash",
                "object": "chat.completion.chunk"
            }
        """.trimIndent()

        val chunk = clientJsonConfig.decodeFromString<ChatCompletionChunk>(json)
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
                "model": "deepseek-v4-flash",
                "object": "chat.completion.chunk"
            }
        """.trimIndent()

        val chunk = clientJsonConfig.decodeFromString<ChatCompletionChunk>(json)
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
                "model": "deepseek-v4-flash",
                "object": "chat.completion.chunk"
            }
        """.trimIndent()

        val chunk = clientJsonConfig.decodeFromString<ChatCompletionChunk>(json)
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
                "model": "deepseek-v4-pro",
                "object": "chat.completion.chunk"
            }
        """.trimIndent()

        val chunk = clientJsonConfig.decodeFromString<ChatCompletionChunk>(json)
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
                        "function": {"name": "get_weather", "arguments": "{}"}
                    }
                ]
            }
        """.trimIndent()

        val message = jsonConfig.decodeFromString<ChatCompletionMessage>(json)
        val toolCalls = message.toolCalls.shouldNotBeNull()
        toolCalls.size shouldBe 1
        toolCalls[0].id shouldBe "call_abc"
        toolCalls[0].function.name shouldBe "get_weather"
        toolCalls[0].function.arguments shouldBe "{}"
    }

    @Test
    fun `tool call arguments are decoded as a raw JSON string`() {
        val json = """
            {
                "role": "assistant",
                "content": null,
                "tool_calls": [
                    {
                        "id": "call_abc",
                        "type": "function",
                        "function": {"name": "get_weather", "arguments": "{\"location\": \"Tokyo\"}"}
                    }
                ]
            }
        """.trimIndent()

        val call = jsonConfig.decodeFromString<ChatCompletionMessage>(json).toolCalls.shouldNotBeNull().single()
        call.function.arguments shouldBe """{"location": "Tokyo"}"""
        call.function.argumentsAsJsonOrNull()
            .shouldNotBeNull()["location"]?.jsonPrimitive?.content shouldBe "Tokyo"
    }

    @Test
    fun `argumentsAsJsonOrNull returns null for malformed model output`() {
        FunctionResponse(name = "get_weather", arguments = """{"location": """)
            .argumentsAsJsonOrNull().shouldBeNull()
        FunctionResponse(name = "get_weather", arguments = "\"just a string\"")
            .argumentsAsJsonOrNull().shouldBeNull()
        FunctionResponse(name = "get_weather", arguments = null)
            .argumentsAsJsonOrNull().shouldBeNull()
    }

    @Test
    fun `ChatCompletionMessage content stays null instead of the string null`() {
        val json = """
            {
                "role": "assistant",
                "content": null,
                "reasoning_content": null,
                "tool_calls": [
                    {
                        "id": "call_abc",
                        "type": "function",
                        "function": {"name": "get_weather", "arguments": "{}"}
                    }
                ]
            }
        """.trimIndent()

        val message = jsonConfig.decodeFromString<ChatCompletionMessage>(json)
        message.content.shouldBeNull()
        message.reasoningContent.shouldBeNull()
        message.toolCalls.shouldNotBeNull().size shouldBe 1
    }

    @Test
    fun `ChatModel serializes as its raw slug and accepts unknown ones`() {
        jsonConfig.encodeToString(ChatModel.DEEPSEEK_V4_PRO) shouldBe "\"deepseek-v4-pro\""
        jsonConfig.decodeFromString<ChatModel>("\"deepseek-v5-flash\"") shouldBe ChatModel("deepseek-v5-flash")

        val request = ChatCompletionRequest(
            messages = listOf(UserMessage("Hi")),
            model = ChatModel("deepseek-v5-flash"),
        )
        jsonConfig.encodeToString(request) shouldContain "\"model\": \"deepseek-v5-flash\""
    }

    @Test
    fun `ChatModel rejects a blank slug`() {
        shouldThrow<IllegalArgumentException> { ChatModel(" ") }
    }

    @Test
    fun `reasoning_effort is a sibling of thinking not one of its fields`() {
        val request = ChatCompletionRequest(
            messages = listOf(UserMessage("Hi")),
            model = ChatModel.DEEPSEEK_V4_PRO,
            thinking = Thinking(ThinkingType.ENABLED),
            reasoningEffort = ReasoningEffort.MAX,
        )

        val encoded = clientJsonConfig.encodeToString(request)
        encoded shouldContain """"thinking":{"type":"enabled"}"""
        encoded shouldContain """"reasoning_effort":"max""""
    }

    @Test
    fun `ReasoningEffort serializes as its raw value and accepts unknown ones`() {
        jsonConfig.encodeToString(ReasoningEffort.LOW) shouldBe "\"low\""
        jsonConfig.encodeToString(ReasoningEffort.HIGH) shouldBe "\"high\""
        jsonConfig.decodeFromString<ReasoningEffort>("\"xhigh\"") shouldBe ReasoningEffort("xhigh")
        ReasoningEffort.MAX.toString() shouldBe "max"
        shouldThrow<IllegalArgumentException> { ReasoningEffort(" ") }
    }

    @Test
    fun `user_id is sent when set and omitted when not`() {
        val messages = listOf(UserMessage("Hi"))

        clientJsonConfig.encodeToString(
            ChatCompletionRequest(messages = messages, model = ChatModel.DEEPSEEK_V4_FLASH, userId = "user_42-a"),
        ) shouldContain """"user_id":"user_42-a""""

        clientJsonConfig.encodeToString(
            ChatCompletionRequest(messages = messages, model = ChatModel.DEEPSEEK_V4_FLASH),
        ) shouldNotContain "user_id"
    }

    @Test
    fun `params carry reasoningEffort and userId into the request and through copy`() {
        val params = chatCompletionParams {
            model = ChatModel.DEEPSEEK_V4_PRO
            reasoningEffort = ReasoningEffort.LOW
            userId = "user-1"
        }

        val request = params.createRequest(listOf(UserMessage("Hi")))
        request.reasoningEffort shouldBe ReasoningEffort.LOW
        request.userId shouldBe "user-1"

        val copied = params.copy(reasoningEffort = ReasoningEffort.MAX)
        copied.reasoningEffort shouldBe ReasoningEffort.MAX
        copied.userId shouldBe "user-1"
        copied.model shouldBe ChatModel.DEEPSEEK_V4_PRO
    }

    @Test
    fun `function strict is sent when set and omitted when not`() {
        val schema = buildJsonObject { put("type", JsonPrimitive("object")) }
        val strictTool = Tool(
            type = ToolCallType.FUNCTION,
            function = FunctionRequest("get_weather", "Get the weather", schema, strict = true),
        )

        clientJsonConfig.encodeToString(strictTool) shouldContain """"strict":true"""
        clientJsonConfig.encodeToString(
            Tool(ToolCallType.FUNCTION, FunctionRequest("get_weather", "Get the weather", schema)),
        ) shouldNotContain "strict"
    }

    @Test
    fun `a strict function declaration decodes as a request not a response`() {
        val json = """{"name": "get_weather", "description": null, "parameters": {}, "strict": true}"""

        val function = clientJsonConfig.decodeFromString<ToolFunction>(json)
        function.shouldBeInstanceOf<FunctionRequest>()
        function.strict shouldBe true
    }
}
