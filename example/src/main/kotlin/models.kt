package org.oremif.deepseek

import kotlinx.coroutines.runBlocking
import org.oremif.deepseek.api.models
import org.oremif.deepseek.client.DeepSeekClient

// Example demonstrating how to list available DeepSeek models
fun main(): Unit = runBlocking {
    // Get API key from environment variable or use default placeholder
    val apiKey = System.getenv("DEEPSEEK_API_KEY") ?: "YOUR_API_KEY"
    // Initialize the DeepSeek client
    val client = DeepSeekClient(apiKey)

    // Fetch and iterate through the list of available models
    client.models().data.forEach { model ->
        println("Model: ${model.id}")      // Display model identifier
        println("Owner: ${model.ownedBy}") // Display model owner
    }

    // Close the client to release resources
    client.close()
}
