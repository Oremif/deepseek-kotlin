package org.oremif.deepseek.models

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import kotlin.test.Test
import kotlin.test.assertEquals

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
                "stop": null,
                "stream": false,
                "stream_options": null,
                "temperature": 1.0,
                "top_p": 1.0,
                "tools": null,
                "tool_choice": "none",
                "logprobs": false,
                "top_logprobs": null
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
        assertEquals(2, expected.messages.size)
        assertEquals(2048, expected.maxTokens)
        assertEquals(ResponseFormat.text, expected.responseFormat)

        assertEquals(jsonRequest, jsonConfig.encodeToString(request).trimIndent())
    }

    @Test
    fun `chat completion response model test`() {
        val response = ChatCompletion(
            id = "930c60df-bf64-41c9-a88e-3ec75f81e00e",
            choices = listOf(
                ChatCompletionChoice(
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
        assertEquals(1, expected.choices.size)
        assertEquals(FinishReason.STOP, expected.choices[0].finishReason)
        assertEquals("Hello! How can I help you today?", expected.choices[0].message.content)
        assertEquals(1705651092L, expected.created)
        assertEquals(jsonResponse, jsonConfig.encodeToString(response).trimIndent())
    }
}