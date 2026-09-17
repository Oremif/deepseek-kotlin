package org.oremif.deepseek

import kotlinx.coroutines.runBlocking
import org.oremif.deepseek.api.chat
import org.oremif.deepseek.client.DeepSeekClient
import org.oremif.deepseek.models.ChatModel
import org.oremif.deepseek.models.ImageDetail
import org.oremif.deepseek.models.chatCompletionParams

// Example demonstrating image input: a user message may carry content parts instead of plain text
// Both models read images, so no separate vision model is needed
fun main(): Unit = runBlocking {
    // Get API key from environment variable or use default placeholder
    val apiKey = System.getenv("DEEPSEEK_API_KEY") ?: "YOUR_API_KEY"
    // Initialize the DeepSeek client
    val client = DeepSeekClient(apiKey)

    val params = chatCompletionParams {
        model = ChatModel.DEEPSEEK_FLASH
        maxTokens = 1024
    }

    val imageUrl =
        System.getenv("IMAGE_URL")
            ?: "https://upload.wikimedia.org/wikipedia/commons/8/8b/Kotlin_logo.png"

    val response =
        client.chat(params) {
            user {
                text("What is in this image? Answer in one sentence.")
                // ImageDetail.LOW downscales the image to 512x512 — faster and cheaper
                image(imageUrl, ImageDetail.LOW)
                // An image can also travel inline as a base64 data URL:
                // imageData("data:image/jpeg;base64,...", filename = "cat.jpg")
                // ...or by the id of a file uploaded through the Files API (see files.kt):
                // imageFile("file-api-...")
            }
        }

    println(response.choices.first().message.content)

    // Close the client to release resources
    client.close()
}
