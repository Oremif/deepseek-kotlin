package org.oremif.deepseek

import kotlinx.coroutines.runBlocking
import org.oremif.deepseek.api.models
import org.oremif.deepseek.client.DeepSeekClient

fun main(): Unit = runBlocking {
    val apiKey = System.getenv("DEEPSEEK_API_KEY") ?: "YOUR_API_KEY"
    val client = DeepSeekClient(apiKey)

    client.models().data.forEach { model ->
        println("Model: ${model.id}")
        println("Owner: ${model.ownedBy}")
    }

    client.close()
}