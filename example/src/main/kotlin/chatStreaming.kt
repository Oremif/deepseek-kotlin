package org.oremif.deepseek

import kotlinx.coroutines.runBlocking
import org.oremif.deepseek.api.chat
import org.oremif.deepseek.client.DeepSeekClientStream
import org.oremif.deepseek.models.ChatModel

// Example demonstrating how to use DeepSeek's streaming chat API
// Streaming allows receiving the response in real-time as it's being generated
fun main(): Unit = runBlocking {
    // Get API key from environment variable or use default placeholder
    val apiKey = System.getenv("DEEPSEEK_API_KEY") ?: "YOUR_API_KEY"
    // Initialize the streaming client with configuration parameters
    val client = DeepSeekClientStream(apiKey) {
        params {
            chatStream {
                model = ChatModel.DEEPSEEK_CHAT  // Specify which model to use
                maxTokens = 2048                 // Set maximum response length
            }
        }
    }

    // Send a chat request and collect the streaming response
    // Each chunk of the response is printed as it arrives
    client.chat("Tell me a story about building the best SDK on Kotlin!").collect {
        print(it.choices.first().delta.content)
    }

    // Close the client to release resources
    client.close()
}
