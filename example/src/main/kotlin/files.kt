package org.oremif.deepseek

import java.io.File
import kotlinx.coroutines.runBlocking
import org.oremif.deepseek.api.chat
import org.oremif.deepseek.api.deleteFile
import org.oremif.deepseek.api.listFiles
import org.oremif.deepseek.api.retrieveFile
import org.oremif.deepseek.api.uploadFile
import org.oremif.deepseek.client.DeepSeekClient
import org.oremif.deepseek.models.ChatModel
import org.oremif.deepseek.models.chatCompletionParams

// Example demonstrating the Files API: upload an image once, then reference it by id from as many
// messages as needed instead of re-sending its bytes
fun main(): Unit = runBlocking {
    // Get API key from environment variable or use default placeholder
    val apiKey = System.getenv("DEEPSEEK_API_KEY") ?: "YOUR_API_KEY"
    // Initialize the DeepSeek client
    val client = DeepSeekClient(apiKey)

    // The image format is detected from the bytes themselves, so `filename` is only a label.
    // `expiresAfterSeconds` (1 hour to 30 days) makes the file temporary; without it, it is kept.
    val bytes = File(System.getenv("IMAGE_PATH") ?: "image.jpg").readBytes()
    val uploaded = client.uploadFile(bytes, "image.jpg", expiresAfterSeconds = 3600)
    println("Uploaded ${uploaded.id}: ${uploaded.bytes} bytes, expires at ${uploaded.expiresAt}")

    // Reference the uploaded file from a message
    val response =
        client.chat(chatCompletionParams { model = ChatModel.DEEPSEEK_FLASH }) {
            user {
                text("What is in this image?")
                imageFile(uploaded.id)
            }
        }
    println(response.choices.first().message.content)

    // Walk the stored files one page at a time
    var page = client.listFiles(limit = 100)
    while (true) {
        page.data.forEach { println("${it.id}  ${it.filename}  ${it.bytes} bytes") }
        if (!page.hasMore) break
        page = client.listFiles(after = page.lastId, limit = 100)
    }

    // Fetch a single file's metadata, then delete it
    println("Purpose: ${client.retrieveFile(uploaded.id).purpose}")
    println("Deleted: ${client.deleteFile(uploaded.id).deleted}")

    // Close the client to release resources
    client.close()
}
