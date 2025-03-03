package org.oremif.deepseek.client

import kotlinx.coroutines.test.runTest
import org.oremif.deepseek.api.chatCompletion
import org.oremif.deepseek.models.ChatCompletionToolChoice
import org.oremif.deepseek.models.ChatModel
import org.oremif.deepseek.models.ResponseFormat
import org.oremif.deepseek.models.chatCompletionParams
import kotlin.test.Test

class ChatCompletionTest {
    @Test
    fun `test send chat completion request`() = runTest {
        val client = DeepSeekClient("DEEPSEEK_API_KEY") {
            chatCompletionParams {
                model = ChatModel.DEEPSEEK_CHAT
            }
        }

        val response = client.chatCompletion {
            messages {
                system("You are a helpful assistant")
                user("Hi")
            }
            params {
                model = ChatModel.DEEPSEEK_CHAT
                frequencyPenalty = 0.0
                maxTokens = 2048
                presencePenalty = 0.0
                responseFormat = ResponseFormat.text
                stop = null
                temperature = 1.0
                topP = 1.0
                tools = null
                toolChoice = ChatCompletionToolChoice.NONE
                logprobs = false
                topLogprobs = null
            }
        }

        println(response)
    }
}