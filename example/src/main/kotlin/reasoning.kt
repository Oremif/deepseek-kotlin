package org.oremif.deepseek

import kotlinx.coroutines.runBlocking
import org.oremif.deepseek.api.chat
import org.oremif.deepseek.client.DeepSeekClient
import org.oremif.deepseek.models.ChatModel

fun main(): Unit = runBlocking {
    val apiKey = System.getenv("DEEPSEEK_API_KEY") ?: "YOUR_API_KEY"
    val client = DeepSeekClient(apiKey) {
        params {
            chat {
                model = ChatModel.DEEPSEEK_REASONER
                maxTokens = 2048
            }
        }
    }

    val message = client.chat("Tell me a story about building the best SDK on Kotlin!")
    println("Reasoning:\n ${message.choices.first().message.reasoningContent}")
    println("Answer:\n ${message.choices.first().message.content}")

    client.close()
}