package org.oremif.deepseek

import kotlinx.coroutines.runBlocking
import org.oremif.deepseek.api.chat
import org.oremif.deepseek.client.DeepSeekClient
import org.oremif.deepseek.models.ChatModel
import org.oremif.deepseek.models.ReasoningEffort
import org.oremif.deepseek.models.Thinking
import org.oremif.deepseek.models.ThinkingType
import org.oremif.deepseek.models.chatCompletionParams

// Example demonstrating thinking mode: the model returns its chain-of-thought alongside the answer
// There is no separate reasoner model anymore — every model thinks unless thinking is disabled
fun main(): Unit = runBlocking {
    // Get API key from environment variable or use default placeholder
    val apiKey = System.getenv("DEEPSEEK_API_KEY") ?: "YOUR_API_KEY"
    // Initialize the DeepSeek client
    val client = DeepSeekClient(apiKey)

    // reasoningEffort sets how long the model thinks: LOW, HIGH (the API default) or MAX
    val params = chatCompletionParams {
        model = ChatModel.DEEPSEEK_V4_PRO
        reasoningEffort = ReasoningEffort.MAX
        maxTokens = 2048
    }

    val response = client.chat(params) { user("How many R's are in the word 'strawberry'?") }
    val message = response.choices.first().message
    // Display the reasoning process (how the model arrived at the answer)
    println("Reasoning:\n${message.reasoningContent}")
    // Display the final answer
    println("Answer:\n${message.content}")
    // Thinking is billed as reasoning tokens
    println("Reasoning tokens: ${response.usage.completionTokensDetails?.reasoningTokens}")

    // Turning thinking off makes the same model answer faster and cheaper
    val fast = chatCompletionParams {
        model = ChatModel.DEEPSEEK_FLASH
        thinking = Thinking(ThinkingType.DISABLED)
    }
    println(client.chat(fast) { user("Say hi in one sentence.") }.choices.first().message.content)

    // Close the client to release resources
    client.close()
}
