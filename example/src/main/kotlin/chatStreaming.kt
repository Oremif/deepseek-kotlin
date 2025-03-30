package org.oremif.deepseek

import kotlinx.coroutines.runBlocking
import org.oremif.deepseek.api.chat
import org.oremif.deepseek.client.DeepSeekClientStream
import org.oremif.deepseek.models.ChatModel

fun main(): Unit = runBlocking {
    val apiKey = System.getenv("DEEPSEEK_API_KEY") ?: "YOUR_API_KEY"
    val client = DeepSeekClientStream(apiKey) {
        params {
            chatStream {
                model = ChatModel.DEEPSEEK_CHAT
                maxTokens = 2048
            }
        }
    }

    client.chat("Tell me a story about building the best SDK on Kotlin!").collect {
        print(it.choices.first().delta.content)
    }

    client.close()
}