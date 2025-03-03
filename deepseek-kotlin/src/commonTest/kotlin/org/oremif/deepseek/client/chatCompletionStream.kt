package org.oremif.deepseek.client

import kotlinx.coroutines.test.runTest
import org.oremif.deepseek.api.chatCompletionStream
import org.oremif.deepseek.models.*
import kotlin.test.Test

class ChatCompletionStreamTest {
    @Test
    fun `test chat completion stream`() = runTest {
        val request = ChatCompletionRequest(
            messages = listOf(
                SystemMessage("You are a helpful assistant"),
                SystemMessage("Hi"),
            ),
            model = ChatModel.DEEPSEEK_CHAT,
            frequencyPenalty = 0.0,
            maxTokens = 2048,
            presencePenalty = 0.0,
            responseFormat = ResponseFormat.text,
            stop = null,
            stream = true,
            temperature = 1.0,
            topP = 1.0,
            tools = null,
            toolChoice = ChatCompletionToolChoice.NONE,
            logprobs = false,
            topLogprobs = null,
        )

        val client = DeepSeekClientStream("DEEPSEEK_API_KEY") {
            chatCompletionParams {
                model = ChatModel.DEEPSEEK_CHAT
            }
        }

        client.chatCompletionStream(request).collect { chunk ->
            println(chunk)
        }
    }
}