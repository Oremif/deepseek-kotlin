package org.oremif.deepseek

import kotlinx.coroutines.runBlocking
import org.oremif.deepseek.api.chat
import org.oremif.deepseek.client.DeepSeekClient
import org.oremif.deepseek.models.ChatModel
import org.oremif.deepseek.models.chatCompletionParams

// Example demonstrating how to use DeepSeek's chat API for a simple text completion
fun main(): Unit = runBlocking {
    // Get API key from environment variable or use default placeholder
    val apiKey = System.getenv("DEEPSEEK_API_KEY") ?: "YOUR_API_KEY"
    // Clients are long-lived: build one and reuse it for the lifetime of the application
    val client = DeepSeekClient(apiKey)

    // Parameters belong to the call, not to the client
    val params = chatCompletionParams {
        model = ChatModel.DEEPSEEK_FLASH // Specify which model to use
        maxTokens = 2048 // Set maximum response length
    }

    // Send a chat request and print the response content
    val response = client.chat(params) {
            system("You are a helpful assistant.")
            user("Tell me a story about building the best SDK on Kotlin!")
        }
    println(response.choices.first().message.content)

    // Close the client to release resources
    client.close()
}
