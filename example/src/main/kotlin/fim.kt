package org.oremif.deepseek

import kotlinx.coroutines.runBlocking
import org.oremif.deepseek.api.fim
import org.oremif.deepseek.client.DeepSeekClient
import org.oremif.deepseek.models.ChatModel
import org.oremif.deepseek.models.fimCompletionParams

// Example demonstrating how to use DeepSeek's Fill-in-the-Middle (FIM) API
// FIM is useful for code completion: the model fills the gap between a prefix and a suffix
fun main(): Unit = runBlocking {
    // Get API key from environment variable or use default placeholder
    val apiKey = System.getenv("DEEPSEEK_API_KEY") ?: "YOUR_API_KEY"
    // Initialize the DeepSeek client
    val client = DeepSeekClient(apiKey)

    // The simplest form takes only the prefix
    println(client.fim("fun main() {").choices.first().text)

    // With a suffix the completion has to fit between the two
    val params = fimCompletionParams {
        model = ChatModel.DEEPSEEK_V4_PRO // Default; deepseek-flash works too
        maxTokens = 128
        suffix = "\n}"
    }
    println(client.fim(params, "fun fibonacci(n: Int): Int {").choices.first().text)

    // Close the client to release resources
    client.close()
}
