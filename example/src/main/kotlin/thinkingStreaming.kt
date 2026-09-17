package org.oremif.deepseek

import kotlinx.coroutines.runBlocking
import org.oremif.deepseek.api.chat
import org.oremif.deepseek.client.DeepSeekClientStream
import org.oremif.deepseek.models.ChatModel
import org.oremif.deepseek.models.ReasoningEffort
import org.oremif.deepseek.models.chatCompletionStreamParams

// Example demonstrating streaming in thinking mode
// Streaming allows receiving both the reasoning process and the final answer in real-time
fun main(): Unit = runBlocking {
    // Get API key from environment variable or use default placeholder
    val apiKey = System.getenv("DEEPSEEK_API_KEY") ?: "YOUR_API_KEY"
    // Initialize the streaming client
    val client = DeepSeekClientStream(apiKey)

    val params = chatCompletionStreamParams {
        model = ChatModel.DEEPSEEK_V4_PRO
        reasoningEffort = ReasoningEffort.LOW
        maxTokens = 2048
    }

    // Each chunk carries either a piece of the chain-of-thought or a piece of the answer
    client
        .chat(params) { user("Tell me a story about building the best SDK on Kotlin!") }
        .collect { chunk ->
            val delta = chunk.choices.firstOrNull()?.delta ?: return@collect
            delta.reasoningContent?.let { print(it) } // Print reasoning process chunks
            delta.content?.let { print(it) } // Print answer chunks
        }

    // Close the client to release resources
    client.close()
}
