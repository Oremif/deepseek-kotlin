package org.oremif.deepseek

import kotlinx.coroutines.runBlocking
import org.oremif.deepseek.api.createResponse
import org.oremif.deepseek.client.DeepSeekClientStream
import org.oremif.deepseek.models.ChatModel
import org.oremif.deepseek.models.responses.OutputTextDeltaEvent
import org.oremif.deepseek.models.responses.ReasoningTextDeltaEvent
import org.oremif.deepseek.models.responses.ResponseSnapshotEvent
import org.oremif.deepseek.models.responses.responseStreamParams

// Example demonstrating streaming from the Responses API
// Unlike the chat endpoint, it emits semantic events — one per lifecycle step, reasoning fragment,
// text fragment or tool-call fragment — and the terminal event carries the whole response
fun main(): Unit = runBlocking {
    // Get API key from environment variable or use default placeholder
    val apiKey = System.getenv("DEEPSEEK_API_KEY") ?: "YOUR_API_KEY"
    // Initialize the streaming client
    val client = DeepSeekClientStream(apiKey)

    val params = responseStreamParams { model = ChatModel.DEEPSEEK_FLASH }

    client.createResponse(params, "Write a haiku about Kotlin coroutines").collect { event ->
        when (event) {
            is ReasoningTextDeltaEvent -> print(event.delta) // Chain-of-thought fragment
            is OutputTextDeltaEvent -> print(event.delta) // Visible answer fragment
            is ResponseSnapshotEvent ->
                if (event.isTerminal) {
                    // The last event carries the finished response, usage included
                    println()
                    println("${event.response.status}: ${event.response.usage?.totalTokens} tokens")
                }
            else -> Unit // Twenty event types in total; the rest are ignored here
        }
    }

    // Close the client to release resources
    client.close()
}
