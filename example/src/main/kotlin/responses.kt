package org.oremif.deepseek

import kotlinx.coroutines.runBlocking
import org.oremif.deepseek.api.createResponse
import org.oremif.deepseek.client.DeepSeekClient
import org.oremif.deepseek.models.ChatModel
import org.oremif.deepseek.models.ReasoningEffort
import org.oremif.deepseek.models.responses.MessageItem
import org.oremif.deepseek.models.responses.ReasoningConfig
import org.oremif.deepseek.models.responses.ResponseItem
import org.oremif.deepseek.models.responses.ResponseRole
import org.oremif.deepseek.models.responses.responseParams

// Example demonstrating the Responses API — the OpenAI-compatible endpoint DeepSeek also serves
// It is stateless: the server keeps nothing, so every turn sends the whole history in `input`
fun main(): Unit = runBlocking {
    // Get API key from environment variable or use default placeholder
    val apiKey = System.getenv("DEEPSEEK_API_KEY") ?: "YOUR_API_KEY"
    // Initialize the DeepSeek client
    val client = DeepSeekClient(apiKey)

    val params = responseParams {
        model = ChatModel.DEEPSEEK_V4_PRO
        instructions = "You are a concise assistant." // Sent as the leading system message
        reasoning = ReasoningConfig(ReasoningEffort.LOW)
        maxOutputTokens = 1024 // Covers reasoning tokens as well as the visible answer
    }

    // The conversation is an ordinary list of items owned by the caller
    val history = mutableListOf<ResponseItem>(MessageItem(ResponseRole.USER, "What is 6 * 7?"))

    val first = client.createResponse(params, history)
    println(first.outputText) // Text of every message item, concatenated
    println("status=${first.status}, tokens=${first.usage?.totalTokens}")

    // Continuing the conversation is appending: the model's own output goes back in unchanged
    history += first.output
    history += MessageItem(ResponseRole.USER, "And what is that times two?")
    println(client.createResponse(params, history).outputText)

    // Close the client to release resources
    client.close()
}
