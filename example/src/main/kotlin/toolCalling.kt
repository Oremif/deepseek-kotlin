package org.oremif.deepseek

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.oremif.deepseek.api.chat
import org.oremif.deepseek.client.DeepSeekClient
import org.oremif.deepseek.models.ChatMessage
import org.oremif.deepseek.models.ChatModel
import org.oremif.deepseek.models.FunctionRequest
import org.oremif.deepseek.models.Tool
import org.oremif.deepseek.models.ToolCallType
import org.oremif.deepseek.models.ToolMessage
import org.oremif.deepseek.models.UserMessage
import org.oremif.deepseek.models.argumentsAsJsonOrNull
import org.oremif.deepseek.models.chatCompletionParams

// Example demonstrating tool (function) calling: the model asks for a tool, the application runs it
// and answers, and the conversation continues with the tool's result
fun main(): Unit = runBlocking {
    // Get API key from environment variable or use default placeholder
    val apiKey = System.getenv("DEEPSEEK_API_KEY") ?: "YOUR_API_KEY"
    // Initialize the DeepSeek client
    val client = DeepSeekClient(apiKey)

    // Describe the tool and the JSON Schema of its arguments
    val getWeather =
        Tool(
            type = ToolCallType.FUNCTION,
            function =
                FunctionRequest(
                    name = "get_weather",
                    description = "Get the current weather for a city.",
                    parameters =
                        buildJsonObject {
                            put("type", "object")
                            putJsonObject("properties") {
                                putJsonObject("city") {
                                    put("type", "string")
                                    put("description", "City name, e.g. Paris")
                                }
                            }
                            putJsonArray("required") { add("city") }
                        },
                    // Beta: makes the model match the schema exactly
                    strict = true,
                ),
        )

    val params = chatCompletionParams {
        model = ChatModel.DEEPSEEK_FLASH
        tools = listOf(getWeather)
    }

    // The conversation is owned by the caller and grows with every turn
    val messages = mutableListOf<ChatMessage>(UserMessage("What's the weather in Paris right now?"))

    val answer = client.chat(params, messages).choices.first().message
    // The message from the response goes straight back into the conversation
    messages.add(answer)

    // Run every requested call and reply to each with a ToolMessage carrying its result
    for (call in answer.toolCalls.orEmpty()) {
        // `arguments` is a JSON string the model generated; parse it defensively
        val city = call.function.argumentsAsJsonOrNull()?.get("city")?.jsonPrimitive?.content
        println("Calling ${call.function.name}(city=$city)")
        messages.add(ToolMessage("""{"temperature_c": 21, "condition": "sunny"}""", call.id))
    }

    // The follow-up request turns the tool output into a natural-language answer
    println(client.chat(params, messages).choices.first().message.content)

    // Close the client to release resources
    client.close()
}
