package org.oremif.deepseek

import kotlinx.coroutines.runBlocking
import org.oremif.deepseek.api.chat
import org.oremif.deepseek.client.DeepSeekClientStream
import org.oremif.deepseek.models.ChatModel
import org.oremif.deepseek.models.chatCompletionStreamParams

// Example demonstrating how to use DeepSeek's streaming chat API
// Streaming allows receiving the response in real-time as it's being generated
fun main(): Unit = runBlocking {
    // Get API key from environment variable or use default placeholder
    val apiKey = System.getenv("DEEPSEEK_API_KEY") ?: "YOUR_API_KEY"
    // Streaming lives on its own client type, which returns a Flow of chunks
    val client = DeepSeekClientStream(apiKey)

    val params = chatCompletionStreamParams {
        model = ChatModel.DEEPSEEK_FLASH // Specify which model to use
        maxTokens = 2048 // Set maximum response length
    }

    // Send a chat request and collect the streaming response
    // Each chunk of the response is printed as it arrives
    client
        .chat(params) { user("Tell me a story about building the best SDK on Kotlin!") }
        .collect { print(it.choices.firstOrNull()?.delta?.content ?: "") }

    // Close the client to release resources
    client.close()
}
