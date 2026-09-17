package org.oremif.deepseek

import kotlinx.coroutines.runBlocking
import org.oremif.deepseek.api.createResponse
import org.oremif.deepseek.client.DeepSeekClient
import org.oremif.deepseek.models.ChatModel
import org.oremif.deepseek.models.responses.WebSearchCallItem
import org.oremif.deepseek.models.responses.WebSearchTool
import org.oremif.deepseek.models.responses.responseParams

// Example demonstrating the built-in web search tool, which the Responses API runs server-side
fun main(): Unit = runBlocking {
    // Get API key from environment variable or use default placeholder
    val apiKey = System.getenv("DEEPSEEK_API_KEY") ?: "YOUR_API_KEY"
    // Initialize the DeepSeek client
    val client = DeepSeekClient(apiKey)

    val params = responseParams {
        model = ChatModel.DEEPSEEK_V4_PRO
        tools = listOf(WebSearchTool.webSearch)
    }

    val response = client.createResponse(params, "What is the latest Kotlin release?")

    // Searches the model ran show up as items in the output, next to its message
    response.output.filterIsInstance<WebSearchCallItem>().forEach {
        println("Search: ${it.action}")
    }
    println(response.outputText)

    // Close the client to release resources
    client.close()
}
