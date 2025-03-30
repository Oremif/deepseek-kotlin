package org.oremif.deepseek

import kotlinx.coroutines.runBlocking
import org.oremif.deepseek.api.chat
import org.oremif.deepseek.client.DeepSeekClient
import org.oremif.deepseek.models.ChatModel

// Example demonstrating how to use DeepSeek's chat API for a simple text completion
fun main(): Unit = runBlocking {
    // Get API key from environment variable or use default placeholder
    val apiKey = System.getenv("DEEPSEEK_API_KEY") ?: "YOUR_API_KEY"
    // Initialize the client with configuration parameters
    val client = DeepSeekClient(apiKey) {
        params {
            chat {
                model = ChatModel.DEEPSEEK_CHAT  // Specify which model to use
                maxTokens = 2048                 // Set maximum response length
            }
        }
    }

    // Send a chat request and print the response content
    println(client.chat("Tell me a story about building the best SDK on Kotlin!").choices.first().message.content)

    // Close the client to release resources
    client.close()
}
