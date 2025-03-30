package org.oremif.deepseek

import kotlinx.coroutines.runBlocking
import org.oremif.deepseek.api.chat
import org.oremif.deepseek.client.DeepSeekClientStream
import org.oremif.deepseek.models.ChatModel

// Example demonstrating how to use DeepSeek's streaming reasoning capabilities
// Streaming allows receiving both reasoning process and final answer in real-time
fun main(): Unit = runBlocking {
    // Get API key from environment variable or use default placeholder
    val apiKey = System.getenv("DEEPSEEK_API_KEY") ?: "YOUR_API_KEY"
    // Initialize the streaming client with configuration parameters
    val client = DeepSeekClientStream(apiKey) {
        params {
            chatStream {
                model = ChatModel.DEEPSEEK_REASONER  // Use the reasoner model
                maxTokens = 2048                     // Set maximum response length
            }
        }
    }

    // Send a chat request to the reasoning model and get a flow of responses
    val flowResponse = client.chat("Tell me a story about building the best SDK on Kotlin!")
    // Collect and print each chunk of the response as it arrives
    flowResponse.collect { message ->
        message.choices.first().delta.reasoningContent?.let { print(it) }  // Print reasoning process chunks
        message.choices.first().delta.content?.let { print(it) }           // Print answer chunks
    }

    // Close the client to release resources
    client.close()
}
