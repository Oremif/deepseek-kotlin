package org.oremif.deepseek

import kotlinx.coroutines.runBlocking
import org.oremif.deepseek.api.chat
import org.oremif.deepseek.client.DeepSeekClient
import org.oremif.deepseek.models.ChatModel

// Example demonstrating how to use DeepSeek's reasoning capabilities
// The reasoner model provides both the final answer and the reasoning process behind it
fun main(): Unit = runBlocking {
    // Get API key from environment variable or use default placeholder
    val apiKey = System.getenv("DEEPSEEK_API_KEY") ?: "YOUR_API_KEY"
    // Initialize the client with configuration parameters
    val client = DeepSeekClient(apiKey) {
        params {
            chat {
                model = ChatModel.DEEPSEEK_REASONER  // Use the reasoner model
                maxTokens = 2048                     // Set maximum response length
            }
        }
    }

    // Send a chat request to the reasoning model
    val message = client.chat("Tell me a story about building the best SDK on Kotlin!")
    // Display the reasoning process (how the model arrived at the answer)
    println("Reasoning:\n ${message.choices.first().message.reasoningContent}")
    // Display the final answer
    println("Answer:\n ${message.choices.first().message.content}")

    // Close the client to release resources
    client.close()
}
