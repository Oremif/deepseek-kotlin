package org.oremif.deepseek

import kotlinx.coroutines.runBlocking
import org.oremif.deepseek.api.fim
import org.oremif.deepseek.client.DeepSeekClient

// Example demonstrating how to use DeepSeek's Fill-in-the-Middle (FIM) API
// FIM is useful for code completion and generating content based on a partial input
fun main(): Unit = runBlocking {
    // Get API key from environment variable or use default placeholder
    val apiKey = System.getenv("DEEPSEEK_API_KEY") ?: "YOUR_API_KEY"
    // Initialize the DeepSeek client
    val client = DeepSeekClient(apiKey)

    // Send a FIM request with a partial function and print the completed code
    println(client.fim("fun main () {").choices.first().text)

    // Close the client to release resources
    client.close()
}
