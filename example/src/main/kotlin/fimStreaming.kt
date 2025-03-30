package org.oremif.deepseek

import kotlinx.coroutines.runBlocking
import org.oremif.deepseek.api.fim
import org.oremif.deepseek.client.DeepSeekClientStream

fun main(): Unit = runBlocking {
    val apiKey = System.getenv("DEEPSEEK_API_KEY") ?: "YOUR_API_KEY"
    val client = DeepSeekClientStream(apiKey)

    client.fim("fun main () {").collect {
        print(it.choices.first().text)
    }

    client.close()
}