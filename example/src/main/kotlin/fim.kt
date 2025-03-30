package org.oremif.deepseek

import kotlinx.coroutines.runBlocking
import org.oremif.deepseek.api.fim
import org.oremif.deepseek.client.DeepSeekClient

fun main(): Unit = runBlocking {
    val apiKey = System.getenv("DEEPSEEK_API_KEY") ?: "YOUR_API_KEY"
    val client = DeepSeekClient(apiKey)

    println(client.fim("fun main () {").choices.first().text)

    client.close()
}