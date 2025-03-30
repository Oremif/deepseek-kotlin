package org.oremif.deepseek

import kotlinx.coroutines.runBlocking
import org.oremif.deepseek.api.fim
import org.oremif.deepseek.client.DeepSeekClientStream

// Example demonstrating how to use DeepSeek's streaming Fill-in-the-Middle (FIM) API
// Streaming allows receiving the code completion in real-time as it's being generated
fun main(): Unit = runBlocking {
    // Get API key from environment variable or use default placeholder
    val apiKey = System.getenv("DEEPSEEK_API_KEY") ?: "YOUR_API_KEY"
    // Initialize the streaming client
    val client = DeepSeekClientStream(apiKey)

    // Send a FIM request and collect the streaming response
    // Each chunk of the completed code is printed as it arrives
    client.fim("fun main () {").collect {
        print(it.choices.first().text)
    }

    // Close the client to release resources
    client.close()
}
